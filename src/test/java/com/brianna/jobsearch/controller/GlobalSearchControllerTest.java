package com.brianna.jobsearch.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brianna.jobsearch.model.search.GlobalSearchResponse;
import com.brianna.jobsearch.model.search.GlobalSearchResponse.Group;
import com.brianna.jobsearch.model.search.GlobalSearchResponse.Result;
import com.brianna.jobsearch.service.GlobalSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GlobalSearchControllerTest {

    @Mock private GlobalSearchService searchService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new GlobalSearchController(searchService)).build();
    }

    @Test
    void searchReturnsJsonAndDisablesCaching() throws Exception {
        Result result = new Result(
                "application", "Senior Software Engineer", "Mastercard", "R-274666 · Applied",
                "/applications/42", "MA", "Exact requisition", true);
        when(searchService.search("R-274666")).thenReturn(new GlobalSearchResponse(
                "R-274666", 1, List.of(new Group("applications", "Applications", List.of(result)))));

        mvc.perform(get("/api/search").param("q", "R-274666"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.query").value("R-274666"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.groups[0].results[0].exactMatch").value(true));
    }

    @Test
    void searchTrimsAndCapsVeryLongQueriesBeforeDelegating() throws Exception {
        String longQuery = "x".repeat(250);
        when(searchService.search(argThat(query -> query != null && query.length() == 200)))
                .thenReturn(new GlobalSearchResponse("x".repeat(200), 0, List.of()));

        mvc.perform(get("/api/search").param("q", "  " + longQuery + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0));

        verify(searchService).search(argThat(query -> query.length() == 200));
    }
}
