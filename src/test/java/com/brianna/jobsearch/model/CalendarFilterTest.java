package com.brianna.jobsearch.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalendarFilterTest {

    @Test
    void actionableHidesHistoricalTerminalEvents() {
        assertThat(CalendarFilter.ACTIONABLE.includes(ApplicationEventType.APPLIED)).isFalse();
        assertThat(CalendarFilter.ACTIONABLE.includes(ApplicationEventType.REJECTED)).isFalse();
        assertThat(CalendarFilter.ACTIONABLE.includes(ApplicationEventType.NO_RESPONSE)).isFalse();
        assertThat(CalendarFilter.ACTIONABLE.includes(ApplicationEventType.TECHNICAL_INTERVIEW)).isTrue();
        assertThat(CalendarFilter.ACTIONABLE.includes(ApplicationEventType.FOLLOW_UP)).isTrue();
    }

    @Test
    void focusedViewsOnlyIncludeTheirRelevantEvents() {
        assertThat(CalendarFilter.INTERVIEWS.includes(ApplicationEventType.INTERVIEW_SCHEDULED)).isTrue();
        assertThat(CalendarFilter.INTERVIEWS.includes(ApplicationEventType.FINAL_ROUND)).isTrue();
        assertThat(CalendarFilter.INTERVIEWS.includes(ApplicationEventType.CODING_ASSESSMENT)).isFalse();

        assertThat(CalendarFilter.ASSESSMENTS.includes(ApplicationEventType.CODING_ASSESSMENT)).isTrue();
        assertThat(CalendarFilter.ASSESSMENTS.includes(ApplicationEventType.TAKE_HOME_ASSESSMENT)).isTrue();
        assertThat(CalendarFilter.ASSESSMENTS.includes(ApplicationEventType.RECRUITER_CONTACT)).isFalse();

        assertThat(CalendarFilter.FOLLOW_UPS.includes(ApplicationEventType.FOLLOW_UP)).isTrue();
        assertThat(CalendarFilter.FOLLOW_UPS.includes(ApplicationEventType.RECRUITER_CONTACT)).isFalse();
    }
}
