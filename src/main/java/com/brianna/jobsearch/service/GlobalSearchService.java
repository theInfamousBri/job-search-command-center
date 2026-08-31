package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.search.GlobalSearchResponse;
import com.brianna.jobsearch.model.search.GlobalSearchResponse.Group;
import com.brianna.jobsearch.model.search.GlobalSearchResponse.Result;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Service
public class GlobalSearchService {

    private static final int APPLICATION_LIMIT = 6;
    private static final int COMPANY_LIMIT = 5;
    private static final int PEOPLE_LIMIT = 6;

    private final JobApplicationService applicationService;
    private final CompanyManagementService companyService;

    public GlobalSearchService(
            JobApplicationService applicationService,
            CompanyManagementService companyService) {
        this.applicationService = applicationService;
        this.companyService = companyService;
    }

    public GlobalSearchResponse search(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isBlank()) {
            return new GlobalSearchResponse("", 0, List.of());
        }

        List<Group> groups = new ArrayList<>();

        List<Result> applications = applicationService.searchGlobal(query, APPLICATION_LIMIT).stream()
                .map(application -> applicationResult(application, query))
                .toList();
        if (!applications.isEmpty()) {
            groups.add(new Group("applications", "Applications", applications));
        }

        List<Result> companies = companyService.searchCompanies(query, COMPANY_LIMIT).stream()
                .map(this::companyResult)
                .toList();
        if (!companies.isEmpty()) {
            groups.add(new Group("companies", "Companies", companies));
        }

        List<Result> people = companyService.searchPeople(query, PEOPLE_LIMIT).stream()
                .map(this::personResult)
                .toList();
        if (!people.isEmpty()) {
            groups.add(new Group("people", "People", people));
        }

        int total = groups.stream().mapToInt(group -> group.results().size()).sum();
        return new GlobalSearchResponse(query, total, List.copyOf(groups));
    }

    private Result applicationResult(JobApplication application, String query) {
        boolean exactRequisition = application.getRequisitionId() != null
                && application.getRequisitionId().trim().equalsIgnoreCase(query);

        String subtitle = joinNonBlank(" · ", application.getCompany(), application.getLocationDisplay());
        String meta = joinNonBlank(" · ",
                application.getRequisitionId(),
                application.getStatus() == null ? null : application.getStatus().getDisplayName(),
                application.getState() == null ? null : application.getState().getDisplayName());

        return new Result(
                "application",
                application.getRole(),
                subtitle,
                meta,
                "/applications/" + application.getId(),
                application.getInitials(),
                exactRequisition ? "Exact requisition" : null,
                exactRequisition);
    }

    private Result companyResult(CompanyManagementService.CompanyGroup company) {
        String meta = company.applications() + " application" + (company.applications() == 1 ? "" : "s")
                + " · " + company.openApplications() + " open";
        String subtitle = company.hasSingleDomain() ? company.domain() : null;
        return new Result(
                "company",
                company.displayName(),
                subtitle,
                meta,
                "/companies/" + encodePathSegment(company.key()),
                company.initials(),
                null,
                false);
    }

    private Result personResult(CompanyManagementService.CompanyPersonSearchResult hit) {
        CompanyContact person = hit.contact();
        String subtitle = joinNonBlank(" · ", person.relationship().getDisplayName(), hit.companyDisplayName());
        String meta = joinNonBlank(" · ", person.role(), person.email());
        return new Result(
                "person",
                person.name(),
                subtitle,
                meta,
                "/companies/" + encodePathSegment(person.companyKey()) + "#person-" + person.id(),
                person.initials(),
                null,
                false);
    }

    private String joinNonBlank(String delimiter, String... values) {
        return java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + delimiter + right)
                .orElse(null);
    }

    private String encodePathSegment(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
