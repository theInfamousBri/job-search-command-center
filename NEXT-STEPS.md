# Job Search Command Center — Next Steps

This file is the post-**1.1.2** roadmap. It consolidates the ideas intentionally deferred while the first usable local version was being built.

The order below is directional rather than a promise. Features should continue to be added when they solve a real workflow problem rather than just making the project larger.

## Near-term product priorities

### 1. True global search / command palette

The current top-bar search is application-focused. Expand it into one search surface across:

- applications
- lifecycle events
- saved job descriptions
- prep items
- STAR stories
- company research
- contacts / people once added

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

### 3. Contacts / people CRM

Create reusable people records for:

- recruiters
- hiring managers
- interviewers
- referrals
- networking contacts

Potential fields:

- name
- company
- title / relationship
- email
- LinkedIn URL
- notes
- first / last contact dates
- linked applications

Application lifecycle events could reference a person record instead of relying only on free-form `contact_name` text.

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

Version 1.1.2 archives cover-letter text and saved job descriptions directly on the application. A future generalized attachment model could add:

- the exact resume version submitted
- original cover-letter PDF / DOCX files
- portfolio or writing samples
- other application-specific documents

Prefer a reusable attachment table or equivalent model rather than adding one file column per material type. Because the product is local-first, keeping modest attachments inside SQLite would make a database backup self-contained.

### Company pages

Group multiple applications under one company and surface:

- past / active roles
- contacts
- company research
- cumulative interview history
- outcomes

### Company branding management

Build on the v1.1.2 domain-based logo cache with company-level tooling so branding does not have to be maintained one application at a time:

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

Analytics should become more sophisticated only as the dataset grows enough to support meaningful conclusions.

### Better timing statistics

Add:

- median alongside average stage timing
- minimum / maximum
- sample-size warnings
- time from one stage to the next, not only from Applied

### Funnel analysis

Add:

- drop-off rate between stages
- funnel by month / quarter
- compare funnels across role types or locations
- compare before / after resume revisions

### Outcome breakdowns

Version 1.1.1 adds outcome mix plus performance by priority, career lane, work arrangement, and source. Continue with:

- company
- role family normalization
- location
- salary band
- month / quarter
- statistically safer comparisons when sample sizes are small

### Search-strategy insights

Possible future insights:

- which sources generate the highest interview rate
- which companies move fastest
- average waiting time after interviews
- where applications most commonly stop progressing
- application volume vs interview conversion over time

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

Add official Maven Wrapper files:

- `mvnw`
- `mvnw.cmd`
- `.mvn/wrapper/...`

This will make command-line setup more reliable on machines without Maven installed globally.

### Automated tests

Prioritize tests around behavior that protects user data:

- application CRUD
- stage-change lifecycle creation
- timeline editing
- prep linking / unlinking
- review scheduling
- analytics queries
- migration behavior against older SQLite schemas

### GitHub Actions

Add CI for:

- Java 21
- Maven build
- tests

Potential later jobs:

- dependency checks
- release packaging

### Database migrations

The lightweight startup migrations are appropriate for the current local app. If the schema becomes substantially more complex, evaluate a formal migration approach that works reliably with SQLite.

### Error pages

Replace the Spring Whitelabel fallback with branded:

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
- [ ] Add Maven Wrapper.
- [ ] Add a basic CI workflow.
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
