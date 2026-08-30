package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.IndustryDomain;
import com.brianna.jobsearch.repository.NormalizationRepository;
import com.brianna.jobsearch.repository.NormalizationRepository.CareerTagGroupRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NormalizationService {

    private final NormalizationRepository repository;

    public NormalizationService(NormalizationRepository repository) {
        this.repository = repository;
    }

    public NormalizationSnapshot snapshot(String query, String careerStatus) {
        String normalizedStatus = normalizeCareerStatus(careerStatus);
        List<CareerTagGroup> careerGroups = repository.findCareerTagGroups(query, normalizedStatus).stream()
                .map(this::toCareerGroup)
                .toList();
        List<TextValueGroup> sourceGroups = repository.findSourceGroups().stream()
                .map(row -> new TextValueGroup(row.value(), row.applicationCount(), suggestSourceLabel(row.value())))
                .toList();
        List<TextValueGroup> workArrangementGroups = repository.findWorkArrangementGroups().stream()
                .map(row -> new TextValueGroup(row.value(), row.applicationCount(), suggestWorkArrangement(row.value())))
                .toList();

        long total = repository.countApplications();
        long legacyTagged = repository.countLegacyCareerTaggedApplications();
        long needingCareerMapping = repository.countCareerApplicationsNeedingMapping();

        return new NormalizationSnapshot(
                total,
                legacyTagged,
                needingCareerMapping,
                careerGroups,
                sourceGroups,
                workArrangementGroups,
                query == null ? "" : query.trim(),
                normalizedStatus);
    }

    @Transactional
    public int applyCareerMapping(
            List<String> legacyTags,
            CareerRoleFamily roleFamily,
            IndustryDomain industryDomain,
            String focus,
            boolean overwriteExisting) {

        if (legacyTags == null || legacyTags.isEmpty()) {
            throw new IllegalArgumentException("Select at least one original career tag to map.");
        }
        if (roleFamily == null && industryDomain == null && (focus == null || focus.isBlank())) {
            throw new IllegalArgumentException("Choose a Role Family, Industry / Domain, or Focus before applying the mapping.");
        }
        return repository.applyCareerMapping(legacyTags, roleFamily, industryDomain, focus, overwriteExisting);
    }

    @Transactional
    public SuggestedMappingResult applySuggestedCareerMappings(List<String> legacyTags) {
        if (legacyTags == null || legacyTags.isEmpty()) {
            throw new IllegalArgumentException("Select at least one original career tag to map.");
        }

        int applicationsReviewed = 0;
        int groupsApplied = 0;
        int groupsSkipped = 0;

        for (String legacyTag : legacyTags.stream().filter(java.util.Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().toList()) {
            CareerRoleFamily roleFamily = suggestRoleFamily(legacyTag);
            IndustryDomain industryDomain = suggestIndustryDomain(legacyTag);
            if (roleFamily == null && industryDomain == null) {
                groupsSkipped++;
                continue;
            }
            applicationsReviewed += repository.applyCareerMapping(
                    List.of(legacyTag), roleFamily, industryDomain, null, false);
            groupsApplied++;
        }

        if (groupsApplied == 0) {
            throw new IllegalArgumentException("None of the selected tags has a confident keyword suggestion. Review those tags manually instead.");
        }
        return new SuggestedMappingResult(applicationsReviewed, groupsApplied, groupsSkipped);
    }

    @Transactional
    public int normalizeSources(List<String> values, String target) {
        requireTextNormalization(values, target, "source");
        return repository.normalizeSourceValues(values, target.trim());
    }

    @Transactional
    public int normalizeWorkArrangements(List<String> values, String target) {
        requireTextNormalization(values, target, "work arrangement");
        return repository.normalizeWorkArrangementValues(values, target.trim());
    }

    private void requireTextNormalization(List<String> values, String target, String label) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Select at least one " + label + " value to normalize.");
        }
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Enter the canonical " + label + " label to use.");
        }
    }

    private CareerTagGroup toCareerGroup(CareerTagGroupRow row) {
        CareerRoleFamily suggestedRole = suggestRoleFamily(row.legacyTag());
        IndustryDomain suggestedDomain = suggestIndustryDomain(row.legacyTag());
        return new CareerTagGroup(
                row.legacyTag(),
                row.applicationCount(),
                row.mappedCount(),
                Math.max(0L, row.applicationCount() - row.mappedCount()),
                displayRoleFamilies(row.roleFamilies()),
                displayIndustryDomains(row.industryDomains()),
                suggestedRole,
                suggestedDomain);
    }

    private String displayRoleFamilies(String values) {
        return displayEnumCsv(values, true);
    }

    private String displayIndustryDomains(String values) {
        return displayEnumCsv(values, false);
    }

    private String displayEnumCsv(String values, boolean roles) {
        if (values == null || values.isBlank()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (String value : values.split(",")) {
            String trimmed = value.trim();
            if (trimmed.isBlank()) continue;
            try {
                labels.add(roles
                        ? CareerRoleFamily.valueOf(trimmed).getDisplayName()
                        : IndustryDomain.valueOf(trimmed).getDisplayName());
            } catch (IllegalArgumentException ignored) {
                labels.add(trimmed);
            }
        }
        return String.join(" · ", labels);
    }

    String normalizeCareerStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNMAPPED";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return List.of("UNMAPPED", "MAPPED", "ALL").contains(normalized) ? normalized : "UNMAPPED";
    }

    CareerRoleFamily suggestRoleFamily(String legacyTag) {
        String value = normalized(legacyTag);
        if (value.isBlank()) return null;

        if (containsAny(value, "full-stack", "full stack")) return CareerRoleFamily.FULL_STACK;
        if (containsAny(value, "forward deployed", "customer engineering")) {
            return CareerRoleFamily.FORWARD_DEPLOYED_CUSTOMER_ENGINEERING;
        }
        if (containsAny(value, "robotics", "autonomy", "embedded")) {
            return CareerRoleFamily.ROBOTICS_EMBEDDED_AUTONOMY;
        }
        if (containsAny(value, "frontend", "front-end")) return CareerRoleFamily.FRONTEND;
        if (containsAny(value, "mobile", "android", "ios")) return CareerRoleFamily.MOBILE;

        if (containsAny(value, "cloud infrastructure", "cloud observability", "cloud storage")) {
            return CareerRoleFamily.CLOUD_INFRASTRUCTURE;
        }

        // A backend-qualified domain is still primarily a backend role. Keep this
        // ahead of Security/Data keyword matching so tags such as
        // "Backend / Cloud Security" do not become Security roles by accident.
        if (containsAny(value, "backend", "java backend", "backend platform", "backend product")) {
            return CareerRoleFamily.BACKEND_PLATFORM;
        }
        if (containsAny(value, "devops", "sre", "production engineering", "database support")) {
            return CareerRoleFamily.DEVOPS_SRE;
        }
        if (containsAny(value, "cybersecurity", "secure identity", "security /", "security platform")) {
            return CareerRoleFamily.SECURITY;
        }
        if (containsAny(value, "data platform", "data engineering", "analytics")) {
            return CareerRoleFamily.DATA_ANALYTICS;
        }
        if (containsAny(value, "product engineering", "ai /", "software /", "marketplace / platform")) {
            return CareerRoleFamily.PRODUCT_ENGINEERING;
        }
        return null;
    }

    IndustryDomain suggestIndustryDomain(String legacyTag) {
        String value = normalized(legacyTag);
        if (value.isBlank()) return null;

        if (containsAny(value, "payment", "payments", "fintech", "debit", "digital wallet", "money movement", "cross-border")) {
            return IndustryDomain.FINTECH_PAYMENTS;
        }
        if (containsAny(value, "legal tech", "regtech", "compliance", "financial crime")) {
            return IndustryDomain.LEGAL_COMPLIANCE;
        }
        if (containsAny(value, "banking", "banktech", "capital markets", "financial services", "financial", "finance", "retirement", "mortgage", "insurance", "private markets", "broker")) {
            return IndustryDomain.FINANCIAL_SERVICES;
        }
        if (containsAny(value, "healthcare", "healthtech", "care management", "revenue cycle")) {
            return IndustryDomain.HEALTHCARE;
        }
        if (containsAny(value, "cybersecurity", "identity", " iam", "iam,", "security", "fraud infrastructure")) {
            return IndustryDomain.SECURITY_IDENTITY;
        }
        if (containsAny(value, "aerospace", "defense")) return IndustryDomain.AEROSPACE_DEFENSE;
        if (containsAny(value, "government", "govcloud", "federal")) return IndustryDomain.GOVERNMENT;
        if (containsAny(value, "automotive")) return IndustryDomain.AUTOMOTIVE;
        if (containsAny(value, "manufacturing", "industrial automation", "hard tech")) {
            return IndustryDomain.MANUFACTURING_INDUSTRIAL;
        }
        if (containsAny(value, "robotics", "autonomy")) return IndustryDomain.ROBOTICS_AUTONOMY;
        if (containsAny(value, "logistics", "supply chain")) return IndustryDomain.LOGISTICS_SUPPLY_CHAIN;
        if (containsAny(value, "travel", "restaurant")) return IndustryDomain.TRAVEL_HOSPITALITY;
        if (containsAny(value, "climate", "waste management")) return IndustryDomain.CLIMATE_SUSTAINABILITY;
        if (containsAny(value, "education", "edtech")) return IndustryDomain.EDUCATION;
        if (containsAny(value, "media", "video")) return IndustryDomain.MEDIA_STREAMING;
        if (containsAny(value, "e-commerce", "ecommerce", "commerce", "marketplace", "shopping")) {
            return IndustryDomain.ECOMMERCE;
        }
        if (containsAny(value, "consumer", "social", "fitness", "sports gaming")) return IndustryDomain.CONSUMER;
        if (containsAny(value, "developer platform", "developer productivity", "developer tools", "open source")) {
            return IndustryDomain.DEVELOPER_TOOLS;
        }
        if (containsAny(value, "cloud storage", "cloud infrastructure", "cloud observability", "database")) {
            return IndustryDomain.CLOUD_INFRASTRUCTURE;
        }
        if (containsAny(value, "saas", "enterprise platform", "b2b", "smb", "loyalty", "compensation")) {
            return IndustryDomain.ENTERPRISE_SAAS;
        }
        return null;
    }

    String suggestSourceLabel(String source) {
        String value = normalized(source);
        if (value.isBlank()) return "";
        if (containsAny(value, "linkedin")) return "LinkedIn";
        if (containsAny(value, "wellfound", "angellist")) return "Wellfound";
        if (containsAny(value, "greenhouse")) return "Greenhouse";
        if (containsAny(value, "ashby")) return "Ashby";
        if (containsAny(value, "indeed")) return "Indeed";
        if (containsAny(value, "handshake")) return "Handshake";
        if (containsAny(value, "referral")) return "Referral";
        if (containsAny(value, "recruiter")) return "Recruiter";
        if (containsAny(value, "company site", "company website", "career site", "careers page")) return "Company Site";
        return titleCase(source);
    }

    String suggestWorkArrangement(String arrangement) {
        String value = normalized(arrangement);
        if (value.isBlank()) return "";
        if (value.contains("hybrid")) return "Hybrid";
        if (containsAny(value, "on-site", "on site", "onsite")) return "On-Site";
        if (value.contains("remote")) return "Remote";
        return titleCase(arrangement);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... terms) {
        return Arrays.stream(terms).anyMatch(value::contains);
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) return "";
        String[] parts = value.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1));
        }
        return result.toString();
    }

    public record SuggestedMappingResult(int applicationsReviewed, int groupsApplied, int groupsSkipped) {
    }

    public record NormalizationSnapshot(
            long applications,
            long legacyCareerTagged,
            long careerApplicationsNeedingMapping,
            List<CareerTagGroup> careerGroups,
            List<TextValueGroup> sourceGroups,
            List<TextValueGroup> workArrangementGroups,
            String query,
            String careerStatus) {
    }

    public record CareerTagGroup(
            String legacyTag,
            long applications,
            long mappedApplications,
            long unmappedApplications,
            String existingRoleFamilies,
            String existingIndustryDomains,
            CareerRoleFamily suggestedRoleFamily,
            IndustryDomain suggestedIndustryDomain) {

        public boolean isFullyMapped() {
            return applications > 0 && mappedApplications == applications;
        }

        public boolean hasSuggestion() {
            return suggestedRoleFamily != null || suggestedIndustryDomain != null;
        }
    }

    public record TextValueGroup(String value, long applications, String suggestedValue) {
        public boolean hasDifferentSuggestion() {
            return suggestedValue != null && !suggestedValue.isBlank() && !suggestedValue.equals(value);
        }
    }
}
