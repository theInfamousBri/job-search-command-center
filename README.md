<div align="center">
  <img src="src/main/resources/static/images/job-search-logo.svg" alt="Job Search Command Center mountain logo" width="92" />

# Job Search Command Center

**A local-first command center for applications, interview prep, follow-ups, calendar events, and job-search analytics.**

Version **1.0.0**
</div>

---

## Overview

Job Search Command Center is a single-user, local web application for managing the full lifecycle of a job search without spreading information across spreadsheets, notes apps, calendars, and browser tabs.

It is intentionally **local-first**. The application runs on your computer, uses a local SQLite database, and does not require hosting, an account, or a cloud database.

The project began as an application tracker and has grown into a small productivity system with:

- application tracking and search
- pipeline-stage and current-state tracking
- chronological application lifecycles
- interview / assessment / follow-up calendar events
- reusable and application-specific interview prep
- confidence-based review scheduling
- job-search funnel and timing analytics
- a polished dashboard-style UI
- a sanitized demo mode for screenshots and public repository previews

## Features

### Dashboard

The landing page provides an at-a-glance view of the active job search:

- total applications
- active applications
- applications currently interviewing
- offers
- response rate
- recently updated applications
- applications that need attention

### Application tracker

Applications can store:

- company and role
- location
- pipeline stage
- current state
- priority
- source
- original job-posting URL
- salary range
- applied date
- working notes
- a saved copy of the job description

The tracker supports text search across company, role, location, source, state, notes, and saved job descriptions.

### Pipeline stage vs. current state

The project deliberately separates **where an application has reached in the hiring process** from **what is happening with it right now**.

Pipeline stages include:

- Saved
- Applied
- Recruiter Screen
- Hiring Manager
- Technical Interview
- Final Round
- Offer
- Rejected
- Withdrawn
- No Response

Current states include:

- Active
- Interview Scheduled
- Awaiting Feedback
- Follow-up Due
- On Hold
- Closed

For example, an application can remain at `Technical Interview` while its current state is `Awaiting Feedback`.

### Application lifecycle

Each application has a chronological timeline. Events are grouped into four categories:

- **Pipeline** — applied, recruiter screen, technical interview, final round, offer, etc.
- **Communication** — recruiter outreach, interview scheduling, follow-ups
- **Assessment** — coding assessments and take-home assignments
- **Activity** — miscellaneous job-search activity worth preserving

Timeline events support:

- date and optional time
- event type
- title
- contact / person
- free-form notes
- editing and deletion

Changing an application's pipeline stage automatically records the new milestone in its lifecycle.

### Calendar

The calendar renders dated lifecycle events in a monthly view and distinguishes event categories visually.

It includes:

- month navigation
- today's date highlighting
- application and event labels
- event times when available
- upcoming activity for the next 30 days
- links from calendar entries back to the relevant application lifecycle

### Prep / Notes knowledge base

Interview preparation is stored separately from application notes so it can be reused across opportunities.

Prep types include:

- Technical Topic
- STAR Story
- Interview Question
- Company Research
- General Note

Each prep item can include:

- category
- searchable tags
- full notes / talking points
- confidence from 1–5
- optional application ownership
- reusable links to multiple applications

This allows, for example, one `HashMap vs ConcurrentHashMap` prep item to be linked to multiple Java interviews while company-specific research remains attached to one role.

### Review workflow

Prep items have a lightweight study workflow. A review records:

- the review date
- updated confidence
- total review count

The review queue uses confidence to decide when material should return:

| Confidence | Meaning | Review behavior |
| --- | --- | --- |
| 1 | Need to learn | stays due |
| 2 | Shaky | stays due |
| 3 | Okay | due after 14 days |
| 4 | Strong | due after 30 days |
| 5 | Interview ready | due after 60 days |

The goal is to keep weak material visible without repeatedly surfacing topics that are already interview-ready.

### Analytics

Analytics are calculated entirely from the application's local data.

Current metrics include:

- application count
- response rate
- interview rate
- offers
- average time to first response
- application funnel
- current application-state breakdown
- six-month application / interview activity
- average time from application to key pipeline stages
- prep-library confidence and review health
- interview performance by application source when source data is available

Analytics become more useful as more application history is entered or imported.

## Local-first design and privacy

There is no user account or hosted backend.

By default, SQLite stores the application data in:

```text
jobsearch.db
```

The database is created in the application's working directory (normally the project root when run from IntelliJ).

`jobsearch.db` and its SQLite sidecar files are ignored by Git, so your real job-search data should not be committed to a repository.

