package com.brianna.jobsearch.service;

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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NeedsAttentionService {

    public static final int DEFAULT_LIMIT = 5;
    private static final int IMMINENT_INTERVIEW_DAYS = 2;
    private static final int FOLLOW_UP_WINDOW_DAYS = 3;
    private static final int QUIET_INTERVIEW_DAYS = 7;
    private static final int APPLIED_NO_RESPONSE_DAYS = 14;
    private static final int LIKELY_NO_RESPONSE_DAYS = 30;
    private static final int STALE_REVIEW_DAYS = 45;
    private static final int ACTIVE_QUIET_DAYS = 21;

    private final JobApplicationRepository applications;
    private final ApplicationEventRepository events;
    private final PrepItemRepository prepItems;
    private final ApplicationContactRepository contacts;

    public NeedsAttentionService(
            JobApplicationRepository applications,
            ApplicationEventRepository events,
            PrepItemRepository prepItems,
            ApplicationContactRepository contacts) {
        this.applications = applications;
        this.events = events;
        this.prepItems = prepItems;
        this.contacts = contacts;
    }

    public AttentionSnapshot snapshot() {
        return snapshot(DEFAULT_LIMIT, LocalDate.now());
    }

    AttentionSnapshot snapshot(int limit, LocalDate today) {
        int safeLimit = Math.max(1, limit);
        List<NeedsAttentionItem> all = applications.findAttentionCandidates().stream()
                .map(application -> evaluate(application, today))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparingInt((NeedsAttentionItem item) -> urgencyRank(item.urgency()))
                        .thenComparingLong(this::sortProximity)
                        .thenComparingInt(NeedsAttentionItem::ruleOrder)
                        .thenComparing(NeedsAttentionItem::company, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new AttentionSnapshot(all.stream().limit(safeLimit).toList(), all.size());
    }

    Optional<NeedsAttentionItem> evaluate(JobApplication application, LocalDate today) {
        if (application == null || application.getId() == null || !isEligible(application)) {
            return Optional.empty();
        }

        long id = application.getId();
        if (application.getState() == ApplicationState.FOLLOW_UP_DUE) {
            return Optional.of(item(application, AttentionUrgency.NOW,
                    "Follow-up is due",
                    "This application is marked for follow-up and needs a next touch.",
                    "Add activity", activityUrl(id), 0, 0));
        }

        List<ApplicationEvent> timeline = events.findByApplicationId(id);
        Optional<ApplicationEvent> nextInterview = timeline.stream()
                .filter(event -> isInterviewRelated(event) && event.getEventDate() != null && !event.getEventDate().isBefore(today))
                .min(eventComparator());

        if (application.getState() == ApplicationState.INTERVIEW_SCHEDULED && nextInterview.isEmpty()) {
            Optional<ApplicationEvent> latestPastInterview = timeline.stream()
                    .filter(event -> isInterviewRelated(event) && event.getEventDate() != null && event.getEventDate().isBefore(today))
                    .max(eventComparator());
            if (latestPastInterview.isPresent() && !hasMeaningfulEventAfter(timeline, latestPastInterview.get())) {
                long days = Math.max(0, ChronoUnit.DAYS.between(latestPastInterview.get().getEventDate(), today));
                return Optional.of(item(application, AttentionUrgency.NOW,
                        "Interview date passed · update status",
                        "The scheduled interview has passed, but the application is still marked Interview Scheduled.",
                        "Update application", editUrl(id), days, 1));
            }
        }

        Long prepCount = null;
        if (nextInterview.isPresent()) {
            long daysUntil = ChronoUnit.DAYS.between(today, nextInterview.get().getEventDate());
            if (daysUntil <= IMMINENT_INTERVIEW_DAYS) {
                prepCount = prepItems.countForApplication(id);
                if (prepCount == 0) {
                    return Optional.of(item(application, AttentionUrgency.NOW,
                            imminentInterviewHeadline(daysUntil),
                            "There is an upcoming interview but no prep is linked to this role yet.",
                            "Add prep", prepUrl(id), daysUntil, 2));
                }
            }
        }

        Optional<ApplicationEvent> latestCompletedInterview = timeline.stream()
                .filter(event -> event.getEventType() != null
                        && event.getEventType().isInterview()
                        && event.getEventDate() != null
                        && event.getEventDate().isBefore(today))
                .max(eventComparator());
        if (latestCompletedInterview.isPresent()) {
            long daysSince = ChronoUnit.DAYS.between(latestCompletedInterview.get().getEventDate(), today);
            if (daysSince >= 1 && daysSince <= FOLLOW_UP_WINDOW_DAYS
                    && !hasMeaningfulEventAfter(timeline, latestCompletedInterview.get())) {
                return Optional.of(item(application, AttentionUrgency.NOW,
                        "Interview completed · follow up",
                        "No newer communication or lifecycle activity has been recorded since the interview.",
                        "Add activity", activityUrl(id), daysSince, 3));
            }
        }

        boolean interviewProcess = isInterviewStage(application.getStatus())
                || application.getState() == ApplicationState.INTERVIEW_SCHEDULED
                || nextInterview.isPresent();
        if (!interviewProcess) {
            return evaluateKeepWarm(application, timeline, today);
        }

        boolean hasFutureInterview = nextInterview.isPresent();
        LocalDate lastMeaningfulDate = timeline.stream()
                .filter(this::isMeaningful)
                .map(ApplicationEvent::getEventDate)
                .filter(date -> date != null && !date.isAfter(today))
                .max(LocalDate::compareTo)
                .orElse(application.getAppliedDate());
        if (!hasFutureInterview && lastMeaningfulDate != null) {
            long quietDays = ChronoUnit.DAYS.between(lastMeaningfulDate, today);
            if (quietDays >= QUIET_INTERVIEW_DAYS
                    && application.getState() != ApplicationState.ON_HOLD
                    && application.getState() != ApplicationState.INTERVIEW_SCHEDULED) {
                return Optional.of(item(application, AttentionUrgency.SOON,
                        "Interview process quiet for " + quietDays + " days",
                        "There is no upcoming interview or newer lifecycle activity on the timeline.",
                        "Add activity", activityUrl(id), quietDays, 4));
            }
        }

        if (prepCount == null) {
            prepCount = prepItems.countForApplication(id);
        }
        if (prepCount == 0) {
            return Optional.of(item(application, AttentionUrgency.SOON,
                    "Interview process active · prep is empty",
                    "Add role-specific or reusable prep before the next conversation.",
                    "Add prep", prepUrl(id), 999, 5));
        }

        if (contacts.countByApplicationId(id) == 0) {
            return Optional.of(item(application, AttentionUrgency.SOON,
                    "Interviewing · no contacts linked",
                    "Save the recruiter, interviewer, or hiring manager so the relationship stays with the role.",
                    "Add contacts", peopleUrl(id), 999, 6));
        }

        return Optional.empty();
    }


    private Optional<NeedsAttentionItem> evaluateKeepWarm(
            JobApplication application, List<ApplicationEvent> timeline, LocalDate today) {
        if (application.getStatus() != ApplicationStatus.APPLIED
                || (application.getState() != ApplicationState.ACTIVE
                    && application.getState() != ApplicationState.AWAITING_FEEDBACK)) {
            return Optional.empty();
        }

        long id = application.getId();
        Optional<LocalDate> latestMeaningfulDate = timeline.stream()
                .filter(this::isMeaningful)
                .map(ApplicationEvent::getEventDate)
                .filter(date -> date != null && !date.isAfter(today))
                .max(LocalDate::compareTo);
        Optional<LocalDate> latestStillActiveReview = timeline.stream()
                .filter(event -> event != null && event.getEventType() == ApplicationEventType.STILL_ACTIVE)
                .map(ApplicationEvent::getEventDate)
                .filter(date -> date != null && !date.isAfter(today))
                .max(LocalDate::compareTo);

        LocalDate appliedDate = application.getAppliedDate();
        if (latestMeaningfulDate.isEmpty()
                && application.getState() == ApplicationState.ACTIVE
                && appliedDate != null) {
            long daysSinceApplied = ChronoUnit.DAYS.between(appliedDate, today);
            LocalDate reviewClockStart = latestStillActiveReview
                    .filter(reviewed -> reviewed.isAfter(appliedDate))
                    .orElse(appliedDate);
            long daysSinceReviewClock = Math.max(0, ChronoUnit.DAYS.between(reviewClockStart, today));
            if (daysSinceApplied >= STALE_REVIEW_DAYS && daysSinceReviewClock >= STALE_REVIEW_DAYS) {
                // The dashboard rolls these into the dedicated stale-review queue instead of
                // spending one of the five Needs Attention slots on likely ghosted applications.
                return Optional.empty();
            }
            if (daysSinceReviewClock >= LIKELY_NO_RESPONSE_DAYS) {
                String headline = latestStillActiveReview.isPresent()
                        ? "Still no response · reviewed " + daysSinceReviewClock + " days ago"
                        : "Likely no response · applied " + daysSinceApplied + " days ago";
                return Optional.of(item(application, AttentionUrgency.KEEP_WARM,
                        headline,
                        "No recruiter response or other lifecycle activity has been recorded since applying.",
                        "Add activity", activityUrl(id), daysSinceReviewClock, 7));
            }
            if (daysSinceReviewClock >= APPLIED_NO_RESPONSE_DAYS) {
                String headline = latestStillActiveReview.isPresent()
                        ? "Still no response · reviewed " + daysSinceReviewClock + " days ago"
                        : "Applied " + daysSinceApplied + " days ago · no response yet";
                return Optional.of(item(application, AttentionUrgency.KEEP_WARM,
                        headline,
                        "No recruiter response or other lifecycle activity has been recorded since applying.",
                        "Add activity", activityUrl(id), daysSinceReviewClock, 7));
            }
        }

        if (latestMeaningfulDate.isPresent()) {
            long quietDays = ChronoUnit.DAYS.between(latestMeaningfulDate.get(), today);
            if (quietDays >= ACTIVE_QUIET_DAYS) {
                return Optional.of(item(application, AttentionUrgency.KEEP_WARM,
                        "No activity for " + quietDays + " days",
                        "This active application has gone quiet since the last recorded lifecycle activity.",
                        "Add activity", activityUrl(id), quietDays, 8));
            }
        }

        return Optional.empty();
    }

    private boolean isEligible(JobApplication application) {
        if (application.getState() == ApplicationState.CLOSED || application.getState() == ApplicationState.ON_HOLD) {
            return false;
        }
        ApplicationStatus status = application.getStatus();
        return status != ApplicationStatus.OFFER
                && status != ApplicationStatus.REJECTED
                && status != ApplicationStatus.WITHDRAWN
                && status != ApplicationStatus.NO_RESPONSE;
    }

    private boolean isInterviewStage(ApplicationStatus status) {
        return status == ApplicationStatus.RECRUITER_SCREEN
                || status == ApplicationStatus.ASSESSMENT
                || status == ApplicationStatus.HIRING_MANAGER
                || status == ApplicationStatus.TECHNICAL_INTERVIEW
                || status == ApplicationStatus.FINAL_ROUND;
    }

    private boolean isInterviewRelated(ApplicationEvent event) {
        if (event == null || event.getEventType() == null) {
            return false;
        }
        return event.getEventType() == ApplicationEventType.INTERVIEW_SCHEDULED || event.getEventType().isInterview();
    }

    private boolean isMeaningful(ApplicationEvent event) {
        if (event == null || event.getEventType() == null) {
            return false;
        }
        return event.getEventType() != ApplicationEventType.SAVED
                && event.getEventType() != ApplicationEventType.APPLIED
                && event.getEventType() != ApplicationEventType.STILL_ACTIVE;
    }

    private boolean hasMeaningfulEventAfter(List<ApplicationEvent> timeline, ApplicationEvent target) {
        return timeline.stream()
                .filter(this::isMeaningful)
                .anyMatch(event -> event != target && eventComparator().compare(event, target) > 0);
    }

    private Comparator<ApplicationEvent> eventComparator() {
        return Comparator
                .comparing(ApplicationEvent::getEventDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ApplicationEvent::getEventTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ApplicationEvent::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    private String imminentInterviewHeadline(long daysUntil) {
        if (daysUntil <= 0) {
            return "Interview today · no prep linked";
        }
        if (daysUntil == 1) {
            return "Interview tomorrow · no prep linked";
        }
        return "Interview in " + daysUntil + " days · no prep linked";
    }

    private NeedsAttentionItem item(
            JobApplication application,
            AttentionUrgency urgency,
            String headline,
            String detail,
            String actionLabel,
            String actionUrl,
            long proximityDays,
            int ruleOrder) {
        long id = application.getId();
        return new NeedsAttentionItem(
                id,
                application.getCompany(),
                application.getRole(),
                urgency,
                headline,
                detail,
                actionLabel,
                actionUrl,
                "/applications/" + id,
                Math.max(0, proximityDays),
                ruleOrder);
    }

    private String activityUrl(long id) {
        return "/applications/" + id + "#activity-composer";
    }

    private String prepUrl(long id) {
        return "/prep/new?applicationId=" + id;
    }

    private String peopleUrl(long id) {
        return "/applications/" + id + "#application-people";
    }

    private String editUrl(long id) {
        return "/applications/" + id + "/edit";
    }

    private long sortProximity(NeedsAttentionItem item) {
        return switch (item.ruleOrder()) {
            case 4, 7, 8 -> -item.proximityDays();
            default -> item.proximityDays();
        };
    }

    private int urgencyRank(AttentionUrgency urgency) {
        return switch (urgency) {
            case NOW -> 0;
            case SOON -> 1;
            case KEEP_WARM -> 2;
        };
    }

    public record AttentionSnapshot(List<NeedsAttentionItem> items, int totalCount) {
        public boolean hasMore() {
            return totalCount > items.size();
        }
    }
}
