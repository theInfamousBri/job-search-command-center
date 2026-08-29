package com.brianna.jobsearch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.CompanyContactRelationship;
import com.brianna.jobsearch.repository.CompanyLogoRepository.CompanyLogo;
import com.brianna.jobsearch.repository.CompanyManagementRepository;
import com.brianna.jobsearch.repository.CompanyManagementRepository.CompanyNameRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompanyManagementServiceTest {

    @Test
    void normalizesPunctuationAndCommonLegalSuffixesIntoOneCompanyGroup() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Acme, Inc.", "acme.com", 2, 1, LocalDate.of(2026, 8, 1)));
        repository.rows.add(new CompanyNameRow("ACME INC", null, 1, 0, LocalDate.of(2026, 7, 1)));
        FakeLogoService logos = new FakeLogoService();
        logos.cached.add("acme.com");

        CompanyManagementService service = new CompanyManagementService(repository, logos);
        var groups = service.snapshot(null, null).groups();

        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).applications());
        assertEquals(2, groups.get(0).aliases().size());
        assertEquals("acme.com", groups.get(0).domain());
        assertTrue(groups.get(0).logoCached());
    }

    @Test
    void settingDomainPropagatesAcrossAliasesAndRefreshesLogo() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Acme, Inc.", null, 2, 1, null));
        repository.rows.add(new CompanyNameRow("ACME INC", null, 1, 0, null));
        FakeLogoService logos = new FakeLogoService();
        CompanyManagementService service = new CompanyManagementService(repository, logos);

        String groupKey = service.snapshot(null, null).groups().get(0).key();
        var result = service.setDomain(groupKey, "https://www.acme.com/about", true);

        assertEquals(3, result.applicationsUpdated());
        assertEquals("acme.com", result.domain());
        assertEquals(List.of("ACME INC", "Acme, Inc."), repository.lastDomainCompanies.stream().sorted().toList());
        assertEquals("acme.com", repository.lastDomain);
        assertEquals("acme.com", logos.lastFetched);
    }

    @Test
    void domainSaveSurvivesLogoFetchFailure() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Example", null, 4, 1, null));
        FakeLogoService logos = new FakeLogoService();
        logos.failFetch = true;
        CompanyManagementService service = new CompanyManagementService(repository, logos);

        String key = service.snapshot(null, null).groups().get(0).key();
        var result = service.setDomain(key, "example.com", true);

        assertEquals(4, result.applicationsUpdated());
        assertEquals("example.com", repository.lastDomain);
        assertFalse(result.logoFetched());
        assertTrue(result.logoWarning().contains("blocked"));
    }

    @Test
    void mergeRequiresTwoCurrentGroupsAndUsesCanonicalName() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Schwab", null, 2, 1, null));
        repository.rows.add(new CompanyNameRow("Charles Schwab", "schwab.com", 3, 0, null));
        CompanyManagementService service = new CompanyManagementService(repository, new FakeLogoService());
        var groups = service.snapshot(null, null).groups();

        var result = service.mergeGroups(groups.stream().map(CompanyManagementService.CompanyGroup::key).toList(), "Charles Schwab");

        assertEquals(2, result.groupsMerged());
        assertEquals(5, result.applicationsUpdated());
        assertEquals("Charles Schwab", repository.lastCanonicalName);
        assertEquals(Set.of("Schwab", "Charles Schwab"), Set.copyOf(repository.lastRenameCompanies));

        assertThrows(IllegalArgumentException.class,
                () -> service.mergeGroups(List.of(groups.get(0).key()), "Charles Schwab"));
    }


    @Test
    void companyDirectoryPaginatesAfterFiltering() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        for (int i = 1; i <= 45; i++) {
            repository.rows.add(new CompanyNameRow(
                    "Company " + i,
                    "company" + i + ".com",
                    1,
                    0,
                    LocalDate.of(2026, 8, 1)));
        }
        CompanyManagementService service = new CompanyManagementService(repository, new FakeLogoService());

        var page = service.snapshot(null, null, 2, 20);

        assertEquals(45, page.matchingCompanies());
        assertEquals(2, page.page());
        assertEquals(20, page.pageSize());
        assertEquals(3, page.totalPages());
        assertEquals(21, page.fromItem());
        assertEquals(40, page.toItem());
        assertEquals(20, page.groups().size());
        assertTrue(page.hasPrevious());
        assertTrue(page.hasNext());
    }

    @Test
    void renameReturnsTheNewNormalizedCompanyKey() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Schwab", "schwab.com", 2, 1, null));
        CompanyManagementService service = new CompanyManagementService(repository, new FakeLogoService());

        String key = service.snapshot(null, null).groups().get(0).key();
        var result = service.renameGroup(key, "Charles Schwab");

        assertEquals(2, result.applicationsUpdated());
        assertEquals("Charles Schwab", result.canonicalName());
        assertEquals("charles schwab", result.groupKey());
    }

    @Test
    void suggestsSharedDomainForKnownCompanyName() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Acme, Inc.", "acme.com", 2, 1, null));
        CompanyManagementService service = new CompanyManagementService(repository, new FakeLogoService());

        assertEquals("acme.com", service.knownDomainForCompany("ACME").orElseThrow());
        assertTrue(service.knownDomainForCompany("Unknown Company").isEmpty());
    }


    @Test
    void createsAndUpdatesCompanyContactsWithoutTouchingApplicationRows() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Acme", "acme.com", 2, 1, null));
        CompanyManagementService service = new CompanyManagementService(repository, new FakeLogoService());
        String key = service.snapshot(null, null).groups().get(0).key();

        var created = service.createContact(
                key,
                "Alicia Smith",
                "Technical Recruiter",
                "RECRUITER",
                "alicia@acme.com",
                "linkedin.com/in/alicia-smith",
                "Primary recruiting contact.",
                null);

        assertEquals("Alicia Smith", created.name());
        assertEquals(CompanyContactRelationship.RECRUITER, created.relationship());
        assertEquals("https://linkedin.com/in/alicia-smith", created.linkedinUrl());

        var updated = service.updateContact(
                key,
                created.id(),
                "Alicia Smith",
                "Senior Technical Recruiter",
                "RECRUITER",
                "alicia@acme.com",
                created.linkedinUrl(),
                "Follow up after final round.",
                null,
                false);

        assertEquals("Senior Technical Recruiter", updated.role());
        assertEquals("Follow up after final round.", updated.notes());
    }

    @Test
    void renameMovesCompanyContactsToTheNewCompanyKey() {
        FakeCompanyRepository repository = new FakeCompanyRepository();
        repository.rows.add(new CompanyNameRow("Schwab", "schwab.com", 2, 1, null));
        CompanyManagementService service = new CompanyManagementService(repository, new FakeLogoService());

        String key = service.snapshot(null, null).groups().get(0).key();
        service.renameGroup(key, "Charles Schwab");

        assertEquals(List.of("schwab"), repository.lastMovedContactKeys);
        assertEquals("charles schwab", repository.lastMovedContactTarget);
    }

    private static final class FakeCompanyRepository extends CompanyManagementRepository {
        final List<CompanyNameRow> rows = new ArrayList<>();
        List<String> lastDomainCompanies = List.of();
        String lastDomain;
        List<String> lastRenameCompanies = List.of();
        String lastCanonicalName;
        List<String> lastMovedNoteKeys = List.of();
        String lastMovedNoteTarget;
        List<String> lastMovedContactKeys = List.of();
        String lastMovedContactTarget;
        final List<CompanyContact> contacts = new ArrayList<>();
        long nextContactId = 1;

        FakeCompanyRepository() {
            super(null);
        }

        @Override
        public List<CompanyNameRow> findCompanyNames() {
            return rows;
        }

        @Override
        public int updateDomainForCompanyNames(List<String> companyNames, String domain) {
            lastDomainCompanies = List.copyOf(companyNames);
            lastDomain = domain;
            return rows.stream().filter(row -> companyNames.contains(row.companyName())).mapToInt(row -> (int) row.applications()).sum();
        }

        @Override
        public int renameCompanyNames(List<String> companyNames, String canonicalName) {
            lastRenameCompanies = List.copyOf(companyNames);
            lastCanonicalName = canonicalName;
            return rows.stream().filter(row -> companyNames.contains(row.companyName())).mapToInt(row -> (int) row.applications()).sum();
        }

        @Override
        public long insertContact(
                String companyKey, String name, String role, String relationshipType, String email,
                String linkedinUrl, String notes, String photoMimeType, byte[] photoData) {
            long id = nextContactId++;
            contacts.add(new CompanyContact(
                    id, companyKey, name, role, CompanyContactRelationship.valueOf(relationshipType),
                    email, linkedinUrl, notes, photoData != null, LocalDateTime.now(), LocalDateTime.now()));
            return id;
        }

        @Override
        public CompanyContact findContact(long id) {
            return contacts.stream().filter(contact -> contact.id() == id).findFirst().orElse(null);
        }

        @Override
        public int updateContact(
                long id, String name, String role, String relationshipType, String email, String linkedinUrl,
                String notes, boolean replacePhoto, String photoMimeType, byte[] photoData) {
            CompanyContact existing = findContact(id);
            if (existing == null) return 0;
            contacts.remove(existing);
            contacts.add(new CompanyContact(
                    id, existing.companyKey(), name, role, CompanyContactRelationship.valueOf(relationshipType),
                    email, linkedinUrl, notes, replacePhoto ? photoData != null : existing.hasPhoto(),
                    existing.createdAt(), LocalDateTime.now()));
            return 1;
        }

        @Override
        public void moveCompanyContacts(List<String> oldKeys, String newKey) {
            lastMovedContactKeys = List.copyOf(oldKeys);
            lastMovedContactTarget = newKey;
        }

        @Override
        public void moveCompanyNotes(List<String> oldKeys, String newKey) {
            lastMovedNoteKeys = List.copyOf(oldKeys);
            lastMovedNoteTarget = newKey;
        }
    }

    private static final class FakeLogoService extends CompanyLogoService {
        final Set<String> cached = new HashSet<>();
        String lastFetched;
        boolean failFetch;

        FakeLogoService() {
            super(null);
        }

        @Override
        public boolean hasLogo(String rawDomain) {
            String domain = normalizeDomain(rawDomain);
            return domain != null && cached.contains(domain);
        }

        @Override
        public CompanyLogo fetchAndCache(String rawDomain) {
            String domain = normalizeDomain(rawDomain);
            lastFetched = domain;
            if (failFetch) {
                throw new IllegalArgumentException("Site blocked automated requests.");
            }
            cached.add(domain);
            return new CompanyLogo(domain, "image/png", new byte[] {1}, "test", LocalDateTime.now());
        }
    }
}