For backup, stop the application and copy `jobsearch.db` somewhere safe.

### Safe demo mode

The repository also includes a separate **demo profile** for screenshots, portfolio previews, and public repository evaluation. Demo mode:

- uses `demo-jobsearch.db` instead of `jobsearch.db`
- runs on port `8081` instead of `8080`
- contains only fictional companies, contacts, roles, notes, and prep material
- resets the synthetic dataset every time demo mode starts
- never reads or modifies the private `jobsearch.db`

Both SQLite files are ignored by Git. This means you can keep the private app running on `http://localhost:8080` and launch the demo independently on `http://localhost:8081`.

## Screenshots

> All screenshots use the built-in demo mode with synthetic data. No personal job-search information is included.

### Dashboard

A quick overview of the current pipeline, response metrics, recent applications, and opportunities that need attention.

![Job Search Command Center dashboard](docs/screenshots/dashboard.png)

### Application lifecycle, application details & calendar

| Application lifecycle | Application details | Calendar|
| --- | --- | ---|
| Track each opportunity from application through interviews, follow-ups, and final outcome. | Keep role details, status, notes, job posting, linked prep, and lifecycle history together in one place. | See interviews, assessments, recruiter activity, and follow-ups in monthly view. |
| ![Application lifecycle](docs/screenshots/applications.png) | ![Application details](docs/screenshots/application-detail.png) | ![Calendar](docs/screenshots/calendar.png) |

### Interview prep & analytics

| Prep / Notes | Analytics |
| --- | --- |
| Build a reusable knowledge base of technical topics, STAR stories, company research, and interview questions. | Understand funnel conversion, response rates, pipeline velocity, interview activity, and prep health. |
| ![Prep and Notes](docs/screenshots/prep-notes.png) | ![Analytics](docs/screenshots/analytics.png) |

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Server-rendered UI | Thymeleaf |
| Web | Spring Web MVC |
| Database access | Spring JDBC / `JdbcTemplate` |
| Database | SQLite |
| Validation | Jakarta Validation / Spring Validation |
| Build | Maven |
| Frontend | HTML, CSS, minimal vanilla JavaScript |

The project intentionally avoids a separate SPA/frontend build pipeline. There is no React, Node, npm, or external web server required.

## Architecture

The application follows a familiar Spring layered structure:

```text
Browser
  │
  ▼
Controller
  │
  ▼
Service
  │
  ▼
Repository / JdbcTemplate
  │
  ▼
SQLite (`jobsearch.db`, or isolated `demo-jobsearch.db` in demo mode)
```

The main data tables are:

| Table | Purpose |
| --- | --- |
| `job_applications` | one row per tracked role |
| `application_events` | lifecycle / calendar events |
| `prep_items` | reusable and role-specific prep material |
| `prep_item_links` | many-to-many links between reusable prep and applications |

The schema is created from `src/main/resources/schema.sql`. A small startup migration runner handles columns introduced after the earliest project versions.

## Project structure

```text
src/main/java/com/brianna/jobsearch/
├── config/
│   ├── DatabaseConfig.java
│   └── DemoDataConfig.java
├── controller/
│   ├── AnalyticsController.java
│   ├── AppModeAdvice.java
│   ├── CalendarController.java
│   ├── DashboardController.java
│   ├── JobApplicationController.java
│   └── PrepController.java
├── model/
├── repository/
├── service/
├── JobSearchDashboardApplication.java
└── JobSearchDemoApplication.java

src/main/resources/
├── static/
│   ├── css/app.css
│   └── images/job-search-logo.svg
├── templates/
│   ├── applications/
│   ├── prep/
│   ├── analytics.html
│   ├── calendar.html
│   ├── dashboard.html
│   └── fragments.html
├── application.properties
├── application-demo.properties
├── demo-data.sql
└── schema.sql
```

## Getting started

### Prerequisites

You need:

- **Java 21**
- **IntelliJ IDEA** with Maven support, or a local Maven installation

No Node/npm tooling is required.

### IntelliJ IDEA — recommended

1. Clone or download the repository.
2. Open the repository root in IntelliJ.
3. Allow IntelliJ to import the Maven project.
4. Open:

   ```text
   src/main/java/com/brianna/jobsearch/JobSearchDashboardApplication.java
   ```

5. Run the `main()` method, or create an **Application** / **Spring Boot** run configuration using:

   ```text
   com.brianna.jobsearch.JobSearchDashboardApplication
   ```

