# Job Search Command Center — Next Steps

This file tracks the **v1.4 roadmap and later ideas** while preserving completed v1.3 scope for context as the local-first command center grows.

The order below is directional rather than a promise. Features should continue to be added when they solve a real workflow problem rather than just making the project larger.

## v1.3.0 — Data Quality & Organization (completed)

### Completed foundation

- [x] Data Quality dashboard with field coverage and missing counts.
- [x] Click-through from missing counts to filtered Applications.
- [x] Split Career Lane into normalized **Role Family**, **Industry / Domain**, and optional **Focus**.
- [x] Preserve the previous free-form Career Lane as an original/legacy tag for later mapping.
- [x] Point Career Lane analytics at the normalized role-family field.
- [x] Expand Role Family and Industry / Domain enums against the historical Career Lane dataset before bulk normalization.

### Completed expansion

- [x] Normalization Center for preserved Career Lane strings, Source labels, and Work Arrangements.
- [x] Bulk cleanup / bulk assignment so historical taxonomy and label data can be normalized without editing applications one at a time.
- [x] Review-before-write keyword suggestions for broad Career Lane / Industry mappings, while preserving the original career tag and lifecycle timestamps.
- [x] Company alias normalization and centralized Company Branding manager with company grouping, domain propagation, logo refresh/upload/remove, and initials fallback.
- [x] Company detail pages with grouped application history, persistent company notes, and known-domain suggestions during application entry.
- [x] Company workspace polish with a company-level people directory, contact CRUD, LinkedIn/email references, optional local profile photos, and preservation across company rename/merge.
- [x] Generalized application attachments stored locally, beginning with original cover-letter files and exact resume versions.

The v1.3 scope is complete. New work starts with the post-1.3 priorities below rather than expanding the release further.


## v1.4.0 — Workflow Intelligence & Personalization (in progress)

The working theme for v1.4 is making the command center faster, more connected, more informative, and safer to evolve.

### Chunk 0 — Engineering baseline

- [x] Move development to `1.4.0-SNAPSHOT`.
- [x] Add Maven Wrapper launchers and pin the Maven distribution.
- [x] Add JaCoCo coverage reporting without an arbitrary initial coverage gate.
- [x] Add GitHub Actions CI for Java 21 + Maven verification.
- [x] Add branded 404 / 500 pages and true not-found handling for missing core resources.
- [x] Expand regression tests around application lifecycle behavior and attachments.
- [x] Add a real older-schema migration fixture and migration/idempotency tests.

### Product chunks

1. **Shared Materials / Resume Library — completed** — reusable files are stored once, SHA-256 deduplicated, and linked many-to-many to applications. Existing v1.3 Resume attachments migrate into the library automatically while application-specific files remain separate.
2. **People ↔ Applications** — many-to-many linking between company contacts and applications, with a path toward lifecycle events referencing saved people.
3. **Application Detail navigation** — Overview / Timeline / Prep / People / Materials organization without turning the server-rendered app into a SPA.
4. **Global Search / `Ctrl/Cmd + K`** — grouped search across applications, companies, people, prep, descriptions, and other useful local content.
5. **Compensation context** — restrained salary-range visualization using comparable tracked roles, medians/quartiles, and sample-size fallbacks rather than noisy global min/max comparisons.
6. **Smarter Needs Attention** — context-aware interview/follow-up/prep cues with useful direct actions.
7. **Dark mode + accessibility** — System / Light / Dark theming plus contrast, keyboard, focus-state, semantic-label, and form-error polish.
8. **Backup / Export + acceptance** — full SQLite backup, useful exports, database-size visibility, and final v1.4 release cleanup.

Engineering coverage should continue to grow alongside these chunks, especially for new migrations and relationships that can affect stored user data.

## Near-term product priorities

### 1. True global search / command palette

The current top-bar search is application-focused. Expand it into one search surface across:

- applications
- lifecycle events
- saved job descriptions
- prep items
- STAR stories
- company research
- company people / contacts

Possible UX:

- `Ctrl/Cmd + K` command palette
- grouped results by resource type
- recent items / quick-jump history
- keyboard-first navigation

### 2. Smarter dashboard actions

The 1.1.1 stale-review queue now handles old active applications. Continue making **Needs attention** more context-aware:

- `Waiting 5 days after technical interview`
- interview tomorrow / prep incomplete
- application still marked `Interview Scheduled` after the interview date
- direct buttons for `Open prep` or context-aware follow-up creation
- configurable attention rules

### 3. People CRM expansion

v1.3 now stores reusable people records inside each company workspace for recruiters, hiring managers, interviewers, referrals, team members, and networking contacts. Remaining expansion ideas:

