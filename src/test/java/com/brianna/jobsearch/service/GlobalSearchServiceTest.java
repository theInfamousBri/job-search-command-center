package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.CompanyContactRelationship;
import com.brianna.jobsearch.model.JobApplication;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

    @Mock private JobApplicationService applications;
    @Mock private CompanyManagementService companies;

    @Test
    void groupsApplicationCompanyAndPeopleResultsAndMarksExactRequisition() {
        JobApplication application = application();
        var company = new CompanyManagementService.CompanyGroup(
                "mastercard", "Mastercard", List.of("Mastercard"), List.of("mastercard.com"),
                4, 1, LocalDate.of(2026, 8, 30), true);
        CompanyContact person = new CompanyContact(
                7L, "mastercard", "Alex Morgan", "Technical Recruiter", CompanyContactRelationship.RECRUITER,
                "alex@mastercard.com", null, null, false,
                LocalDateTime.of(2026, 8, 1, 12, 0), LocalDateTime.of(2026, 8, 30, 12, 0), 2);
        var personHit = new CompanyManagementService.CompanyPersonSearchResult(person, "Mastercard");

        when(applications.searchGlobal("R-274666", 6)).thenReturn(List.of(application));
        when(companies.searchCompanies("R-274666", 5)).thenReturn(List.of(company));
        when(companies.searchPeople("R-274666", 6)).thenReturn(List.of(personHit));

        var response = new GlobalSearchService(applications, companies).search(" R-274666 ");

        assertThat(response.query()).isEqualTo("R-274666");
        assertThat(response.totalResults()).isEqualTo(3);
        assertThat(response.groups()).extracting(group -> group.label())
                .containsExactly("Applications", "Companies", "People");
        assertThat(response.groups().getFirst().results().getFirst().exactMatch()).isTrue();
        assertThat(response.groups().getFirst().results().getFirst().badge()).isEqualTo("Exact requisition");
        assertThat(response.groups().getFirst().results().getFirst().url()).isEqualTo("/applications/42");
        assertThat(response.groups().get(2).results().getFirst().url()).isEqualTo("/companies/mastercard#person-7");
    }

    @Test
    void blankSearchDoesNotQueryBackingServices() {
        var response = new GlobalSearchService(applications, companies).search("   ");

        assertThat(response.totalResults()).isZero();
        assertThat(response.groups()).isEmpty();
    }

    private JobApplication application() {
        JobApplication application = new JobApplication();
        application.setId(42L);
        application.setCompany("Mastercard");
        application.setRole("Senior Software Engineer");
        application.setLocation("United States");
        application.setWorkArrangement("Remote");
        application.setRequisitionId("R-274666");
        application.setStatus(ApplicationStatus.APPLIED);
        application.setState(ApplicationState.ACTIVE);
        return application;
    }
}