6. Open:

   ```text
   http://localhost:8080
   ```

A convenient IntelliJ run configuration can be named `Job Search Dashboard` so future starts are just one click on the green Run button.

### Demo mode — recommended for screenshots

For a public-safe version of the app, run:

```text
com.brianna.jobsearch.JobSearchDemoApplication
```

In IntelliJ, create a second **Application** run configuration named `Job Search Demo` using that main class. Then open:

```text
http://localhost:8081
```

The top bar and sidebar explicitly show **Demo mode** so it is easy to tell which environment is open. The demo database is rebuilt from `src/main/resources/demo-data.sql` on every launch.

The generated dataset includes:

- 12 fictional applications across different pipeline stages and states
- recruiter outreach, assessments, interviews, follow-ups, final rounds, and an offer
- past and upcoming calendar activity
- multiple application sources for source-performance analytics
- reusable technical prep, STAR stories, company research, and review-due items

Because the demo profile uses a separate database and port, it is safe to use for README screenshots without exposing the contents of your personal job search.

### Maven command line

If Maven is installed globally:

```bash
mvn spring-boot:run
```

Run the sanitized demo profile with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

The private app opens on port `8080`; the demo opens on port `8081`.

To create a packaged JAR:

```bash
mvn clean package
java -jar target/job-search-dashboard-1.0.0.jar
```

> The repository does not currently include the Maven Wrapper (`mvnw` / `mvnw.cmd`). Adding it is tracked in `NEXT-STEPS.md`.

## First-use workflow

A useful first pass through the app is:

1. Add an application.
2. Record recruiter outreach, assessments, interviews, and follow-ups on its lifecycle.
3. Set the current application state independently from the pipeline stage.
4. Add company-specific research under **Prep / Notes**.
5. Add reusable technical topics and STAR stories.
6. Link relevant reusable prep to the application.
7. Use **Review** to update confidence as interview prep improves.
8. Use **Calendar** for dated activity and **Analytics** for funnel / timing trends.

## Development notes

### Template and static-resource changes

Thymeleaf caching is disabled for local development:

```properties
spring.thymeleaf.cache=false
```

If a change still appears stale:

1. stop the application
2. run Maven `clean` and `package` from IntelliJ's Maven tool window
3. restart
4. hard-refresh the browser (`Ctrl+Shift+R` on Windows)

### Demo-data changes

`demo-data.sql` should contain **only synthetic data**. Avoid copying values from the private database into the demo seed, even temporarily. The demo launcher intentionally reseeds the demo database each time it starts so screenshots are repeatable and local demo edits are disposable.

When adding a feature that depends on structured data, update the demo seed when useful so public screenshots continue to exercise the feature.

### Database changes

Because this is a local-first project, schema changes should be backward-safe whenever possible. `schema.sql` uses `CREATE TABLE IF NOT EXISTS`, and `DatabaseConfig` contains lightweight startup migrations for older local databases.

When changing the schema, always test against both:

- a new empty database
- an existing database containing application / timeline / prep data

## Publishing this project to GitHub

The repository is designed so local personal data stays out of source control. Before the first public push:

1. Confirm `jobsearch.db` is not staged.
2. Do not commit screenshots containing real recruiter names, interview details, salary information, or other personal job-search data unless intentionally sanitized.
3. Choose a license if the repository will be public.
4. Launch `JobSearchDemoApplication` and capture screenshots from `http://localhost:8081` rather than using the private database.
5. Confirm `demo-jobsearch.db` is also untracked; it is generated locally and should not be committed.

Typical first-repository commands:

```bash
git init
git add .
git commit -m "Release v1.0.0"
git branch -M main
git remote add origin <your-repository-url>
git push -u origin main
```

## Current limitations

Version 1.0.0 is intentionally a local, single-user application. It includes a public-safe demo profile, but does **not** currently include:

- authentication or multi-user support
- cloud hosting / cloud database
- email parsing
- Google Calendar synchronization
- automatic job-posting imports
- global cross-app search
- CSV import / export UI
- contacts / recruiter CRM
- dark mode

Those and other ideas are tracked in [`NEXT-STEPS.md`](NEXT-STEPS.md).

## Roadmap

See [`NEXT-STEPS.md`](NEXT-STEPS.md) for the prioritized product, analytics, automation, UX, and engineering backlog.

## Version

Current release: **1.0.0**

This release consolidates the original iterative v1–v9 development work into the first GitHub-ready project baseline, including an isolated synthetic demo profile for public screenshots.