- global People directory across all companies
- first / last contact dates
- direct links between people and specific applications
- lifecycle events referencing a saved person instead of only free-form `contact_name` text
- follow-up reminders and communication history

LinkedIn URLs are saved only as references; the app should not depend on scraping LinkedIn profile pages or profile photos.

### 4. Application detail tabs

Move the long application page toward the mockup-style layout:

- Overview
- Timeline
- Prep & Notes
- Contacts
- Files / Links

Keep the information available on one page for fast navigation, but use tabs or anchors to reduce vertical scanning.

### 5. Calendar refinement

Version 1.1.1 adds Actionable / Interviews / Assessments / Follow-ups / All history filters. Remaining ideas:

- dedicated upcoming-interviews rail
- click an empty date to add an activity
- quick-create a future follow-up date from an application
- clearer future vs completed activity states
- optional communication-only / pipeline-only views if they prove useful

Google Calendar sync belongs later under integrations.

## Application tracking improvements

### Application attachments

v1.3 introduced application-specific SQLite BLOB attachments. v1.4.1 separates reusable material from one-off files:

- reusable resumes/materials live in `material_files` and are linked through `application_material_links`
- byte-identical uploads are SHA-256 deduplicated so the BLOB exists only once
- existing v1.3 Resume attachments migrate automatically into the shared library
- original cover-letter files and one-off supporting documents remain application-specific in `application_attachments`
- metadata queries intentionally avoid loading BLOB content until download

Possible later polish: richer file-type icons, bulk export, replacement/version-family relationships between resume revisions, and analytics by resume version once the dataset supports it.

### Company pages

Implemented in v1.3: company detail pages now surface grouped application history, reusable company notes, and the shared branding/identity controls. The directory is browse-first and paginated. Remaining expansion ideas:

- global People directory and application-to-person linking
- richer structured company research beyond the current notes field
- cumulative interview history
- company-level outcomes / analytics

### Company branding management

Implemented in v1.3: company-level tooling now builds on the v1.1.2 domain-based logo cache so branding does not have to be maintained one application at a time:

- propagate a company domain to every application with the same normalized company name
- reuse the same cached logo automatically across those matching applications
- add a centralized branding cleanup screen grouped by company
- show application count, current domain, logo status, and initials fallback at a glance
- allow setting / correcting a domain once for the whole company group
- allow refresh, manual upload, or logo removal from the centralized screen
- keep per-application overrides possible if two similarly named companies ever need different branding

### Source normalization

Replace free-form source strings with normalized values such as:

- LinkedIn
- Company Site
- Recruiter
- Referral
- Indeed
- Handshake
- Other

Allow a custom display value where needed. This will improve source analytics.

### Job-description formatting

Improve saved job descriptions with:

- headings / bullets
- extracted responsibilities
- requirements
- compensation
- tech-stack tags
- highlights used during interview prep

### Saved / interested roles

Refine the `Saved` stage into a better pre-application workflow:

- application deadline
- priority
- `Apply next` queue
- archived / skipped saved roles

## Prep and review improvements

### Structured STAR stories

Store behavioral stories as separate fields:

- Situation
- Task
- Action
- Result
- lessons / reflection
- question themes

Keep an optional free-form version for storytelling notes.

### Interview-specific study sets

Generate a focused prep collection for an upcoming application from:

- linked reusable topics
- company research
- role keywords
- weak-confidence items

### Related topics

Allow prep items to reference related material, for example:

`HashMap vs ConcurrentHashMap` → `Thread safety` → `synchronized` → `Atomic operations`

### Review-history visualization

Only if it proves useful:

- confidence history
- review counts over time
- upcoming review load

Avoid adding streaks / gamification unless they actually improve interview preparation.

### Library controls

Add:

- grid / list toggle
- sort by updated, confidence, last reviewed, title
- tag filters
- bulk link/unlink to an application

## Analytics roadmap

Version 1.2.0 turns priority, career lane, work arrangement, and source performance into visual decision support with overall-baseline deltas, response-to-interview conversion, field coverage, and sample-size context. Continue making Analytics more sophisticated only when the dataset supports meaningful conclusions.

### Better timing statistics

Add:

- median alongside average stage timing
- minimum / maximum
- time from one stage to the next, not only from Applied
- clearer sample-size context on timing metrics

### Funnel analysis

Add:

- drop-off rate between stages
- funnel by month / quarter
- compare funnels across role types or locations
- compare before / after resume revisions

### Deeper performance dimensions

Extend the v1.2.0 comparison model to:

- company
- normalized role family
- location / metro
- salary band
- month / quarter
- resume version once attachments / resume tracking exists

Consider stronger statistical safeguards once enough observations exist, rather than adding arbitrary scoring formulas.

### Search-strategy insights

Possible next insights:

