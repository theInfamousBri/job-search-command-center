package com.brianna.jobsearch.model;

import java.time.LocalDate;
import java.util.List;

public class CalendarDay {

    private final LocalDate date;
    private final boolean inSelectedMonth;
    private final boolean today;
    private final List<CalendarEntry> events;

    public CalendarDay(LocalDate date, boolean inSelectedMonth, boolean today, List<CalendarEntry> events) {
        this.date = date;
        this.inSelectedMonth = inSelectedMonth;
        this.today = today;
        this.events = events;
    }

    public LocalDate getDate() { return date; }
    public int getDayNumber() { return date.getDayOfMonth(); }
    public boolean isInSelectedMonth() { return inSelectedMonth; }
    public boolean isToday() { return today; }
    public List<CalendarEntry> getEvents() { return events; }
}
