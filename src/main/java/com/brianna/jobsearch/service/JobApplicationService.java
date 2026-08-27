package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.CalendarEntry;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.repository.ApplicationEventRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final ApplicationEventRepository eventRepository;

    public JobApplicationService(
            JobApplicationRepository repository,
            ApplicationEventRepository eventRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
    }

    public List<JobApplication> search(String query) {
        return repository.findAll(query);
    }

    public JobApplication get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }

    public List<ApplicationEvent> eventsForApplication(long applicationId) {
        get(applicationId);
        return eventRepository.findByApplicationId(applicationId);
    }

    public List<CalendarEntry> calendarEvents(LocalDate startDate, LocalDate endDate) {
        return eventRepository.findBetween(startDate, endDate);
    }

    public ApplicationEvent getEvent(long applicationId, long eventId) {
        get(applicationId);
        return eventRepository.findById(eventId, applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Timeline event not found: " + eventId));
    }

    @Transactional
    public long create(JobApplication application) {
        normalizeState(application);
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
        normalizeState(application);
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
        repository.touch(applicationId);
    }

    @Transactional
    public void updateEvent(long applicationId, long eventId, ApplicationEvent event) {
        getEvent(applicationId, eventId);
        event.setId(eventId);
        event.setApplicationId(applicationId);
        normalizeEvent(event);
        eventRepository.update(event);
        repository.touch(applicationId);
    }

    @Transactional
    public void deleteEvent(long applicationId, long eventId) {
        get(applicationId);
        eventRepository.delete(eventId, applicationId);
        repository.touch(applicationId);
    }

    @Transactional
    public void delete(long id) {
        eventRepository.deleteByApplicationId(id);
        repository.delete(id);
    }

    public DashboardSnapshot dashboardSnapshot() {
        long total = repository.countAll();
        long responded = repository.countResponded();
        double responseRate = total == 0 ? 0.0 : (responded * 100.0) / total;

        return new DashboardSnapshot(
                total,
                repository.countActive(),
                repository.countInterviewing(),
                repository.countOffers(),
                responseRate,
                repository.findRecent(5),
                repository.findNeedsAttention(5));
    }

    private void normalizeEvent(ApplicationEvent event) {
        if (event.getEventType() == null) {
            event.setEventType(ApplicationEventType.OTHER);
        }
        if (event.getEventDate() == null) {
            event.setEventDate(LocalDate.now());
        }
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

    public record DashboardSnapshot(
            long total,
            long active,
            long interviewing,
            long offers,
            double responseRate,
            List<JobApplication> recent,
            List<JobApplication> needsAttention) {
    }
}
