# Changelog

## 1.2.0

Analytics expansion focused on search-strategy decision support.

### Added

- Search Strategy summary cards for priority, career lane, work arrangement, and source.
- Visual response and interview rate bars for each tracked performance dimension.
- Percentage-point deltas against the user's overall response and interview baselines.
- Response-to-interview conversion for each segment so replies and interview traction can be distinguished.
- Coverage indicators showing how many submitted applications have each analytics field populated.
- Sample-size labels: `Stronger sample` (10+ applications), `Directional` (3–9), and `Small sample` (fewer than 3).
- Sample-aware leader selection so a one-application outlier is not promoted over a segment with usable history.

### Changed

- Priority, career-lane, work-arrangement, and source analytics now keep 3+ application samples visible and tuck 1–2 application categories into consistent collapsed small-samples drawers.
- Small-sample Search Strategy winners are labeled as an `Early signal` instead of visually promoting a 100% rate from one or two applications.
- Low career-lane coverage now shows a `Needs more tagging` cue so sparse categorization is obvious at a glance.
- Analytics now cache-busts its stylesheet reference for v1.2.0 so visual updates are not hidden by a stale browser CSS cache.
- Reorganized Analytics so search-strategy signals sit above the detailed performance breakdowns.
- Priority, career-lane, work-arrangement, and source tables are now visual comparison panels rather than raw rate tables.
- Source analytics now explicitly show how many responses continue into interviews.
- Analytics copy distinguishes descriptive historical signals from predictions.
- Demo data now includes broader repeated career-lane and work-arrangement labels so v1.2.0 decision-support views are useful in public-safe screenshots.

### Data / migration

- No database migration is required for v1.2.0. The release uses application and lifecycle fields already introduced in v1.1.x.

## 1.1.2

Application materials and company-branding polish.

### Added

- Optional company-domain metadata on applications.
- Locally cached company logos keyed by normalized domain, with initials as the fallback.
- Explicit `Fetch from domain`, manual image upload, refresh, and cached-logo removal controls.
- Automatic logo refresh when an application company domain is added or changed.
- Smart favicon discovery from homepage `<link>` declarations, web manifests, common fallback paths, and SVG icons.
- Full archived cover-letter text in addition to the existing cover-letter-used flag.
- Expandable Application Materials panel for cover letters and saved job descriptions.
- Optional `Company Domain` and `Cover Letter Text` fields for future Excel / CSV imports.
- Demo cover-letter content so the materials accordion can be exercised without personal data.

### Changed

- Company avatars on Dashboard, Applications, stale review, and application detail can render cached logos.
- Saving archived cover-letter text automatically marks the application as having used a cover letter.
- Application search now includes company domain and archived cover-letter content.
- Long-form job descriptions moved into the same collapsed Application Materials area as cover letters.
- Application create/edit actions use a sticky bottom action bar so Cancel / Save remain reachable on long forms.
- Startup schema initialization was made backward-safe so the company-domain index is created only after the migration adds the column on older databases.

### Local-first behavior

- Logo bytes are stored in SQLite rather than relying on a third-party image URL during normal page rendering.
- Automatic logo fetching runs when a company domain changes or when manually requested, targets the public company domain, validates redirect targets, discovers declared site icons, and caches the selected result locally.
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
