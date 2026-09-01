package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.repository.ApplicationAttachmentRepository;
import com.brianna.jobsearch.repository.ApplicationContactRepository;
import com.brianna.jobsearch.repository.ApplicationEventRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import com.brianna.jobsearch.repository.MaterialRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository applications;

    @Mock
    private ApplicationEventRepository events;

    @Mock
    private ApplicationAttachmentRepository attachments;

    @Mock
    private MaterialRepository materials;

    @Mock
    private ApplicationContactRepository contacts;

    @InjectMocks
    private JobApplicationService service;

    @Test
    void createAtLaterStagePreservesAppliedStartingPointAndAddsCurrentStage() {
        JobApplication application = application(42L, ApplicationStatus.TECHNICAL_INTERVIEW);
        application.setAppliedDate(LocalDate.of(2026, 8, 1));
        when(applications.save(application)).thenReturn(42L);

        long id = service.create(application);

        assertThat(id).isEqualTo(42L);
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(events, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ApplicationEvent::getEventType)
                .containsExactly(ApplicationEventType.APPLIED, ApplicationEventType.TECHNICAL_INTERVIEW);
        assertThat(captor.getAllValues().getFirst().getEventDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(captor.getAllValues()).allMatch(event -> event.getApplicationId() == 42L);
    }

    @Test
    void changingPipelineStageCreatesLifecycleEvent() {
        JobApplication previous = application(7L, ApplicationStatus.APPLIED);
        JobApplication edited = application(7L, ApplicationStatus.RECRUITER_SCREEN);
        when(applications.findById(7L)).thenReturn(Optional.of(previous));

        service.update(edited);

        verify(applications).update(edited);
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(7L);
        assertThat(captor.getValue().getEventType()).isEqualTo(ApplicationEventType.RECRUITER_SCREEN);
        assertThat(captor.getValue().getEventDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void editingWithoutStageChangeDoesNotInventLifecycleHistory() {
        JobApplication previous = application(7L, ApplicationStatus.APPLIED);
        JobApplication edited = application(7L, ApplicationStatus.APPLIED);
        edited.setNotes("Updated notes only");
        when(applications.findById(7L)).thenReturn(Optional.of(previous));

        service.update(edited);

        verify(applications).update(edited);
        verify(events, never()).save(any());
    }

    @Test
    void terminalTimelineEventClosesApplicationAndSynchronizesHeadlineStage() {
        JobApplication application = application(9L, ApplicationStatus.TECHNICAL_INTERVIEW);
        application.setState(ApplicationState.AWAITING_FEEDBACK);
        when(applications.findById(9L)).thenReturn(Optional.of(application));

        ApplicationEvent applied = event(ApplicationEventType.APPLIED);
        ApplicationEvent rejected = event(ApplicationEventType.REJECTED);
        when(events.findByApplicationId(9L)).thenReturn(List.of(applied, rejected));

        ApplicationEvent newEvent = event(ApplicationEventType.REJECTED);
        newEvent.setEventDate(null);
        service.addEvent(9L, newEvent);

        assertThat(newEvent.getEventDate()).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getState()).isEqualTo(ApplicationState.CLOSED);
        verify(applications).update(application);
    }

    @Test
    void stillActiveReviewMarkerIsInternalAndDoesNotAppearInApplicationTimeline() {
        JobApplication current = application(46L, ApplicationStatus.APPLIED);
        when(applications.findById(46L)).thenReturn(Optional.of(current));
        ApplicationEvent applied = event(ApplicationEventType.APPLIED);
        ApplicationEvent review = event(ApplicationEventType.STILL_ACTIVE);
        when(events.findByApplicationId(46L)).thenReturn(List.of(applied, review));

        assertThat(service.eventsForApplication(46L))
                .extracting(ApplicationEvent::getEventType)
                .containsExactly(ApplicationEventType.APPLIED);
    }

    @Test
    void acknowledgeStillActiveRecordsAReviewEventInsteadOfTouchingApplicationMetadata() {
        JobApplication current = application(45L, ApplicationStatus.APPLIED);
        when(applications.findById(45L)).thenReturn(Optional.of(current));

        service.acknowledgeStillActive(45L);

        verify(applications, never()).touch(45L);
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(45L);
        assertThat(captor.getValue().getEventType()).isEqualTo(ApplicationEventType.STILL_ACTIVE);
        assertThat(captor.getValue().getEventDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void markNoResponseClosesApplicationAndCreatesLifecycleMilestone() {
        JobApplication current = application(44L, ApplicationStatus.APPLIED);
        JobApplication previous = application(44L, ApplicationStatus.APPLIED);
        when(applications.findById(44L))
                .thenReturn(Optional.of(current), Optional.of(previous));

        service.markNoResponse(44L);

        assertThat(current.getStatus()).isEqualTo(ApplicationStatus.NO_RESPONSE);
        assertThat(current.getState()).isEqualTo(ApplicationState.CLOSED);
        verify(applications).update(current);
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(44L);
        assertThat(captor.getValue().getEventType()).isEqualTo(ApplicationEventType.NO_RESPONSE);
    }

    @Test
    void deleteRemovesChildRowsBeforeApplication() {
        InOrder order = inOrder(contacts, materials, attachments, events, applications);

        service.delete(55L);

        order.verify(contacts).deleteLinksByApplicationId(55L);
        order.verify(materials).deleteLinksByApplicationId(55L);
        order.verify(attachments).deleteByApplicationId(55L);
        order.verify(events).deleteByApplicationId(55L);
        order.verify(applications).delete(55L);
    }


    @Test
    void requisitionDuplicateCheckIsSkippedWhenNoJobIdIsSaved() {
        JobApplication application = application(0L, ApplicationStatus.APPLIED);
        application.setId(null);

        assertThat(service.findPotentialDuplicate(application)).isEmpty();
        verify(applications, never()).findDuplicateByCompanyAndRequisition(any(), any(), any());
    }

    @Test
    void requisitionDuplicateCheckUsesCompanyAndExcludesCurrentApplicationOnEdit() {
        JobApplication application = application(42L, ApplicationStatus.APPLIED);
        application.setRequisitionId(" R-274666 ");
        JobApplication duplicate = application(7L, ApplicationStatus.REJECTED);
        duplicate.setRequisitionId("R-274666");
        when(applications.findDuplicateByCompanyAndRequisition("Example Co", " R-274666 ", 42L))
                .thenReturn(Optional.of(duplicate));

        assertThat(service.findPotentialDuplicate(application)).contains(duplicate);
    }

    @Test
    void createTrimsRequisitionIdBeforeSaving() {
        JobApplication application = application(0L, ApplicationStatus.APPLIED);
        application.setId(null);
        application.setRequisitionId("  REF076199W  ");
        when(applications.save(application)).thenReturn(11L);

        service.create(application);

        assertThat(application.getRequisitionId()).isEqualTo("REF076199W");
    }

    @Test
    void missingApplicationIsA404StyleResourceError() {
        when(applications.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found: 404");
    }

    private JobApplication application(long id, ApplicationStatus status) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setCompany("Example Co");
        application.setRole("Software Engineer");
        application.setStatus(status);
        application.setState(ApplicationState.ACTIVE);
        return application;
    }

    private ApplicationEvent event(ApplicationEventType type) {
        ApplicationEvent event = new ApplicationEvent();
        event.setEventType(type);
        event.setEventDate(LocalDate.of(2026, 8, 1));
        return event;
    }
}
