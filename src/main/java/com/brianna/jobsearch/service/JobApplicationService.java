package com.brianna.jobsearch.service;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationPage;
import com.brianna.jobsearch.model.ApplicationSearchCriteria;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.CalendarEntry;
import com.brianna.jobsearch.model.CalendarFilter;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.repository.ApplicationAttachmentRepository;
import com.brianna.jobsearch.repository.ApplicationContactRepository;
import com.brianna.jobsearch.repository.ApplicationEventRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import com.brianna.jobsearch.repository.MaterialRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobApplicationService {

    public static final int DEFAULT_STALE_DAYS = 45;

    private final JobApplicationRepository repository;
    private final ApplicationEventRepository eventRepository;
    private final ApplicationAttachmentRepository attachmentRepository;
    private final MaterialRepository materialRepository;
    private final ApplicationContactRepository contactRepository;

    public JobApplicationService(
            JobApplicationRepository repository,
            ApplicationEventRepository eventRepository,
            ApplicationAttachmentRepository attachmentRepository,
            MaterialRepository materialRepository,
            ApplicationContactRepository contactRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.attachmentRepository = attachmentRepository;
        this.materialRepository = materialRepository;
        this.contactRepository = contactRepository;
    }

    public List<JobApplication> search(String query) {
        return repository.findAll(query);
    }

    public List<JobApplication> findAll() {
        return repository.findAll(null);
    }

    public List<JobApplication> searchGlobal(String query, int limit) {
        return repository.searchGlobal(query, limit);
    }

    public ApplicationPage search(ApplicationSearchCriteria criteria) {
        return repository.findPage(criteria);
    }

    public List<String> workArrangements() {
        return repository.findWorkArrangements();
    }

    public List<String> sources() {
        return repository.findSources();
    }

    public List<String> careerLanes() {
        return repository.findCareerLanes();
    }

    public Optional<JobApplication> findPotentialDuplicate(JobApplication application) {
        if (application == null || application.getRequisitionId() == null || application.getRequisitionId().isBlank()) {
            return Optional.empty();
        }
        return repository.findDuplicateByCompanyAndRequisition(
                application.getCompany(), application.getRequisitionId(), application.getId());
    }

    public JobApplication get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    public List<ApplicationEvent> eventsForApplication(long applicationId) {
        get(applicationId);
        return eventRepository.findByApplicationId(applicationId).stream()
                .filter(event -> event.getEventType() != ApplicationEventType.STILL_ACTIVE)
                .toList();
    }

    public List<CalendarEntry> calendarEvents(LocalDate startDate, LocalDate endDate) {
        return calendarEvents(startDate, endDate, CalendarFilter.ALL);
    }

    public List<CalendarEntry> calendarEvents(LocalDate startDate, LocalDate endDate, CalendarFilter filter) {
        CalendarFilter safeFilter = filter == null ? CalendarFilter.ACTIONABLE : filter;
        return eventRepository.findBetween(startDate, endDate).stream()
                .filter(entry -> safeFilter.includes(entry.getEventType()))
                .toList();
    }

    public ApplicationEvent getEvent(long applicationId, long eventId) {
        get(applicationId);
        return eventRepository.findById(eventId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Timeline event not found: " + eventId));
    }

    @Transactional
    public long create(JobApplication application) {
        normalizeApplication(application);
        long id = repository.save(application);

        ApplicationEvent firstEvent = new ApplicationEvent();
        firstEvent.setApplicationId(id);

        if (application.getStatus() == ApplicationStatus.SAVED) {
            firstEvent.setEventType(ApplicationEventType.SAVED);
            firstEvent.setEventDate(LocalDate.now());
        } else {
            firstEvent.setEventType(ApplicationEventType.APPLIED);
            firstEvent.setEventDate(application.getAppliedDate() == null ? LocalDate.now() : application.getAppliedDate());
        }
        eventRepository.save(firstEvent);

        if (application.getStatus() != ApplicationStatus.SAVED
                && application.getStatus() != ApplicationStatus.APPLIED) {
            ApplicationEvent currentStage = new ApplicationEvent();
            currentStage.setApplicationId(id);
            currentStage.setEventType(ApplicationEventType.fromStatus(application.getStatus()));
            currentStage.setEventDate(LocalDate.now());
            eventRepository.save(currentStage);
        }

        return id;
    }

    @Transactional
    public void update(JobApplication application) {
        JobApplication previous = get(application.getId());
        normalizeApplication(application);
        repository.update(application);

        if (previous.getStatus() != application.getStatus()) {
            ApplicationEvent stageChange = new ApplicationEvent();
            stageChange.setApplicationId(application.getId());
            stageChange.setEventType(ApplicationEventType.fromStatus(application.getStatus()));
            stageChange.setEventDate(LocalDate.now());
            eventRepository.save(stageChange);
        }
    }

    @Transactional
    public void addEvent(long applicationId, ApplicationEvent event) {
        get(applicationId);
        event.setApplicationId(applicationId);
        normalizeEvent(event);
        eventRepository.save(event);
        syncApplicationFromTimeline(applicationId);
    }

    @Transactional
    public void updateEvent(long applicationId, long eventId, ApplicationEvent event) {
        getEvent(applicationId, eventId);
        event.setId(eventId);
        event.setApplicationId(applicationId);
        normalizeEvent(event);
        eventRepository.update(event);
        syncApplicationFromTimeline(applicationId);
    }

    @Transactional
    public void deleteEvent(long applicationId, long eventId) {
        get(applicationId);
        eventRepository.delete(eventId, applicationId);
        syncApplicationFromTimeline(applicationId);
    }

    @Transactional
    public void delete(long id) {
        contactRepository.deleteLinksByApplicationId(id);
        materialRepository.deleteLinksByApplicationId(id);
        attachmentRepository.deleteByApplicationId(id);
        eventRepository.deleteByApplicationId(id);
        repository.delete(id);
    }

    public List<JobApplication> staleApplications(int staleDays) {
        return repository.findStale(normalizeStaleDays(staleDays));
    }

    public long staleApplicationCount(int staleDays) {
        return repository.countStale(normalizeStaleDays(staleDays));
    }

    @Transactional
    public void markCoverLetterUsedForAttachment(long id) {
        get(id);
        repository.markCoverLetterUsed(id);
    }

    @Transactional
    public void acknowledgeStillActive(long id) {
        get(id);
        ApplicationEvent acknowledgement = new ApplicationEvent();
        acknowledgement.setApplicationId(id);
        acknowledgement.setEventType(ApplicationEventType.STILL_ACTIVE);
        acknowledgement.setEventDate(LocalDate.now());
        acknowledgement.setTitle("Still active");
        acknowledgement.setNotes("Kept active from stale application review.");
        eventRepository.save(acknowledgement);
    }

    @Transactional
    public void markNoResponse(long id) {
        JobApplication application = get(id);
        application.setStatus(ApplicationStatus.NO_RESPONSE);
        application.setState(ApplicationState.CLOSED);
        update(application);
    }

    @Transactional
    public void markFollowUpDue(long id) {
        JobApplication application = get(id);
        application.setState(ApplicationState.FOLLOW_UP_DUE);
        repository.update(application);

        ApplicationEvent followUp = new ApplicationEvent();
        followUp.setApplicationId(id);
        followUp.setEventType(ApplicationEventType.FOLLOW_UP);
        followUp.setEventDate(LocalDate.now());
        followUp.setTitle("Follow-up due");
        followUp.setNotes("Marked for follow-up from stale application review.");
        eventRepository.save(followUp);
    }

    @Transactional
    public int applyStaleBulkAction(List<Long> ids, String action) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            switch (action == null ? "" : action) {
                case "NO_RESPONSE" -> markNoResponse(id);
                case "FOLLOW_UP" -> markFollowUpDue(id);
                case "KEEP_ACTIVE" -> acknowledgeStillActive(id);
                default -> throw new IllegalArgumentException("Unknown stale application action: " + action);
            }
            changed++;
        }
        return changed;
    }

    public DashboardSnapshot dashboardSnapshot() {
        long total = repository.countAll();
        long responded = repository.countResponded();
        double responseRate = total == 0 ? 0.0 : (responded * 100.0) / total;
        long stale = repository.countStale(DEFAULT_STALE_DAYS);

        return new DashboardSnapshot(
                total,
                repository.countActive(),
                repository.countInterviewing(),
                repository.countOffers(),
                responseRate,
                repository.findRecent(5),
                stale,
                DEFAULT_STALE_DAYS);
    }


    /**
     * Keeps the application's headline pipeline stage in sync with its timeline.
     *
     * Timeline history is the source of truth for the furthest/latest stage event,
     * while current state remains independently user-controlled except for
     * deterministic terminal outcomes (Rejected / Withdrawn / No Response).
     */
    private void syncApplicationFromTimeline(long applicationId) {
        JobApplication application = get(applicationId);
        ApplicationStatus latestStatus = null;

        for (ApplicationEvent timelineEvent : eventRepository.findByApplicationId(applicationId)) {
            ApplicationStatus mappedStatus = statusForTimelineEvent(timelineEvent.getEventType());
            if (mappedStatus != null) {
                latestStatus = mappedStatus;
            }
        }

        if (latestStatus == null) {
            repository.touch(applicationId);
            return;
        }

        application.setStatus(latestStatus);

        if (latestStatus == ApplicationStatus.REJECTED
                || latestStatus == ApplicationStatus.WITHDRAWN
                || latestStatus == ApplicationStatus.NO_RESPONSE) {
            application.setState(ApplicationState.CLOSED);
        } else if (application.getState() == ApplicationState.CLOSED) {
            // If a terminal milestone was removed/edited, reopen conservatively.
            application.setState(ApplicationState.ACTIVE);
        }

        repository.update(application);
    }

    private ApplicationStatus statusForTimelineEvent(ApplicationEventType eventType) {
        if (eventType == null) {
            return null;
        }

        return switch (eventType) {
            case SAVED -> ApplicationStatus.SAVED;
            case APPLIED -> ApplicationStatus.APPLIED;
            case CODING_ASSESSMENT, TAKE_HOME_ASSESSMENT -> ApplicationStatus.ASSESSMENT;
            case RECRUITER_SCREEN -> ApplicationStatus.RECRUITER_SCREEN;
            case HIRING_MANAGER -> ApplicationStatus.HIRING_MANAGER;
            case TECHNICAL_INTERVIEW -> ApplicationStatus.TECHNICAL_INTERVIEW;
            case FINAL_ROUND -> ApplicationStatus.FINAL_ROUND;
            case OFFER -> ApplicationStatus.OFFER;
            case REJECTED -> ApplicationStatus.REJECTED;
            case WITHDRAWN -> ApplicationStatus.WITHDRAWN;
            case NO_RESPONSE -> ApplicationStatus.NO_RESPONSE;
            case RECRUITER_CONTACT, INTERVIEW_SCHEDULED, FOLLOW_UP, STILL_ACTIVE, OTHER -> null;
        };
    }

    private void normalizeEvent(ApplicationEvent event) {
        if (event.getEventType() == null) {
            event.setEventType(ApplicationEventType.OTHER);
        }
        if (event.getEventDate() == null) {
            event.setEventDate(LocalDate.now());
        }
    }

    private void normalizeApplication(JobApplication application) {
        normalizeState(application);
        application.setCompanyDomain(CompanyLogoService.normalizeDomain(application.getCompanyDomain()));
        application.setRequisitionId(blankToNull(application.getRequisitionId()));
        if (application.hasCoverLetterText()) {
            application.setCoverLetter(true);
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void normalizeState(JobApplication application) {
        if (application.getState() == null) {
            application.setState(ApplicationState.ACTIVE);
        }

        if (application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.WITHDRAWN
                || application.getStatus() == ApplicationStatus.NO_RESPONSE) {
            application.setState(ApplicationState.CLOSED);
        }
    }

    private int normalizeStaleDays(int days) {
        return days == 30 || days == 45 || days == 60 || days == 90
                ? days
                : DEFAULT_STALE_DAYS;
    }

    public record DashboardSnapshot(
            long total,
            long active,
            long interviewing,
            long offers,
            double responseRate,
            List<JobApplication> recent,
            long staleCount,
            int staleDays) {
    }
}
