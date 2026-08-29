package com.brianna.jobsearch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.IndustryDomain;
import com.brianna.jobsearch.repository.NormalizationRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NormalizationServiceTest {

    @Test
    void careerSuggestionsSeparateRoleFamilyFromBusinessDomain() {
        NormalizationService service = new NormalizationService(new FakeNormalizationRepository());

        assertEquals(
                CareerRoleFamily.BACKEND_PLATFORM,
                service.suggestRoleFamily("Backend / Debit Processing / Payments Platform"));
        assertEquals(
                IndustryDomain.FINTECH_PAYMENTS,
                service.suggestIndustryDomain("Backend / Debit Processing / Payments Platform"));

        assertEquals(
                CareerRoleFamily.FORWARD_DEPLOYED_CUSTOMER_ENGINEERING,
                service.suggestRoleFamily("Forward Deployed Engineering / Manufacturing Software / Hard Tech"));
        assertEquals(
                IndustryDomain.MANUFACTURING_INDUSTRIAL,
                service.suggestIndustryDomain("Forward Deployed Engineering / Manufacturing Software / Hard Tech"));
    }

    @Test
    void backendQualificationWinsOverSecurityKeywordForRoleFamily() {
        NormalizationService service = new NormalizationService(new FakeNormalizationRepository());

        assertEquals(
                CareerRoleFamily.BACKEND_PLATFORM,
                service.suggestRoleFamily("Backend / Cloud Security / Distributed Systems / IAM"));
        assertEquals(
                IndustryDomain.SECURITY_IDENTITY,
                service.suggestIndustryDomain("Backend / Cloud Security / Distributed Systems / IAM"));
    }

    @Test
    void domainSuggestionsKeepComplianceDistinctFromGeneralFinancialServices() {
        NormalizationService service = new NormalizationService(new FakeNormalizationRepository());

        assertEquals(IndustryDomain.LEGAL_COMPLIANCE,
                service.suggestIndustryDomain("Backend / RegTech / Financial Crime & Risk Platform"));
        assertEquals(IndustryDomain.FINANCIAL_SERVICES,
                service.suggestIndustryDomain("Backend / Banktech Infrastructure / Financial Correctness Platform"));
    }

    @Test
    void sourceAndWorkSuggestionsOnlyCanonicalizeLabels() {
        NormalizationService service = new NormalizationService(new FakeNormalizationRepository());

        assertEquals("LinkedIn", service.suggestSourceLabel("linkedin"));
        assertEquals("Greenhouse", service.suggestSourceLabel("greenhouse"));
        assertEquals("Hybrid", service.suggestWorkArrangement("Hybrid / Remote flexible"));
        assertEquals("On-Site", service.suggestWorkArrangement("onsite"));
        assertEquals("Remote", service.suggestWorkArrangement("Remote (US)"));
    }

    @Test
    void snapshotShowsUnmappedCountsAndExistingMappings() {
        FakeNormalizationRepository repository = new FakeNormalizationRepository();
        repository.total = 12;
        repository.legacyTagged = 10;
        repository.needingMapping = 3;
        repository.careerRows.add(new NormalizationRepository.CareerTagGroupRow(
                "Backend / Fintech Platform",
                2,
                1,
                "BACKEND_PLATFORM",
                "FINTECH_PAYMENTS"));
        repository.sourceRows.add(new NormalizationRepository.TextValueGroupRow("linkedin", 4));
        repository.workRows.add(new NormalizationRepository.TextValueGroupRow("Onsite", 3));

        var snapshot = new NormalizationService(repository).snapshot(null, "UNMAPPED");

        assertEquals(12, snapshot.applications());
        assertEquals(10, snapshot.legacyCareerTagged());
        assertEquals(3, snapshot.careerApplicationsNeedingMapping());
        assertEquals(1, snapshot.careerGroups().size());
        assertEquals(1, snapshot.careerGroups().get(0).unmappedApplications());
        assertEquals("Backend / Platform", snapshot.careerGroups().get(0).existingRoleFamilies());
        assertEquals("Fintech & Payments", snapshot.careerGroups().get(0).existingIndustryDomains());
        assertEquals("LinkedIn", snapshot.sourceGroups().get(0).suggestedValue());
        assertEquals("On-Site", snapshot.workArrangementGroups().get(0).suggestedValue());
    }

    @Test
    void careerBulkMappingRequiresSelectionAndAtLeastOneTargetField() {
        NormalizationService service = new NormalizationService(new FakeNormalizationRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.applyCareerMapping(List.of(), CareerRoleFamily.BACKEND_PLATFORM, null, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> service.applyCareerMapping(List.of("Backend / Platform"), null, null, "", false));
    }


    @Test
    void suggestedBulkMappingAppliesDifferentMappingsPerSelectedTag() {
        FakeNormalizationRepository repository = new FakeNormalizationRepository();
        NormalizationService service = new NormalizationService(repository);

        var result = service.applySuggestedCareerMappings(List.of(
                "Backend / Debit Processing / Payments Platform",
                "Forward Deployed Engineering / Manufacturing Software / Hard Tech",
                "Mystery Role"));

        assertEquals(2, result.groupsApplied());
        assertEquals(1, result.groupsSkipped());
        assertEquals(2, repository.careerMappingCalls.size());
        assertEquals(CareerRoleFamily.BACKEND_PLATFORM, repository.careerMappingCalls.get(0).roleFamily());
        assertEquals(IndustryDomain.FINTECH_PAYMENTS, repository.careerMappingCalls.get(0).industryDomain());
        assertEquals(CareerRoleFamily.FORWARD_DEPLOYED_CUSTOMER_ENGINEERING, repository.careerMappingCalls.get(1).roleFamily());
        assertEquals(IndustryDomain.MANUFACTURING_INDUSTRIAL, repository.careerMappingCalls.get(1).industryDomain());
    }

    @Test
    void careerStatusFallsBackToUnmapped() {
        NormalizationService service = new NormalizationService(new FakeNormalizationRepository());

        assertEquals("UNMAPPED", service.normalizeCareerStatus(null));
        assertEquals("MAPPED", service.normalizeCareerStatus("mapped"));
        assertEquals("ALL", service.normalizeCareerStatus("all"));
        assertEquals("UNMAPPED", service.normalizeCareerStatus("bogus"));
    }

    private static final class FakeNormalizationRepository extends NormalizationRepository {
        long total;
        long legacyTagged;
        long needingMapping;
        List<CareerTagGroupRow> careerRows = new ArrayList<>();
        List<TextValueGroupRow> sourceRows = new ArrayList<>();
        List<TextValueGroupRow> workRows = new ArrayList<>();
        List<CareerMappingCall> careerMappingCalls = new ArrayList<>();

        FakeNormalizationRepository() {
            super(null);
        }

        @Override
        public long countApplications() {
            return total;
        }

        @Override
        public long countLegacyCareerTaggedApplications() {
            return legacyTagged;
        }

        @Override
        public long countCareerApplicationsNeedingMapping() {
            return needingMapping;
        }

        @Override
        public List<CareerTagGroupRow> findCareerTagGroups(String query, String status) {
            return careerRows;
        }

        @Override
        public List<TextValueGroupRow> findSourceGroups() {
            return sourceRows;
        }

        @Override
        public List<TextValueGroupRow> findWorkArrangementGroups() {
            return workRows;
        }

        @Override
        public int applyCareerMapping(
                List<String> legacyTags,
                CareerRoleFamily roleFamily,
                IndustryDomain industryDomain,
                String focus,
                boolean overwriteExisting) {
            careerMappingCalls.add(new CareerMappingCall(legacyTags, roleFamily, industryDomain, focus, overwriteExisting));
            return legacyTags.size();
        }
    }

    private record CareerMappingCall(
            List<String> legacyTags,
            CareerRoleFamily roleFamily,
            IndustryDomain industryDomain,
            String focus,
            boolean overwriteExisting) {
    }
}
