package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.CompanyContactRelationship;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.repository.ApplicationContactRepository;
import com.brianna.jobsearch.repository.CompanyManagementRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationContactServiceTest {

    @Mock
    private ApplicationContactRepository repository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private CompanyManagementRepository companyRepository;

    @InjectMocks
    private ApplicationContactService service;

    @Test
    void linksCompanyPersonWithoutDuplicatingRelationship() {
        JobApplication application = application(42L, "Northstar Labs");
        CompanyContact contact = contact(7L, "northstar labs", "Alex Morgan");
        when(applicationRepository.findById(42L)).thenReturn(Optional.of(application));
        when(companyRepository.findContact(7L)).thenReturn(contact);
        when(repository.link(42L, 7L)).thenReturn(true);

        assertThat(service.link(42L, 7L)).isTrue();
        verify(repository).link(42L, 7L);
    }

    @Test
    void rejectsCrossCompanyLink() {
        JobApplication application = application(42L, "Northstar Labs");
        CompanyContact contact = contact(9L, "atlas payments", "Maya Chen");
        when(applicationRepository.findById(42L)).thenReturn(Optional.of(application));
        when(companyRepository.findContact(9L)).thenReturn(contact);

        assertThatThrownBy(() -> service.link(42L, 9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maya Chen belongs to a different company and cannot be linked to this application.");
        verify(repository, never()).link(42L, 9L);
    }

    @Test
    void linkablePeopleAreRestrictedToNormalizedApplicationCompany() {
        JobApplication application = application(42L, "Northstar Labs, Inc.");
        when(applicationRepository.findById(42L)).thenReturn(Optional.of(application));

        service.linkableForApplication(42L);

        verify(repository).findLinkableForApplication(42L, "northstar labs");
    }

    @Test
    void missingApplicationAndPersonUse404StyleErrors() {
        when(applicationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.forApplication(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found: 404");

        JobApplication application = application(42L, "Northstar Labs");
        when(applicationRepository.findById(42L)).thenReturn(Optional.of(application));
        when(companyRepository.findContact(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.link(42L, 404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Person not found: 404");
    }

    private JobApplication application(long id, String company) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setCompany(company);
        application.setRole("Software Engineer");
        application.setStatus(ApplicationStatus.APPLIED);
        application.setState(ApplicationState.ACTIVE);
        return application;
    }

    private CompanyContact contact(long id, String companyKey, String name) {
        return new CompanyContact(
                id, companyKey, name, "Technical Recruiter", CompanyContactRelationship.RECRUITER,
                "person@example.com", null, null, false,
                LocalDateTime.of(2026, 8, 30, 12, 0), LocalDateTime.of(2026, 8, 30, 12, 0), 0);
    }
}