- which companies move fastest
- average waiting time after interviews
- where applications most commonly stop progressing
- application volume vs interview conversion over time
- changes in response / interview rates over rolling periods
- whether a strategy signal is improving or degrading over time

## Import, export, and backup

### Import refinements

The v1.1 preview-first Excel / CSV importer now handles the current historical tracker format. Future refinements:

- user-defined column mapping for unrelated spreadsheet layouts
- import batch history / undo
- richer parsing of interview dates embedded in follow-up text
- source normalization during import
- optional dry-run export of the normalized data

### Export / backup UI

Allow the user to export:

- applications CSV
- lifecycle CSV / JSON
- prep-library export
- full SQLite database backup

### Job-posting import

Eventually support creating an application from a posting URL or pasted job description.

Possible extraction targets:

- company
- role
- location
- compensation
- job description
- tech keywords

Avoid brittle scraping where a pasted description is more reliable.

## Integrations / automation

These are intentionally later because they add authentication, API, and privacy complexity.

### Email integration

Potential workflow:

- identify recruiter / application status emails
- suggest lifecycle events
- update application state
- detect interview scheduling or rejection messages

Prefer **review-before-write** behavior instead of silently modifying application history.

### Calendar integration

Potential Google Calendar integration for:

- importing interview invitations
- linking calendar events to applications
- syncing scheduled interviews / follow-ups

### Notifications

Local or integrated reminders for:

- follow-ups
- upcoming interviews
- prep due for review
- stale active applications

## UX / visual roadmap

### Dark mode

The v1.0.0 design system is structured enough for a proper theme layer. Add dark mode without simply inverting colors.

### Responsive design

Continue refining:

- tablet layout
- mobile application cards
- calendar behavior on narrow screens
- collapsible navigation

Desktop remains the primary workflow.

### Brand refinement

The mountain / upward-path identity is working well. Potential future polish:

- refined standalone logo geometry
- alternate small icon
- social / repository preview image
- consistent empty-state illustrations

### Accessibility

Audit:

- keyboard navigation
- focus states
- semantic labels
- contrast
- screen-reader text
- form error announcements

## Engineering backlog

### Maven Wrapper

Implemented as the v1.4 engineering baseline. The repository now includes wrapper launchers and pinned wrapper properties:

- `mvnw`
- `mvnw.cmd`
- `.mvn/wrapper/...`

This will make command-line setup more reliable on machines without Maven installed globally.

### Automated tests

Expanded in the v1.4 engineering baseline; continue prioritizing behavior that protects user data:

- application CRUD
- stage-change lifecycle creation
- timeline editing
- prep linking / unlinking
- review scheduling
- analytics queries
- migration behavior against older SQLite schemas

### GitHub Actions

Implemented as the v1.4 engineering baseline for:

- Java 21
- Maven build
- tests

Potential later jobs:

- dependency checks
- release packaging

### Database migrations

The lightweight startup migrations are appropriate for the current local app. If the schema becomes substantially more complex, evaluate a formal migration approach that works reliably with SQLite.

### Error pages

Implemented as the v1.4 engineering baseline with branded:

- 404
- 500
- resource-not-found pages

### Demo-mode refinement

The initial demo profile and synthetic dataset are now implemented. Future refinements could include:

- a one-click reset action while demo mode is running
- a small demo-tour / first-run callout
- optional deterministic demo dates for documentation snapshots
- a curated demo GIF once the public README screenshots are finalized

### Refactoring / maintainability

As features grow, consider:

- extracting calendar logic from the controller
- dedicated DTO / view-model classes
- central date/time formatting utilities
- consistent repository query helpers
- explicit migration classes

Do this when complexity warrants it rather than preemptively.

## Public GitHub repository checklist

Before or shortly after publishing:

- [ ] Confirm `jobsearch.db` and sidecar files are ignored and untracked.
- [ ] Choose a repository license.
- [ ] Capture sanitized screenshots from the built-in demo profile and add them to the README.
- [x] Add Maven Wrapper.
- [x] Add a basic CI workflow.
- [ ] Add a first `v1.0.0` Git tag / GitHub Release.
- [ ] Add repository topics such as `spring-boot`, `java`, `sqlite`, `thymeleaf`, `job-search`.
- [ ] Decide whether contributions / issues are welcome and document expectations if needed.
- [ ] Verify the README setup steps on a clean checkout.

## Ideas intentionally not in 1.0.0

The following are **not bugs** in the initial release; they are deliberately deferred:

- hosted deployment
- accounts / authentication
- multi-user data
- true global search
- contact CRM
- email parsing
- calendar synchronization
- automatic job scraping
- import / export UI
- dark mode
- mobile-first layout

The 1.0.0 goal is a stable, useful, local personal command center first.
