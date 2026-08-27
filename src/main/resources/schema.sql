CREATE TABLE IF NOT EXISTS job_applications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company TEXT NOT NULL,
    role TEXT NOT NULL,
    location TEXT,
    status TEXT NOT NULL,
    state TEXT NOT NULL DEFAULT 'ACTIVE',
    priority TEXT NOT NULL,
    source TEXT,
    job_url TEXT,
    salary TEXT,
    applied_date TEXT,
    notes TEXT,
    job_description TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_job_applications_status
    ON job_applications(status);


CREATE INDEX IF NOT EXISTS idx_job_applications_updated_at
    ON job_applications(updated_at);

CREATE INDEX IF NOT EXISTS idx_job_applications_company
    ON job_applications(company);

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

-- Backfill an Applied/Saved lifecycle starting point for applications created before
-- the timeline feature existed. The NOT EXISTS clauses keep this safe on every startup.
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

-- Existing applications may already be several stages into the process. Add their
-- current stage once so the first timeline is immediately useful.
INSERT INTO application_events (
    application_id, event_type, title, event_date, event_time,
    contact_name, notes, created_at
)
SELECT
    ja.id,
    ja.status,
    NULL,
    substr(ja.updated_at, 1, 10),
    NULL,
    NULL,
    NULL,
    ja.updated_at
FROM job_applications ja
WHERE ja.status NOT IN ('SAVED', 'APPLIED')
  AND NOT EXISTS (
      SELECT 1
      FROM application_events ae
      WHERE ae.application_id = ja.id
        AND ae.event_type = ja.status
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

-- Existing application-specific prep items should immediately appear on their
-- application's prep panel. INSERT OR IGNORE keeps this safe on every startup.
INSERT OR IGNORE INTO prep_item_links (prep_item_id, application_id, created_at)
SELECT id, application_id, created_at
FROM prep_items
WHERE application_id IS NOT NULL;
