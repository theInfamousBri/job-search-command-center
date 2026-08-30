package com.brianna.jobsearch.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.CompanyContactRelationship;
import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.IndustryDomain;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.service.ApplicationAttachmentService;
import com.brianna.jobsearch.service.ApplicationContactService;
import com.brianna.jobsearch.service.ApplicationImportService;
import com.brianna.jobsearch.service.CompanyLogoService;
import com.brianna.jobsearch.service.CompanyManagementService;
import com.brianna.jobsearch.service.JobApplicationService;
import com.brianna.jobsearch.service.MaterialService;
import com.brianna.jobsearch.service.PrepService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class JobApplicationControllerTest {

    @Mock private JobApplicationService applications;
    @Mock private PrepService prep;
    @Mock private ApplicationImportService imports;
    @Mock private ApplicationAttachmentService attachments;
    @Mock private ApplicationContactService contacts;
    @Mock private CompanyLogoService logos;
    @Mock private CompanyManagementService companies;
    @Mock private MaterialService materials;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        JobApplicationController controller = new JobApplicationController(
                applications, prep, imports, attachments, contacts, logos, companies, materials);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void applicationDetailIncludesLinkedAndLinkablePeople() throws Exception {
        JobApplication application = application(42L);
        CompanyContact linked = contact(7L, "Alex Morgan");
        CompanyContact linkable = contact(8L, "Taylor Reed");
        when(applications.get(42L)).thenReturn(application);
        when(applications.eventsForApplication(42L)).thenReturn(List.of());
        when(prep.forApplication(42L)).thenReturn(List.of());
        when(prep.linkableReusableForApplication(42L)).thenReturn(List.of());
        when(attachments.forApplication(42L)).thenReturn(List.of());
        when(materials.forApplication(42L)).thenReturn(List.of());
        when(materials.linkableForApplication(42L)).thenReturn(List.of());
        when(contacts.forApplication(42L)).thenReturn(List.of(linked));
        when(contacts.linkableForApplication(42L)).thenReturn(List.of(linkable));
        when(logos.hasLogo("northstarlabs.com")).thenReturn(false);

        mvc.perform(get("/applications/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/detail"))
                .andExpect(model().attribute("linkedPeople", List.of(linked)))
                .andExpect(model().attribute("linkablePeople", List.of(linkable)));
    }


    @Test
    void applicationDetailExposesNormalizedTaxonomyWhilePreservingLegacyDataInModel() throws Exception {
        JobApplication application = application(42L);
        application.setRoleFamily(CareerRoleFamily.BACKEND_PLATFORM);
        application.setIndustryDomain(IndustryDomain.FINTECH_PAYMENTS);
        application.setCareerFocus("Payment orchestration and distributed systems");
        application.setCareerLane("Backend / Payments / Legacy Tag");
        application.setNotes("Short working notes");

        when(applications.get(42L)).thenReturn(application);
        when(applications.eventsForApplication(42L)).thenReturn(List.of());
        when(prep.forApplication(42L)).thenReturn(List.of());
        when(prep.linkableReusableForApplication(42L)).thenReturn(List.of());
        when(attachments.forApplication(42L)).thenReturn(List.of());
        when(materials.forApplication(42L)).thenReturn(List.of());
        when(materials.linkableForApplication(42L)).thenReturn(List.of());
        when(contacts.forApplication(42L)).thenReturn(List.of());
        when(contacts.linkableForApplication(42L)).thenReturn(List.of());
        when(logos.hasLogo("northstarlabs.com")).thenReturn(false);

        mvc.perform(get("/applications/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/detail"))
                .andExpect(model().attribute("jobApplication", application));
    }

    @Test
    void applicationDetailEditEventModeUsesExistingTimelineEvent() throws Exception {
        JobApplication application = application(42L);
        ApplicationEvent event = new ApplicationEvent();
        event.setId(15L);
        event.setApplicationId(42L);
        event.setEventDate(LocalDate.of(2026, 8, 30));

        when(applications.get(42L)).thenReturn(application);
        when(applications.eventsForApplication(42L)).thenReturn(List.of(event));
        when(applications.getEvent(42L, 15L)).thenReturn(event);
        when(prep.forApplication(42L)).thenReturn(List.of());
        when(prep.linkableReusableForApplication(42L)).thenReturn(List.of());
        when(attachments.forApplication(42L)).thenReturn(List.of());
        when(materials.forApplication(42L)).thenReturn(List.of());
        when(materials.linkableForApplication(42L)).thenReturn(List.of());
        when(contacts.forApplication(42L)).thenReturn(List.of());
        when(contacts.linkableForApplication(42L)).thenReturn(List.of());
        when(logos.hasLogo("northstarlabs.com")).thenReturn(false);

        mvc.perform(get("/applications/42").param("editEvent", "15"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/detail"))
                .andExpect(model().attribute("eventForm", event))
                .andExpect(model().attribute("editingEvent", true));
    }

    @Test
    void linkPersonPostsAndRedirectsBackToPeopleSection() throws Exception {
        JobApplication application = application(42L);
        CompanyContact person = contact(7L, "Alex Morgan");
        when(applications.get(42L)).thenReturn(application);
        when(contacts.link(42L, 7L)).thenReturn(true);
        when(contacts.forApplication(42L)).thenReturn(List.of(person));

        mvc.perform(post("/applications/42/people/link").param("contactId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/42#application-people"))
                .andExpect(flash().attribute("peopleSuccess", "Alex Morgan linked to this application."));
    }

    @Test
    void unlinkPersonPostsAndRedirectsBackToPeopleSection() throws Exception {
        JobApplication application = application(42L);
        CompanyContact person = contact(7L, "Alex Morgan");
        when(applications.get(42L)).thenReturn(application);
        when(contacts.forApplication(42L)).thenReturn(List.of(person));
        when(contacts.unlink(42L, 7L)).thenReturn(true);

        mvc.perform(post("/applications/42/people/7/unlink"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/42#application-people"))
                .andExpect(flash().attribute("peopleSuccess", "Alex Morgan unlinked from this application."));
    }



    @Test
    void crossCompanyLinkReturnsFriendlyFlashInsteadOfCreatingRelationship() throws Exception {
        when(applications.get(42L)).thenReturn(application(42L));
        when(contacts.link(42L, 9L)).thenThrow(new IllegalArgumentException("Person belongs to a different company."));

        mvc.perform(post("/applications/42/people/link").param("contactId", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/42#application-people"))
                .andExpect(flash().attribute("peopleError", "Person belongs to a different company."));
    }

    @Test
    void missingPersonLinkReturns404InsteadOfFlashRedirect() throws Exception {
        when(applications.get(42L)).thenReturn(application(42L));
        when(contacts.link(42L, 404L)).thenThrow(new ResourceNotFoundException("Person not found: 404"));

        mvc.perform(post("/applications/42/people/link").param("contactId", "404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingApplicationReturns404AtHttpBoundary() throws Exception {
        when(applications.get(404L)).thenThrow(new ResourceNotFoundException("Application not found: 404"));

        mvc.perform(get("/applications/404"))
                .andExpect(status().isNotFound());
    }

    private JobApplication application(long id) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setCompany("Northstar Labs");
        application.setCompanyDomain("northstarlabs.com");
        application.setRole("Senior Backend Engineer");
        application.setStatus(ApplicationStatus.TECHNICAL_INTERVIEW);
        application.setState(ApplicationState.AWAITING_FEEDBACK);
        application.setPriority(Priority.HIGH);
        application.setAppliedDate(LocalDate.of(2026, 8, 1));
        return application;
    }

    private CompanyContact contact(long id, String name) {
        return new CompanyContact(
                id, "northstar labs", name, "Staff Engineer", CompanyContactRelationship.INTERVIEWER,
                null, null, null, false,
                LocalDateTime.of(2026, 8, 1, 12, 0), LocalDateTime.of(2026, 8, 1, 12, 0), 1);
    }
}
