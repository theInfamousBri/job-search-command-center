# Changelog

## 1.1.2

Application materials and company-branding polish.

### Added

- Optional company-domain metadata on applications.
- Locally cached company logos keyed by normalized domain, with initials as the fallback.
- Explicit `Fetch from domain`, manual image upload, refresh, and cached-logo removal controls.
- Full archived cover-letter text in addition to the existing cover-letter-used flag.
- Expandable Application Materials panel for cover letters and saved job descriptions.
- Optional `Company Domain` and `Cover Letter Text` fields for future Excel / CSV imports.
- Demo cover-letter content so the materials accordion can be exercised without personal data.

### Changed

- Company avatars on Dashboard, Applications, stale review, and application detail can render cached logos.
- Saving archived cover-letter text automatically marks the application as having used a cover letter.
- Application search now includes company domain and archived cover-letter content.
- Long-form job descriptions moved into the same collapsed Application Materials area as cover letters.

### Local-first behavior

- Logo bytes are stored in SQLite rather than relying on a third-party image URL during normal page rendering.
- Automatic logo fetching only runs when explicitly requested and only targets common image locations on a public company domain.
- Manual logo uploads are limited to 1 MB and supported raster / icon image formats.

## 1.1.1

Large-history usability and analytics optimization.

### Added

- Server-side application filtering by stage, state, priority, work arrangement, source, career lane, and applied-date range.
- Application sorting and 25 / 50 / 100-row pagination.
- Stale-application review workflow with configurable age thresholds, quick actions, and bulk actions.
- Calendar views for Actionable, Interviews, Assessments, Follow-ups, and All history.
- Analytics outcome mix plus performance breakdowns by priority, career lane, work arrangement, and source.
- Response-rate columns alongside interview-rate source analytics.

### Changed

- Dashboard stale detection now uses a 21-day default and links directly to the review queue.
- Calendar defaults to Actionable so imported historical Applied / Rejected / No Response events do not overwhelm the month view.
- Analytics Current States now focuses on still-open applications; terminal results live in the new Outcome panel.

## 1.1.0

Historical data import and richer application metadata.

### Added

- Preview-first Excel (`.xlsx` / `.xls`) and CSV application importer.
- Conservative duplicate detection with merge / import-separate / skip decisions.
- Historical lifecycle creation from applied and updated dates.
- Pipeline `Assessment` stage for coding / technical screens.
- `Stretch`, `Skip`, and `Not set` priority values.
- Work arrangement, experience required, career lane, next step / follow-up, and cover-letter fields.
- Search across the new application metadata.
- Application detail next-step callout and richer application-info panel.
- Analytics funnel / velocity support for assessment events.

### Import safety

- No SQLite writes occur until the preview is approved.
- Exact duplicate matching uses job URL or company + role + location + applied date.
- Company + role matches are warnings only and never auto-merged.
- Excel display values are preserved with Apache POI `DataFormatter` to avoid date-serial corruption in fields such as YOE requirements.

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
