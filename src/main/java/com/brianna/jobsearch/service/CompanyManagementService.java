package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.CompanyContactRelationship;
import com.brianna.jobsearch.repository.CompanyManagementRepository;
import com.brianna.jobsearch.repository.CompanyManagementRepository.CompanyApplicationRow;
import com.brianna.jobsearch.repository.CompanyManagementRepository.CompanyNameRow;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyManagementService {

    private static final Set<String> COMPANY_SUFFIXES = Set.of(
            "inc", "incorporated", "llc", "ltd", "limited", "corp", "corporation", "company", "co");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final long MAX_CONTACT_PHOTO_BYTES = 1_000_000L;

    private final CompanyManagementRepository repository;
    private final CompanyLogoService logoService;

    public CompanyManagementService(
            CompanyManagementRepository repository,
            CompanyLogoService logoService) {
        this.repository = repository;
        this.logoService = logoService;
    }

    public CompanySnapshot snapshot(String query, String status) {
        return snapshot(query, status, 1, 20);
    }

    public CompanySnapshot snapshot(String query, String status, Integer page, Integer pageSize) {
        String safeQuery = query == null ? "" : query.trim();
        String safeStatus = normalizeStatus(status);
        int safePageSize = normalizePageSize(pageSize);
        List<CompanyGroup> allGroups = buildGroups(repository.findCompanyNames());

        long applications = allGroups.stream().mapToLong(CompanyGroup::applications).sum();
        long withDomain = allGroups.stream().filter(CompanyGroup::hasSingleDomain).count();
        long withLogo = allGroups.stream().filter(CompanyGroup::logoCached).count();
        long needingAttention = allGroups.stream().filter(CompanyGroup::needsAttention).count();

        List<CompanyGroup> matching = allGroups.stream()
                .filter(group -> matchesQuery(group, safeQuery))
                .filter(group -> matchesStatus(group, safeStatus))
                .toList();

        int totalPages = Math.max(1, (matching.size() + safePageSize - 1) / safePageSize);
        int safePage = page == null ? 1 : Math.max(1, Math.min(page, totalPages));
        int fromIndex = Math.min((safePage - 1) * safePageSize, matching.size());
        int toIndex = Math.min(fromIndex + safePageSize, matching.size());
        List<CompanyGroup> visible = matching.subList(fromIndex, toIndex);

        return new CompanySnapshot(
                applications,
                allGroups.size(),
                withDomain,
                withLogo,
                needingAttention,
                matching.size(),
                safeQuery,
                safeStatus,
                safePage,
                safePageSize,
                totalPages,
                matching.isEmpty() ? 0 : fromIndex + 1,
                toIndex,
                List.copyOf(visible));
    }


    public CompanyDetail detail(String groupKey) {
        CompanyGroup group = requireGroup(groupKey);
        return new CompanyDetail(
                group,
                repository.findApplicationsForCompanyNames(group.aliases()),
                repository.findContacts(group.key()),
                repository.findCompanyNotes(group.key()));
    }

    public Optional<String> knownDomainForCompany(String companyName) {
        String key = normalizeCompanyKey(companyName);
        if (key.isBlank()) {
            return Optional.empty();
        }
        CompanyGroup group = groupsByKey().get(key);
        return group != null && group.hasSingleDomain()
                ? Optional.of(group.domain())
                : Optional.empty();
    }

    public CompanyContact createContact(
            String groupKey,
            String name,
            String role,
            String relationshipType,
            String email,
            String linkedinUrl,
            String notes,
            MultipartFile photo) {
        CompanyGroup group = requireGroup(groupKey);
        ContactValues values = validateContactValues(name, role, relationshipType, email, linkedinUrl, notes);
        PhotoPayload photoPayload = readPhoto(photo);
        long id = repository.insertContact(
                group.key(),
                values.name(),
                values.role(),
                values.relationship().name(),
                values.email(),
                values.linkedinUrl(),
                values.notes(),
                photoPayload == null ? null : photoPayload.mimeType(),
                photoPayload == null ? null : photoPayload.data());
        return repository.findContact(id);
    }

    public CompanyContact updateContact(
            String groupKey,
            long contactId,
            String name,
            String role,
            String relationshipType,
            String email,
            String linkedinUrl,
            String notes,
            MultipartFile photo,
            boolean removePhoto) {
        CompanyGroup group = requireGroup(groupKey);
        CompanyContact existing = requireContact(group, contactId);
        ContactValues values = validateContactValues(name, role, relationshipType, email, linkedinUrl, notes);
        PhotoPayload photoPayload = readPhoto(photo);
        boolean replacePhoto = removePhoto || photoPayload != null;
        repository.updateContact(
                existing.id(),
                values.name(),
                values.role(),
                values.relationship().name(),
                values.email(),
                values.linkedinUrl(),
                values.notes(),
                replacePhoto,
                removePhoto || photoPayload == null ? null : photoPayload.mimeType(),
                removePhoto || photoPayload == null ? null : photoPayload.data());
        return repository.findContact(existing.id());
    }

    public void deleteContact(String groupKey, long contactId) {
        CompanyGroup group = requireGroup(groupKey);
        CompanyContact existing = requireContact(group, contactId);
        repository.deleteContact(existing.id());
    }

    public Optional<CompanyManagementRepository.ContactPhoto> contactPhoto(long contactId) {
        return Optional.ofNullable(repository.findContactPhoto(contactId));
    }

    public void saveNotes(String groupKey, String notes) {
        CompanyGroup group = requireGroup(groupKey);
        repository.saveCompanyNotes(group.key(), notes);
    }

    public DomainUpdateResult setDomain(String groupKey, String rawDomain, boolean fetchLogo) {
        CompanyGroup group = requireGroup(groupKey);
        String domain = CompanyLogoService.normalizeDomain(rawDomain);
        if (domain == null) {
            throw new IllegalArgumentException("Enter a valid company domain, for example mastercard.com.");
        }

        int updated = repository.updateDomainForCompanyNames(group.aliases(), domain);
        String logoWarning = null;
        boolean logoFetched = false;
        if (fetchLogo) {
            try {
                logoService.fetchAndCache(domain);
                logoFetched = true;
            } catch (IllegalArgumentException ex) {
                logoWarning = ex.getMessage();
            }
        }
        return new DomainUpdateResult(updated, domain, logoFetched, logoWarning);
    }

    public void refreshLogo(String groupKey) {
        CompanyGroup group = requireGroup(groupKey);
        logoService.fetchAndCache(requireSingleDomain(group));
    }

    public void uploadLogo(String groupKey, MultipartFile file) {
        CompanyGroup group = requireGroup(groupKey);
        logoService.storeUpload(requireSingleDomain(group), file);
    }

    public void deleteLogo(String groupKey) {
        CompanyGroup group = requireGroup(groupKey);
        logoService.delete(requireSingleDomain(group));
    }

    public RenameResult renameGroup(String groupKey, String canonicalName) {
        CompanyGroup group = requireGroup(groupKey);
        String cleanName = requireCompanyName(canonicalName);
        String newKey = normalizeCompanyKey(cleanName);
        int updated = repository.renameCompanyNames(group.aliases(), cleanName);
        repository.moveCompanyNotes(List.of(group.key()), newKey);
        repository.moveCompanyContacts(List.of(group.key()), newKey);
        return new RenameResult(updated, cleanName, newKey);
    }

    public MergeResult mergeGroups(List<String> groupKeys, String canonicalName) {
        if (groupKeys == null || groupKeys.size() < 2) {
            throw new IllegalArgumentException("Select at least two company groups to merge.");
        }
        String cleanName = requireCompanyName(canonicalName);
        Map<String, CompanyGroup> current = groupsByKey();
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        int groups = 0;
        for (String key : groupKeys) {
            CompanyGroup group = current.get(key);
            if (group != null) {
                groups++;
                aliases.addAll(group.aliases());
            }
        }
        if (groups < 2 || aliases.size() < 2) {
            throw new IllegalArgumentException("Those company groups are no longer available to merge. Reload and try again.");
        }
        int updated = repository.renameCompanyNames(List.copyOf(aliases), cleanName);
        List<String> oldKeys = groupKeys.stream().filter(current::containsKey).distinct().toList();
        String newKey = normalizeCompanyKey(cleanName);
        repository.moveCompanyNotes(oldKeys, newKey);
        repository.moveCompanyContacts(oldKeys, newKey);
        return new MergeResult(groups, updated, cleanName);
    }

    public String normalizeStatus(String status) {
        if (status == null) return "ALL";
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "MISSING_DOMAIN" -> "MISSING_DOMAIN";
            case "MISSING_LOGO" -> "MISSING_LOGO";
            case "ALIASES" -> "ALIASES";
            case "DOMAIN_CONFLICT" -> "DOMAIN_CONFLICT";
            default -> "ALL";
        };
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) return 20;
        return switch (pageSize) {
            case 40 -> 40;
            case 80 -> 80;
            default -> 20;
        };
    }

    List<CompanyGroup> buildGroups(List<CompanyNameRow> rows) {
        Map<String, MutableCompanyGroup> grouped = new LinkedHashMap<>();
        for (CompanyNameRow row : rows) {
            String key = normalizeCompanyKey(row.companyName());
            if (key.isBlank()) {
                key = row.companyName().trim().toLowerCase(Locale.ROOT);
            }
            grouped.computeIfAbsent(key, MutableCompanyGroup::new).add(row);
        }

        List<CompanyGroup> result = new ArrayList<>();
        for (MutableCompanyGroup group : grouped.values()) {
            result.add(group.toImmutable());
        }
        result.sort(Comparator
                .comparing(CompanyGroup::needsAttention).reversed()
                .thenComparing(CompanyGroup::applications, Comparator.reverseOrder())
                .thenComparing(CompanyGroup::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    public static String normalizeCompanyKey(String value) {
        if (value == null || value.isBlank()) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String[] tokens = NON_ALNUM.matcher(ascii).replaceAll(" ").trim().split("\\s+");
        List<String> kept = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank() && !COMPANY_SUFFIXES.contains(token)) {
                kept.add(token);
            }
        }
        return String.join(" ", kept);
    }

    private boolean matchesQuery(CompanyGroup group, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase(Locale.ROOT);
        return group.displayName().toLowerCase(Locale.ROOT).contains(q)
                || group.aliases().stream().anyMatch(alias -> alias.toLowerCase(Locale.ROOT).contains(q))
                || group.domains().stream().anyMatch(domain -> domain.toLowerCase(Locale.ROOT).contains(q));
    }

    private boolean matchesStatus(CompanyGroup group, String status) {
        return switch (status) {
            case "MISSING_DOMAIN" -> group.domains().isEmpty();
            case "MISSING_LOGO" -> group.hasSingleDomain() && !group.logoCached();
            case "ALIASES" -> group.aliases().size() > 1;
            case "DOMAIN_CONFLICT" -> group.domains().size() > 1;
            default -> true;
        };
    }

    private CompanyGroup requireGroup(String groupKey) {
        CompanyGroup group = groupsByKey().get(groupKey);
        if (group == null) {
            throw new IllegalArgumentException("That company group changed or no longer exists. Reload and try again.");
        }
        return group;
    }

    private Map<String, CompanyGroup> groupsByKey() {
        Map<String, CompanyGroup> map = new LinkedHashMap<>();
        for (CompanyGroup group : buildGroups(repository.findCompanyNames())) {
            map.put(group.key(), group);
        }
        return map;
    }

    private CompanyContact requireContact(CompanyGroup group, long contactId) {
        CompanyContact contact = repository.findContact(contactId);
        if (contact == null || !group.key().equals(contact.companyKey())) {
            throw new IllegalArgumentException("That person is no longer associated with this company.");
        }
        return contact;
    }

    private ContactValues validateContactValues(
            String name,
            String role,
            String relationshipType,
            String email,
            String linkedinUrl,
            String notes) {
        String cleanName = cleanRequired(name, "Enter the person's name.", 160);
        String cleanRole = cleanOptional(role, 200, "Roles must be 200 characters or fewer.");
        CompanyContactRelationship relationship = parseRelationship(relationshipType);
        String cleanEmail = cleanOptional(email, 320, "Email addresses must be 320 characters or fewer.");
        if (cleanEmail != null && !cleanEmail.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email address or leave it blank.");
        }
        String cleanLinkedIn = cleanOptional(linkedinUrl, 500, "LinkedIn URLs must be 500 characters or fewer.");
        if (cleanLinkedIn != null
                && !(cleanLinkedIn.startsWith("https://") || cleanLinkedIn.startsWith("http://"))) {
            cleanLinkedIn = "https://" + cleanLinkedIn;
        }
        if (cleanLinkedIn != null && !cleanLinkedIn.toLowerCase(Locale.ROOT).contains("linkedin.com/")) {
            throw new IllegalArgumentException("Enter a LinkedIn profile URL or leave it blank.");
        }
        String cleanNotes = cleanOptional(notes, 10_000, "Person notes must be 10,000 characters or fewer.");
        return new ContactValues(cleanName, cleanRole, relationship, cleanEmail, cleanLinkedIn, cleanNotes);
    }

    private CompanyContactRelationship parseRelationship(String value) {
        if (value == null || value.isBlank()) return CompanyContactRelationship.OTHER;
        try {
            return CompanyContactRelationship.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CompanyContactRelationship.OTHER;
        }
    }

    private String cleanRequired(String value, String message, int maxLength) {
        String clean = value == null ? null : value.trim().replaceAll("\\s+", " ");
        if (clean == null || clean.isBlank()) throw new IllegalArgumentException(message);
        if (clean.length() > maxLength) throw new IllegalArgumentException("Names must be " + maxLength + " characters or fewer.");
        return clean;
    }

    private String cleanOptional(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.length() > maxLength) throw new IllegalArgumentException(message);
        return clean;
    }

    private PhotoPayload readPhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) return null;
        if (photo.getSize() > MAX_CONTACT_PHOTO_BYTES) {
            throw new IllegalArgumentException("Profile photos must be 1 MB or smaller.");
        }
        try {
            byte[] data = photo.getBytes();
            String mime = detectPhotoMimeType(data);
            if (mime == null) {
                throw new IllegalArgumentException("Profile photos must be PNG, JPEG, GIF, or WebP images.");
            }
            return new PhotoPayload(mime, data);
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("The profile photo could not be read.");
        }
    }

    private String detectPhotoMimeType(byte[] data) {
        if (data == null || data.length < 4) return null;
        if (data.length >= 8
                && (data[0] & 0xff) == 0x89 && data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47
                && data[4] == 0x0d && data[5] == 0x0a && data[6] == 0x1a && data[7] == 0x0a) {
            return "image/png";
        }
        if ((data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (data.length >= 6) {
            String header = new String(data, 0, 6, StandardCharsets.US_ASCII);
            if ("GIF87a".equals(header) || "GIF89a".equals(header)) return "image/gif";
        }
        if (data.length >= 12
                && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return "image/webp";
        }
        return null;
    }

    private String requireSingleDomain(CompanyGroup group) {
        if (group.domains().isEmpty()) {
            throw new IllegalArgumentException("Set a company domain before managing its logo.");
        }
        if (group.domains().size() > 1) {
            throw new IllegalArgumentException("Resolve the conflicting company domains before managing the shared logo.");
        }
        return group.domains().get(0);
    }

    private String requireCompanyName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enter the canonical company name first.");
        }
        String clean = value.trim().replaceAll("\\s+", " ");
        if (clean.length() > 160) {
            throw new IllegalArgumentException("Company names must be 160 characters or fewer.");
        }
        return clean;
    }

    private final class MutableCompanyGroup {
        private final String key;
        private final Map<String, Long> aliasCounts = new LinkedHashMap<>();
        private final Set<String> domains = new LinkedHashSet<>();
        private long applications;
        private long openApplications;
        private LocalDate latestAppliedDate;

        private MutableCompanyGroup(String key) {
            this.key = key;
        }

        private void add(CompanyNameRow row) {
            aliasCounts.merge(row.companyName(), row.applications(), Long::sum);
            String domain = CompanyLogoService.normalizeDomain(row.companyDomain());
            if (domain != null) domains.add(domain);
            applications += row.applications();
            openApplications += row.openApplications();
            if (row.latestAppliedDate() != null
                    && (latestAppliedDate == null || row.latestAppliedDate().isAfter(latestAppliedDate))) {
                latestAppliedDate = row.latestAppliedDate();
            }
        }

        private CompanyGroup toImmutable() {
            String displayName = aliasCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                            .thenComparing(entry -> entry.getKey().length(), Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(key);
            List<String> aliases = aliasCounts.keySet().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            List<String> domainList = domains.stream().sorted().toList();
            boolean logoCached = domainList.size() == 1 && logoService.hasLogo(domainList.get(0));
            return new CompanyGroup(
                    key,
                    displayName,
                    aliases,
                    domainList,
                    applications,
                    openApplications,
                    latestAppliedDate,
                    logoCached);
        }
    }


    public record CompanyDetail(
            CompanyGroup group,
            List<CompanyApplicationRow> applications,
            List<CompanyContact> contacts,
            String notes) {
    }

    public record CompanySnapshot(
            long applications,
            long companies,
            long companiesWithDomain,
            long companiesWithLogo,
            long companiesNeedingAttention,
            long matchingCompanies,
            String query,
            String status,
            int page,
            int pageSize,
            int totalPages,
            int fromItem,
            int toItem,
            List<CompanyGroup> groups) {

        public boolean hasPrevious() {
            return page > 1;
        }

        public boolean hasNext() {
            return page < totalPages;
        }

        public int previousPage() {
            return Math.max(1, page - 1);
        }

        public int nextPage() {
            return Math.min(totalPages, page + 1);
        }
    }

    public record CompanyGroup(
            String key,
            String displayName,
            List<String> aliases,
            List<String> domains,
            long applications,
            long openApplications,
            LocalDate latestAppliedDate,
            boolean logoCached) {

        public boolean hasSingleDomain() {
            return domains.size() == 1;
        }

        public String domain() {
            return hasSingleDomain() ? domains.get(0) : null;
        }

        public boolean hasDomainConflict() {
            return domains.size() > 1;
        }

        public boolean needsAttention() {
            return domains.isEmpty() || hasDomainConflict() || !logoCached || aliases.size() > 1;
        }

        public String initials() {
            String[] words = displayName == null ? new String[0] : displayName.trim().split("\\s+");
            if (words.length == 0) return "?";
            if (words.length == 1) return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase(Locale.ROOT);
            return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
    }

    private record ContactValues(
            String name,
            String role,
            CompanyContactRelationship relationship,
            String email,
            String linkedinUrl,
            String notes) {
    }

    private record PhotoPayload(String mimeType, byte[] data) {
    }

    public record DomainUpdateResult(int applicationsUpdated, String domain, boolean logoFetched, String logoWarning) {
    }

    public record RenameResult(int applicationsUpdated, String canonicalName, String groupKey) {
    }

    public record MergeResult(int groupsMerged, int applicationsUpdated, String canonicalName) {
    }
}
