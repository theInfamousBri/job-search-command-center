CREATE TABLE IF NOT EXISTS job_applications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company TEXT NOT NULL,
    company_domain TEXT,
    role TEXT NOT NULL,
    location TEXT,
    work_arrangement TEXT,
    years_experience_required TEXT,
    career_lane TEXT,
    status TEXT NOT NULL,
    state TEXT NOT NULL DEFAULT 'ACTIVE',
    priority TEXT NOT NULL,
    source TEXT,
    job_url TEXT,
    salary TEXT,
    applied_date TEXT,
    next_step TEXT,
    cover_letter INTEGER,
    cover_letter_text TEXT,
    notes TEXT,
    job_description TEXT,
    import_source TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_applications_status
    ON job_applications(status);
CREATE INDEX IF NOT EXISTS idx_job_applications_state
    ON job_applications(state);
CREATE INDEX IF NOT EXISTS idx_job_applications_updated_at
    ON job_applications(updated_at);
CREATE INDEX IF NOT EXISTS idx_job_applications_company
    ON job_applications(company);


CREATE TABLE IF NOT EXISTS company_logos (
    domain TEXT PRIMARY KEY,
    mime_type TEXT NOT NULL,
    image_data BLOB NOT NULL,
    source_url TEXT,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS application_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    title TEXT,
    event_date TEXT NOT NULL,
    event_time TEXT,
    contact_name TEXT,
    notes TEXT,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_application_events_application_id
    ON application_events(application_id);
CREATE INDEX IF NOT EXISTS idx_application_events_event_date
    ON application_events(event_date);

-- Backfill only the lifecycle starting point for legacy applications that predate
-- timeline support. Imported rows already receive explicit historical events.
INSERT INTO application_events (
    application_id, event_type, title, event_date, event_time,
    contact_name, notes, created_at
)
SELECT
    ja.id,
    CASE WHEN ja.status = 'SAVED' THEN 'SAVED' ELSE 'APPLIED' END,
    NULL,
    CASE
        WHEN ja.status = 'SAVED' THEN substr(ja.created_at, 1, 10)
        ELSE COALESCE(ja.applied_date, substr(ja.created_at, 1, 10))
    END,
    NULL,
    NULL,
    NULL,
    ja.created_at
FROM job_applications ja
WHERE NOT EXISTS (
    SELECT 1
    FROM application_events ae
    WHERE ae.application_id = ja.id
      AND ae.event_type = CASE WHEN ja.status = 'SAVED' THEN 'SAVED' ELSE 'APPLIED' END
);

CREATE TABLE IF NOT EXISTS prep_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_type TEXT NOT NULL,
    title TEXT NOT NULL,
    category TEXT,
    tags TEXT,
    content TEXT,
    confidence INTEGER NOT NULL DEFAULT 3,
    application_id INTEGER,
    last_reviewed_at TEXT,
    review_count INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_prep_items_type
    ON prep_items(item_type);
CREATE INDEX IF NOT EXISTS idx_prep_items_application_id
    ON prep_items(application_id);
CREATE INDEX IF NOT EXISTS idx_prep_items_confidence
    ON prep_items(confidence);

CREATE TABLE IF NOT EXISTS prep_item_links (
    prep_item_id INTEGER NOT NULL,
    application_id INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY (prep_item_id, application_id)
);

CREATE INDEX IF NOT EXISTS idx_prep_item_links_application_id
    ON prep_item_links(application_id);
CREATE INDEX IF NOT EXISTS idx_prep_item_links_prep_item_id
    ON prep_item_links(prep_item_id);

-- Test fixture data used by DatabaseMigrationTest.
INSERT INTO job_applications (
    id, company, company_domain, role, location, work_arrangement, years_experience_required,
    career_lane, status, state, priority, source, job_url, salary, applied_date, next_step,
    cover_letter, cover_letter_text, notes, job_description, import_source, created_at, updated_at
) VALUES (
    1, 'Legacy Co', 'legacy.example', 'Backend Engineer', 'Denver, CO', 'Hybrid', '5+',
    'Backend / Payments', 'REJECTED', 'ACTIVE', 'HIGH', 'LinkedIn', NULL, '$150k-$180k', NULL, NULL,
    0, NULL, 'Legacy notes', 'Legacy job description', NULL,
    '2026-01-01T09:00:00', '2026-02-03T11:22:33'
);

INSERT INTO application_events (
    application_id, event_type, title, event_date, event_time, contact_name, notes, created_at
) VALUES (
    1, 'APPLIED', NULL, '2026-01-15', NULL, NULL, NULL, '2026-01-15T09:00:00'
);
