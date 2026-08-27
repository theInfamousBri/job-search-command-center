package com.brianna.jobsearch.model;

import java.time.LocalDate;

public class ApplicationSearchCriteria {

    private final String query;
    private final ApplicationStatus status;
    private final ApplicationState state;
    private final Priority priority;
    private final String workArrangement;
    private final String source;
    private final String careerLane;
    private final LocalDate appliedFrom;
    private final LocalDate appliedTo;
    private final ApplicationSort sort;
    private final int page;
    private final int size;

    public ApplicationSearchCriteria(
            String query,
            ApplicationStatus status,
            ApplicationState state,
            Priority priority,
            String workArrangement,
            String source,
            String careerLane,
            LocalDate appliedFrom,
            LocalDate appliedTo,
            ApplicationSort sort,
            int page,
            int size) {
        this.query = normalize(query);
        this.status = status;
        this.state = state;
        this.priority = priority;
        this.workArrangement = normalize(workArrangement);
        this.source = normalize(source);
        this.careerLane = normalize(careerLane);
        this.appliedFrom = appliedFrom;
        this.appliedTo = appliedTo;
        this.sort = sort == null ? ApplicationSort.UPDATED_DESC : sort;
        this.page = Math.max(0, page);
        this.size = Math.max(10, Math.min(size, 100));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public boolean hasFilters() {
        return query != null || status != null || state != null || priority != null
                || workArrangement != null || source != null || careerLane != null
                || appliedFrom != null || appliedTo != null;
    }

    public String getQuery() { return query; }
    public ApplicationStatus getStatus() { return status; }
    public ApplicationState getState() { return state; }
    public Priority getPriority() { return priority; }
    public String getWorkArrangement() { return workArrangement; }
    public String getSource() { return source; }
    public String getCareerLane() { return careerLane; }
    public LocalDate getAppliedFrom() { return appliedFrom; }
    public LocalDate getAppliedTo() { return appliedTo; }
    public ApplicationSort getSort() { return sort; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
