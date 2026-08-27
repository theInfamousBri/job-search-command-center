package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.CalendarDay;
import com.brianna.jobsearch.model.CalendarEntry;
import com.brianna.jobsearch.service.JobApplicationService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    private static final DateTimeFormatter MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final JobApplicationService service;

    public CalendarController(JobApplicationService service) {
        this.service = service;
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) String month, Model model) {
        YearMonth selectedMonth = parseMonth(month);
        LocalDate today = LocalDate.now();

        LocalDate gridStart = selectedMonth.atDay(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = selectedMonth.atEndOfMonth()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<CalendarEntry> entries = service.calendarEvents(gridStart, gridEnd);
        Map<LocalDate, List<CalendarEntry>> eventsByDate = entries.stream()
                .collect(Collectors.groupingBy(CalendarEntry::getEventDate));

        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            days.add(new CalendarDay(
                    date,
                    YearMonth.from(date).equals(selectedMonth),
                    date.equals(today),
                    eventsByDate.getOrDefault(date, List.of())));
        }

        List<CalendarEntry> upcoming = service.calendarEvents(today, today.plusDays(30));

        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("monthTitle", selectedMonth.format(MONTH_TITLE));
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1).toString());
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1).toString());
        model.addAttribute("todayMonth", YearMonth.from(today).toString());
        model.addAttribute("days", days);
        model.addAttribute("upcoming", upcoming);
        return "calendar";
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }

        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ignored) {
            return YearMonth.now();
        }
    }
}
