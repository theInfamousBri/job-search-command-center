package com.brianna.jobsearch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.CompensationContext;
import com.brianna.jobsearch.model.JobApplication;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompensationServiceTest {

    @Mock private JobApplicationService applications;

    private CompensationService service;

    @BeforeEach
    void setUp() {
        service = new CompensationService(applications);
    }

    @Test
    void parsesCommaSeparatedAnnualRange() {
        var range = service.parseAnnualSalary("$112,710 - $183,140").orElseThrow();
        assertEquals(112_710d, range.min());
        assertEquals(183_140d, range.max());
    }

    @Test
    void parsesCompactKRange() {
        var range = service.parseAnnualSalary("$150k–$180k + bonus").orElseThrow();
        assertEquals(150_000d, range.min());
        assertEquals(180_000d, range.max());
    }

    @Test
    void parsesRangeWithSharedKSuffix() {
        var range = service.parseAnnualSalary("$115–184k + bonus").orElseThrow();
        assertEquals(115_000d, range.min());
        assertEquals(184_000d, range.max());
    }

    @Test
    void usesFirstAnnualRangeWhenCompensationAlsoMentionsAnIncentiveTarget() {
        var range = service.parseAnnualSalary("$91,000–$185,900 base + incentive; target ~$180,000").orElseThrow();
        assertEquals(91_000d, range.min());
        assertEquals(185_900d, range.max());
    }

    @Test
    void parsesSingleAnnualSalary() {
        var range = service.parseAnnualSalary("Base salary $172,500").orElseThrow();
        assertEquals(172_500d, range.min());
        assertEquals(172_500d, range.max());
    }

    @Test
    void ignoresHourlyCompensationRatherThanMixingUnits() {
        assertTrue(service.parseAnnualSalary("$72–$88 per hour").isEmpty());
        assertTrue(service.parseAnnualSalary("$75/hr").isEmpty());
    }

    @Test
    void usesSameRoleFamilyWhenThereIsEnoughComparableData() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$160k–$185k");
        when(applications.findAll()).thenReturn(List.of(
                target,
                application(2L, CareerRoleFamily.BACKEND_PLATFORM, "$140k–$160k"),
                application(3L, CareerRoleFamily.BACKEND_PLATFORM, "$150k–$170k"),
                application(4L, CareerRoleFamily.BACKEND_PLATFORM, "$170k–$190k"),
                application(5L, CareerRoleFamily.FRONTEND, "$95k–$110k")));

        CompensationContext context = service.contextFor(target);

        assertTrue(context.benchmarkAvailable());
        assertEquals(3, context.sampleSize());
        assertEquals("Backend / Platform roles", context.comparisonLabel());
        assertEquals("$160k", context.medianDisplay());
        assertEquals("Spans tracked median", context.positionLabel());
        assertEquals("Directional", context.sampleStrength());
        assertEquals("$172.5k", context.targetMidpointDisplay());
        assertEquals("+8% vs median", context.midpointDeltaDisplay());
    }

    @Test
    void fallsBackToAllTrackedRolesWhenRoleFamilySampleIsTooSmall() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$160k–$185k");
        when(applications.findAll()).thenReturn(List.of(
                target,
                application(2L, CareerRoleFamily.BACKEND_PLATFORM, "$145k–$165k"),
                application(3L, CareerRoleFamily.FRONTEND, "$120k–$140k"),
                application(4L, CareerRoleFamily.DATA_ANALYTICS, "$130k–$150k"),
                application(5L, CareerRoleFamily.CLOUD_INFRASTRUCTURE, "$155k–$175k")));

        CompensationContext context = service.contextFor(target);

        assertTrue(context.benchmarkAvailable());
        assertEquals(4, context.sampleSize());
        assertEquals("All tracked roles", context.comparisonLabel());
        assertTrue(context.comparisonNote().contains("Not enough salary data exists in this role family"));
    }

    @Test
    void returnsSampleFallbackWhenThereAreTooFewComparableSalaries() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$160k–$185k");
        when(applications.findAll()).thenReturn(List.of(
                target,
                application(2L, CareerRoleFamily.BACKEND_PLATFORM, "$145k–$165k"),
                application(3L, CareerRoleFamily.FRONTEND, null)));

        CompensationContext context = service.contextFor(target);

        assertTrue(context.targetParsed());
        assertFalse(context.benchmarkAvailable());
        assertEquals(1, context.sampleSize());
        assertTrue(context.message().contains("at least 3 other tracked roles"));
    }

    @Test
    void returnsHelpfulFallbackWhenTargetSalaryCannotBeCompared() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$82/hour");

        CompensationContext context = service.contextFor(target);

        assertFalse(context.targetParsed());
        assertFalse(context.benchmarkAvailable());
        assertTrue(context.message().contains("annual salary"));
    }

    @Test
    void identifiesRangeAboveTrackedMiddleFiftyPercent() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$200k–$220k");
        when(applications.findAll()).thenReturn(List.of(
                target,
                application(2L, CareerRoleFamily.BACKEND_PLATFORM, "$120k–$140k"),
                application(3L, CareerRoleFamily.BACKEND_PLATFORM, "$140k–$160k"),
                application(4L, CareerRoleFamily.BACKEND_PLATFORM, "$160k–$180k")));

        CompensationContext context = service.contextFor(target);

        assertEquals("Entire range above tracked median", context.positionLabel());
        assertTrue(context.positionDescription().contains("full range sits above"));
        assertTrue(context.targetLeftPercent() > context.medianPercent());
    }

    @Test
    void refinesLargeRoleFamilySampleByWorkArrangement() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$160k–$185k");
        target.setWorkArrangement("Remote");

        List<JobApplication> tracked = new java.util.ArrayList<>();
        tracked.add(target);
        for (long id = 2; id <= 11; id++) {
            JobApplication remote = application(id, CareerRoleFamily.BACKEND_PLATFORM, "$150k–$180k");
            remote.setWorkArrangement(id % 2 == 0 ? "Remote" : " remote ");
            tracked.add(remote);
        }
        JobApplication hybrid = application(20L, CareerRoleFamily.BACKEND_PLATFORM, "$220k–$240k");
        hybrid.setWorkArrangement("Hybrid");
        tracked.add(hybrid);
        when(applications.findAll()).thenReturn(tracked);

        CompensationContext context = service.contextFor(target);

        assertEquals(10, context.sampleSize());
        assertEquals("Stronger sample", context.sampleStrength());
        assertEquals("Backend / Platform · Remote roles", context.comparisonLabel());
        assertEquals("$165k", context.medianDisplay());
    }

    @Test
    void doesNotOverRefineWorkArrangementWhenTheSampleWouldBeTooSmall() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$160k–$185k");
        target.setWorkArrangement("Remote");

        List<JobApplication> tracked = new java.util.ArrayList<>();
        tracked.add(target);
        for (long id = 2; id <= 10; id++) {
            JobApplication remote = application(id, CareerRoleFamily.BACKEND_PLATFORM, "$150k–$180k");
            remote.setWorkArrangement("Remote");
            tracked.add(remote);
        }
        JobApplication hybrid = application(20L, CareerRoleFamily.BACKEND_PLATFORM, "$220k–$240k");
        hybrid.setWorkArrangement("Hybrid");
        tracked.add(hybrid);
        when(applications.findAll()).thenReturn(tracked);

        CompensationContext context = service.contextFor(target);

        assertEquals(10, context.sampleSize());
        assertEquals("Backend / Platform roles", context.comparisonLabel());
    }

    @Test
    void describesRangePositionMorePrecisely() {
        JobApplication target = application(1L, CareerRoleFamily.BACKEND_PLATFORM, "$145k–$155k");
        when(applications.findAll()).thenReturn(List.of(
                target,
                application(2L, CareerRoleFamily.BACKEND_PLATFORM, "$140k–$150k"),
                application(3L, CareerRoleFamily.BACKEND_PLATFORM, "$150k–$160k"),
                application(4L, CareerRoleFamily.BACKEND_PLATFORM, "$160k–$170k"),
                application(5L, CareerRoleFamily.BACKEND_PLATFORM, "$170k–$180k")));

        CompensationContext context = service.contextFor(target);

        assertEquals("Entire range below tracked median", context.positionLabel());
        assertTrue(context.positionDescription().contains("lower end")
                || context.positionDescription().contains("inside"));
    }

    private JobApplication application(long id, CareerRoleFamily roleFamily, String salary) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setCompany("Company " + id);
        application.setRole("Role " + id);
        application.setRoleFamily(roleFamily);
        application.setSalary(salary);
        return application;
    }
}
