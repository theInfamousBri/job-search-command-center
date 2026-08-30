# Changelog

## 1.4.0 (in progress)

### Application detail polish

- Refines the v1.4.3 application overview into a clearer Notes / People / Role Details hierarchy.
- Retires the legacy Original Career Tag from normal application UI while preserving `career_lane` in SQLite for backward compatibility and old imports.
- Standardizes the active taxonomy label on **Role Family** instead of using Career Lane for both legacy and normalized concepts.
- Collapses unusually long working notes behind an inline Show full notes control.
- Makes the zero-People state quieter and gives Focus full-width treatment in Role Details.
- Removes duplicated company-domain/posting rows from Role Details and demotes last-updated metadata to a subtle footer.

### Engineering foundation

- Starts the v1.4 development line at `1.4.0-SNAPSHOT`.
- Adds Maven Wrapper launchers pinned to Maven 3.9.16 so local development and CI do not depend on a globally installed Maven version.
- Adds JaCoCo 0.8.15 coverage reporting during Maven `verify`; coverage is informational in this first chunk rather than enforced as a percentage gate.
- Adds a GitHub Actions CI workflow that checks out the repository, sets up Eclipse Temurin Java 21, and runs `./mvnw clean verify` on pushes and pull requests. The workflow explicitly restores the Unix executable bit on `mvnw` so Windows-created commits do not fail on Ubuntu with `Permission denied`.
- Adds branded 404 and 500 pages and disables the default Whitelabel/error-detail browser surface.
- Missing applications, timeline events, prep items, company groups, and application attachments now surface as true HTTP 404 resources where they are not already handled as form actions.
- Adds regression tests for application lifecycle synchronization, child-row deletion order, attachment validation/scoping, and resource-not-found behavior.
- Adds a migration test fixture based on the released v1.2 schema to verify v1.3+ migrations are idempotent and do not fake application activity by changing `updated_at`.


### Shared Materials / Resume Library

- Adds a dedicated **Materials Library** for reusable resumes and other files that should be stored once and referenced by many applications.
- Adds `material_files` for unique file BLOBs and `application_material_links` for many-to-many application references.
- Uses SHA-256 content hashes to detect exact duplicate uploads and reuse the existing stored BLOB instead of growing the SQLite database with another copy.
- Adds library names and optional notes so resume versions can be labeled by purpose/date while preserving the original filename for downloads.
- Adds Materials Library storage/usage summaries, including an estimate of duplicate storage avoided through reuse.
- Application detail can link an existing library material, upload a new resume version and link it immediately, download the shared file, or unlink it without deleting the library copy.
- Existing v1.3 `RESUME` rows in `application_attachments` migrate automatically into the shared library on startup; byte-identical resumes are deduplicated and linked back to every original application without touching application `updated_at` timestamps.
- `application_attachments` remains available for application-specific cover letters and one-off supporting documents.
- Deleting an application now removes its material links while leaving reusable library files intact for other applications.
- Adds service, repository-integration, and migration regression coverage for shared materials and resume deduplication.


### People ↔ Applications

- Adds `application_contact_links` so company-level People can be connected to any number of applications without duplicating the contact record.
- Application detail now shows the recruiters, hiring managers, interviewers, referrals, team members, and other contacts involved with that role.
- Existing People from the application company can be linked or unlinked directly from the application page; cross-company links are rejected.
- Company People cards now show how many applications reference each person.
- Linking/unlinking People is relationship metadata only and does not touch application or contact `updated_at` timestamps.
- Deleting an application removes its person links while preserving the reusable company contact; deleting a person removes that person’s application links.
- Demo mode includes representative Person ↔ Application links for the active interview flows.
- Adds repository, service, migration, and HTTP/controller regression tests around the new relationship, including 404 behavior and company scoping.


### Application Detail organization

- Starts the v1.4.3 Application Detail redesign by moving high-value application context above the long timeline/prep sections.
- Adds a lightweight Overview / Timeline / Prep / Materials jump bar while keeping the page server-rendered and fully available as one document.
- Moves Application Info into the top overview area and removes fields already repeated in the header/summary so the detail panel stays compact.
- Moves People beneath Working Notes in the left overview column and replaces the large full-width contact cards with compact linked-person rows and a collapsed linker.
- Keeps Timeline, Prep, Materials, company branding, and destructive maintenance actions progressively deeper in the page.



## 1.3.0

Data quality, organization, company workspaces, people, and application materials.

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
- Application detail pages now support generalized **SQLite-backed file attachments** for exact resume versions, original cover-letter files, and other application-specific documents.
- Attachment downloads preserve the original filename while file bytes remain local to the database; metadata lists do not load BLOB content until a download is requested.
- Attachment uploads are limited to 10 MB per file and are removed automatically when their parent application is deleted.
- Adding a file as **Cover letter** marks the existing cover-letter-used flag without touching the application activity timestamp; deleting the file does not rewrite historical usage.

### Changed

- v1.2 Career Lane analytics now use the normalized role-family field rather than the older free-form Career Lane string.
- The Analytics `Needs more tagging` cue now links directly to applications missing a normalized Career Lane.
- Existing free-form Career Lane values are preserved as an **Original career tag** and shown during editing instead of being discarded.
- All pages now share a v1.3.0 release cache key for `app.css` so the final UI is not hidden by an older development stylesheet cached by the browser.
- Normalization updates classification fields directly without changing `updated_at`, applied dates, or lifecycle events, so cleanup does not make historical applications look newly active.
- Demo data now keeps richer legacy Career Lane strings alongside normalized fields so the Normalization Center can be exercised safely.
- Company branding/name cleanup updates intentionally preserve application `updated_at`, so organization work does not make historical applications look newly active.
- Application-list logos again fill their avatar containers while Company Directory cards keep the intentionally inset logo treatment.

### Data / migration

- Adds nullable `role_family`, `industry_domain`, and `career_focus` columns to `job_applications`.
- Existing `career_lane` data is intentionally left untouched so the Normalization Center can map it without losing detail.
- New indexes are created only after startup migration adds the new columns, keeping upgrades from older SQLite databases backward-safe.
- Adds a `company_notes` table keyed by normalized company identity for reusable company-level notes.
- Adds a `company_contacts` table for company-level people records and optional profile-photo BLOBs.
- Adds an `application_attachments` table with application linkage, material type, original filename/MIME metadata, and locally stored BLOB content.
- Release cleanup refreshes README/schema documentation and removes stale roadmap/limitations text now superseded by company People and attachments.

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
