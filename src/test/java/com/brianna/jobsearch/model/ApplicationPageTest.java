package com.brianna.jobsearch.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationPageTest {

    @Test
    void reportsRangeAndNavigationForLargeApplicationHistory() {
        ApplicationPage page = new ApplicationPage(List.of(), 163, 2, 25);

        assertThat(page.getPageNumber()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(7);
        assertThat(page.getFirstItem()).isEqualTo(51);
        assertThat(page.getLastItem()).isEqualTo(75);
        assertThat(page.isHasPrevious()).isTrue();
        assertThat(page.isHasNext()).isTrue();
        assertThat(page.getVisiblePages()).containsExactly(0, 1, 2, 3, 4);
    }
}
