# Changelog

## 1.0.0

First GitHub-ready baseline of Job Search Command Center.

### Included

- Local-first Spring Boot / Thymeleaf / SQLite application.
- Dashboard metrics and needs-attention view.
- Searchable application tracker with stage, state, priority, source, notes, salary, URL, and saved job description.
- Chronological application lifecycle with pipeline, communication, assessment, and activity events.
- Monthly calendar and upcoming-activity view.
- Prep / Notes knowledge base with reusable and application-specific material.
- Prep-to-application linking.
- Confidence-based prep review workflow and review history.
- Funnel, timing, source, state, activity, and prep-health analytics.
- Mockup-inspired visual refresh with mountain branding, navigation icons, utility bar, stat cards, and polished component styling.
- Isolated `demo` profile with 12 fictional applications, lifecycle/calendar activity, prep data, analytics data, and automatic reset on startup.
- Dedicated `JobSearchDemoApplication` launcher on port `8081` for public-safe screenshots without touching `jobsearch.db`.

### Repository cleanup

- Consolidated application detail templates to one canonical `applications/detail.html`.
- Removed obsolete versioned templates left from iterative local patches.
- Updated Maven project version to `1.0.0`.
- Expanded `.gitignore` for local SQLite, IDE, build, and temporary files.
- Replaced iteration-specific roadmap documentation with `NEXT-STEPS.md`.
- Rewrote README for a public GitHub repository.
