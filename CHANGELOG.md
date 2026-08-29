# Changelog

## 1.3.0 (in progress)

Data quality and organization foundation.

### Added

- New **Data Quality** workspace with coverage metrics for Career Lane, Industry / Domain, Source, Work Arrangement, Priority, and Company Domain.
- Clickable missing-data counts that open the Applications tracker filtered to records that need that field.
- Structured career taxonomy fields: broad `Career Lane / Role Family`, `Industry / Domain`, and optional `Focus`.
- Broad role-family and industry/domain enums so new data is normalized at entry time instead of creating one-off analytics categories.
- Expanded the taxonomy from real historical application tags with first-class Forward Deployed / Customer Engineering and Robotics / Embedded / Autonomy role families, plus Aerospace & Defense, Manufacturing & Industrial, Robotics & Autonomy, Logistics & Supply Chain, Travel & Hospitality, Climate & Sustainability, and Legal & Compliance domains. Existing enum identifiers remain stable for persisted SQLite values.
- Data-quality coverage states (`Strong coverage`, `Partial coverage`, and `Needs attention`) plus an overall completeness snapshot.
- Career Lane and Industry / Domain filters in the Applications tracker.
- New **Normalization Center** under Data Quality for review-before-write cleanup of historical Career Lane, Source, and Work Arrangement values.
- Bulk career-taxonomy mapping from preserved original tags into normalized Career Lane / Role Family, Industry / Domain, and optional Focus fields.
- Keyword-based career mapping suggestions that can be reviewed and applied in bulk without overwriting existing normalized fields.
- Bulk label normalization for Source and Work Arrangement values, including suggested canonical casing / broad work-type labels.
- New **Companies** workspace that groups applications by normalized company identity and centralizes domain/logo maintenance.
- Company-level domain propagation so one domain update can apply to every matching application.
- Centralized logo fetch/refresh, manual upload, removal, and initials fallback using the existing domain-keyed SQLite logo cache.
- Company-name alias cleanup, including automatic punctuation/legal-suffix grouping plus explicit multi-group merge into a chosen canonical name.
- Clickable company detail pages with grouped application history and persistent company-level notes.
- Company-domain suggestions on application create/edit forms when the entered company already has a single known shared domain.
- Company detail rendering now avoids Thymeleaf's reserved `application` web-variable name.
- Company Directory cards are compact browse-first entries; branding/domain/logo/rename controls now live on the company detail page.
- Company Directory adds 20/40/80-item pagination while preserving search and cleanup filters.
- Broken-logo fallback now keeps initials visible, and downloaded logo bytes are signature-validated before caching.
- Company pages now include a **People at this company** directory for recruiters, hiring managers, interviewers, referrals, team members, and networking contacts.
- Company people records support name, role/title, relationship type, email, LinkedIn reference URL, notes, and an optional locally stored profile photo.
- Company people survive company rename/alias merges by following the normalized company identity.
- Company detail pages were visually reworked into a workspace: applications, people, and notes stay prominent while branding/identity maintenance is collapsed under **Manage company**.
- Profile-photo uploads are validated as PNG, JPEG, GIF, or WebP and limited to 1 MB; initials remain the fallback.

### Changed

- v1.2 Career Lane analytics now use the normalized role-family field rather than the older free-form Career Lane string.
- The Analytics `Needs more tagging` cue now links directly to applications missing a normalized Career Lane.
- Existing free-form Career Lane values are preserved as an **Original career tag** and shown during editing instead of being discarded.
- All application pages now cache-bust `app.css` with the v1.3 development version so UI changes are not hidden by a stale browser stylesheet.
- Normalization updates classification fields directly without changing `updated_at`, applied dates, or lifecycle events, so cleanup does not make historical applications look newly active.
- Demo data now keeps richer legacy Career Lane strings alongside normalized fields so the Normalization Center can be exercised safely.
- Company branding/name cleanup updates intentionally preserve application `updated_at`, so organization work does not make historical applications look newly active.
- Application-list logos again fill their avatar containers while Company Directory cards keep the intentionally inset logo treatment.

### Data / migration

- Adds nullable `role_family`, `industry_domain`, and `career_focus` columns to `job_applications`.
- Existing `career_lane` data is intentionally left untouched so the upcoming normalization workflow can map it without losing detail.
- New indexes are created only after startup migration adds the new columns, keeping upgrades from older SQLite databases backward-safe.
- Adds a `company_notes` table keyed by normalized company identity for reusable company-level notes.
- Adds a `company_contacts` table for company-level people records and optional profile-photo BLOBs.

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
