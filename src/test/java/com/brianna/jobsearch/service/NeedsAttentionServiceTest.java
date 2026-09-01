package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.AttentionUrgency;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.NeedsAttentionItem;
import com.brianna.jobsearch.repository.ApplicationContactRepository;
import com.brianna.jobsearch.repository.ApplicationEventRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import com.brianna.jobsearch.repository.PrepItemRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NeedsAttentionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 31);

    @Mock private JobApplicationRepository applications;
    @Mock private ApplicationEventRepository events;
    @Mock private PrepItemRepository prepItems;
    @Mock private ApplicationContactRepository contacts;

    private NeedsAttentionService service;

    @BeforeEach
    void setUp() {
        service = new NeedsAttentionService(applications, events, prepItems, contacts);
    }

    @Test
    void explicitFollowUpDueWinsWithoutNeedingSecondaryContext() {
        JobApplication app = application(1L, ApplicationStatus.APPLIED, ApplicationState.FOLLOW_UP_DUE);

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.NOW);
        assertThat(item.headline()).isEqualTo("Follow-up is due");
        assertThat(item.actionUrl()).isEqualTo("/applications/1#activity-composer");
        verify(events, never()).findByApplicationId(1L);
        verify(prepItems, never()).countForApplication(1L);
        verify(contacts, never()).countByApplicationId(1L);
    }

    @Test
    void imminentInterviewWithoutPrepBeatsOtherMissingContext() {
        JobApplication app = application(2L, ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationState.INTERVIEW_SCHEDULED);
        when(events.findByApplicationId(2L)).thenReturn(List.of(
                event(20L, ApplicationEventType.INTERVIEW_SCHEDULED, TODAY.plusDays(1))));
        when(prepItems.countForApplication(2L)).thenReturn(0L);

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.NOW);
        assertThat(item.headline()).isEqualTo("Interview tomorrow · no prep linked");
        assertThat(item.actionLabel()).isEqualTo("Add prep");
        verify(contacts, never()).countByApplicationId(2L);
    }

    @Test
    void passedScheduledInterviewPromptsStateUpdate() {
        JobApplication app = application(3L, ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationState.INTERVIEW_SCHEDULED);
        when(events.findByApplicationId(3L)).thenReturn(List.of(
                event(30L, ApplicationEventType.INTERVIEW_SCHEDULED, TODAY.minusDays(1))));

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.NOW);
        assertThat(item.headline()).isEqualTo("Interview date passed · update status");
        assertThat(item.actionUrl()).isEqualTo("/applications/3/edit");
    }

    @Test
    void recentCompletedInterviewWithoutNewerActivityPromptsFollowUp() {
        JobApplication app = application(4L, ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationState.AWAITING_FEEDBACK);
        when(events.findByApplicationId(4L)).thenReturn(List.of(
                event(40L, ApplicationEventType.TECHNICAL_INTERVIEW, TODAY.minusDays(2))));

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.NOW);
        assertThat(item.headline()).isEqualTo("Interview completed · follow up");
        assertThat(item.actionUrl()).endsWith("#activity-composer");
    }

    @Test
    void newerCommunicationSuppressesRecentInterviewFollowUpCue() {
        JobApplication app = application(5L, ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationState.AWAITING_FEEDBACK);
        when(events.findByApplicationId(5L)).thenReturn(List.of(
                event(50L, ApplicationEventType.TECHNICAL_INTERVIEW, TODAY.minusDays(2)),
                event(51L, ApplicationEventType.RECRUITER_CONTACT, TODAY.minusDays(1))));
        when(prepItems.countForApplication(5L)).thenReturn(1L);
        when(contacts.countByApplicationId(5L)).thenReturn(1L);

        assertThat(service.evaluate(app, TODAY)).isEmpty();
    }

    @Test
    void quietInterviewProcessOutranksGenericMissingPrep() {
        JobApplication app = application(6L, ApplicationStatus.FINAL_ROUND, ApplicationState.AWAITING_FEEDBACK);
        when(events.findByApplicationId(6L)).thenReturn(List.of(
                event(60L, ApplicationEventType.RECRUITER_CONTACT, TODAY.minusDays(9))));

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.SOON);
        assertThat(item.headline()).isEqualTo("Interview process quiet for 9 days");
        verify(prepItems, never()).countForApplication(6L);
    }

    @Test
    void activeInterviewProcessWithoutPrepGetsPrepCueBeforeContactCue() {
        JobApplication app = application(7L, ApplicationStatus.RECRUITER_SCREEN, ApplicationState.ACTIVE);
        when(events.findByApplicationId(7L)).thenReturn(List.of(
                event(70L, ApplicationEventType.RECRUITER_CONTACT, TODAY)));
        when(prepItems.countForApplication(7L)).thenReturn(0L);

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.headline()).isEqualTo("Interview process active · prep is empty");
        assertThat(item.actionUrl()).isEqualTo("/prep/new?applicationId=7");
        verify(contacts, never()).countByApplicationId(7L);
    }

    @Test
    void activeInterviewProcessWithPrepButNoPeopleGetsContactCue() {
        JobApplication app = application(8L, ApplicationStatus.RECRUITER_SCREEN, ApplicationState.ACTIVE);
        when(events.findByApplicationId(8L)).thenReturn(List.of(
                event(80L, ApplicationEventType.RECRUITER_CONTACT, TODAY)));
        when(prepItems.countForApplication(8L)).thenReturn(1L);
        when(contacts.countByApplicationId(8L)).thenReturn(0L);

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.SOON);
        assertThat(item.headline()).isEqualTo("Interviewing · no contacts linked");
        assertThat(item.actionUrl()).isEqualTo("/applications/8#application-people");
    }


    @Test
    void appliedWithoutResponseBecomesKeepWarmAfterFourteenDays() {
        JobApplication app = application(11L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        app.setAppliedDate(TODAY.minusDays(14));
        when(events.findByApplicationId(11L)).thenReturn(List.of(
                event(110L, ApplicationEventType.APPLIED, TODAY.minusDays(14))));

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.KEEP_WARM);
        assertThat(item.headline()).isEqualTo("Applied 14 days ago · no response yet");
        assertThat(item.detail()).contains("since applying");
        assertThat(item.actionUrl()).isEqualTo("/applications/11#activity-composer");
        verify(prepItems, never()).countForApplication(11L);
        verify(contacts, never()).countByApplicationId(11L);
    }

    @Test
    void appliedWithoutResponseStaysQuietBeforeFourteenDays() {
        JobApplication app = application(12L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        app.setAppliedDate(TODAY.minusDays(13));
        when(events.findByApplicationId(12L)).thenReturn(List.of(
                event(120L, ApplicationEventType.APPLIED, TODAY.minusDays(13))));

        assertThat(service.evaluate(app, TODAY)).isEmpty();
    }

    @Test
    void unansweredApplicationGetsStrongerLikelyNoResponseCopyAtThirtyDays() {
        JobApplication app = application(17L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        app.setAppliedDate(TODAY.minusDays(30));
        when(events.findByApplicationId(17L)).thenReturn(List.of(
                event(170L, ApplicationEventType.APPLIED, TODAY.minusDays(30))));

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.KEEP_WARM);
        assertThat(item.headline()).isEqualTo("Likely no response · applied 30 days ago");
    }

    @Test
    void oldUntouchedUnansweredApplicationRollsIntoStaleReviewInsteadOfAttentionCard() {
        JobApplication app = application(18L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        app.setAppliedDate(TODAY.minusDays(45));
        app.setUpdatedAt(TODAY.atStartOfDay());
        when(events.findByApplicationId(18L)).thenReturn(List.of(
                event(180L, ApplicationEventType.APPLIED, TODAY.minusDays(45))));

        assertThat(service.evaluate(app, TODAY)).isEmpty();
    }

    @Test
    void explicitStillActiveReviewResetsNoResponseAgingWithoutPretendingThereWasEmployerActivity() {
        JobApplication app = application(19L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        app.setAppliedDate(TODAY.minusDays(70));
        when(events.findByApplicationId(19L)).thenReturn(List.of(
                event(190L, ApplicationEventType.APPLIED, TODAY.minusDays(70)),
                event(191L, ApplicationEventType.STILL_ACTIVE, TODAY.minusDays(10))));

        assertThat(service.evaluate(app, TODAY)).isEmpty();
    }

    @Test
    void oldStillActiveReviewEventuallyReturnsToStaleReview() {
        JobApplication app = application(20L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        app.setAppliedDate(TODAY.minusDays(100));
        when(events.findByApplicationId(20L)).thenReturn(List.of(
                event(200L, ApplicationEventType.APPLIED, TODAY.minusDays(100)),
                event(201L, ApplicationEventType.STILL_ACTIVE, TODAY.minusDays(45))));

        assertThat(service.evaluate(app, TODAY)).isEmpty();
    }

    @Test
    void activeApplicationWithOldLifecycleActivityGetsQuietKeepWarmCue() {
        JobApplication app = application(13L, ApplicationStatus.APPLIED, ApplicationState.AWAITING_FEEDBACK);
        app.setAppliedDate(TODAY.minusDays(45));
        when(events.findByApplicationId(13L)).thenReturn(List.of(
                event(130L, ApplicationEventType.APPLIED, TODAY.minusDays(45)),
                event(131L, ApplicationEventType.RECRUITER_CONTACT, TODAY.minusDays(23))));

        NeedsAttentionItem item = service.evaluate(app, TODAY).orElseThrow();

        assertThat(item.urgency()).isEqualTo(AttentionUrgency.KEEP_WARM);
        assertThat(item.headline()).isEqualTo("No activity for 23 days");
        assertThat(item.detail()).contains("gone quiet");
    }

    @Test
    void recentLifecycleActivitySuppressesGenericKeepWarmCue() {
        JobApplication app = application(14L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        app.setAppliedDate(TODAY.minusDays(45));
        when(events.findByApplicationId(14L)).thenReturn(List.of(
                event(140L, ApplicationEventType.RECRUITER_CONTACT, TODAY.minusDays(20))));

        assertThat(service.evaluate(app, TODAY)).isEmpty();
    }

    @Test
    void snapshotSortsNowBeforeSoonAndCapsVisibleItems() {
        JobApplication soon = application(9L, ApplicationStatus.RECRUITER_SCREEN, ApplicationState.ACTIVE);
        JobApplication now = application(10L, ApplicationStatus.APPLIED, ApplicationState.FOLLOW_UP_DUE);
        when(applications.findAttentionCandidates()).thenReturn(List.of(soon, now));
        when(events.findByApplicationId(9L)).thenReturn(List.of(
                event(90L, ApplicationEventType.RECRUITER_CONTACT, TODAY)));
        when(prepItems.countForApplication(9L)).thenReturn(0L);

        NeedsAttentionService.AttentionSnapshot snapshot = service.snapshot(1, TODAY);

        assertThat(snapshot.totalCount()).isEqualTo(2);
        assertThat(snapshot.items()).singleElement()
                .extracting(NeedsAttentionItem::applicationId)
                .isEqualTo(10L);
        assertThat(snapshot.hasMore()).isTrue();
    }


    @Test
    void snapshotShowsOlderKeepWarmItemsFirstWithinSameUrgency() {
        JobApplication newer = application(15L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        newer.setAppliedDate(TODAY.minusDays(14));
        JobApplication older = application(16L, ApplicationStatus.APPLIED, ApplicationState.ACTIVE);
        older.setAppliedDate(TODAY.minusDays(31));
        when(applications.findAttentionCandidates()).thenReturn(List.of(newer, older));
        when(events.findByApplicationId(15L)).thenReturn(List.of());
        when(events.findByApplicationId(16L)).thenReturn(List.of());

        NeedsAttentionService.AttentionSnapshot snapshot = service.snapshot(1, TODAY);

        assertThat(snapshot.items()).singleElement()
                .extracting(NeedsAttentionItem::applicationId)
                .isEqualTo(16L);
        assertThat(snapshot.totalCount()).isEqualTo(2);
    }

    private JobApplication application(long id, ApplicationStatus status, ApplicationState state) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setCompany("Example " + id);
        app.setRole("Software Engineer");
        app.setStatus(status);
        app.setState(state);
        app.setAppliedDate(TODAY.minusDays(20));
        return app;
    }

    private ApplicationEvent event(long id, ApplicationEventType type, LocalDate date) {
        ApplicationEvent event = new ApplicationEvent();
        event.setId(id);
        event.setEventType(type);
        event.setEventDate(date);
        return event;
    }
}
