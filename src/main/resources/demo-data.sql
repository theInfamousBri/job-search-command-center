-- Sanitized demo data for screenshots, public demos, and repository evaluation.
-- This script only runs with the Spring "demo" profile, whose datasource is
-- demo-jobsearch.db. It intentionally resets the demo database on every launch.

DELETE FROM prep_item_links;
DELETE FROM prep_items;
DELETE FROM application_events;
DELETE FROM company_logos;
DELETE FROM job_applications;
DELETE FROM sqlite_sequence
WHERE name IN ('job_applications', 'application_events', 'prep_items');

-- ---------------------------------------------------------------------------
-- Applications
-- ---------------------------------------------------------------------------
INSERT INTO job_applications (
    id, company, role, location, status, state, priority, source, job_url,
    salary, applied_date, notes, job_description, created_at, updated_at
) VALUES
(1, 'Northstar Labs', 'Senior Backend Engineer', 'Denver, CO · Hybrid', 'TECHNICAL_INTERVIEW', 'AWAITING_FEEDBACK', 'HIGH', 'Referral',
 'https://example.com/jobs/northstar-backend', '$150,000 – $175,000', date('now','localtime','-24 days'),
 'Strong backend fit. The team emphasized service reliability, API ownership, and pragmatic system design.',
 'Build and operate Java/Spring services that support high-volume customer workflows. Partner with platform and product teams on reliability, observability, API design, and production readiness.',
 strftime('%Y-%m-%dT09:00:00','now','localtime','-24 days'), strftime('%Y-%m-%dT16:20:00','now','localtime','-1 day')),

(2, 'Atlas Payments', 'Software Engineer III', 'Remote (US)', 'FINAL_ROUND', 'INTERVIEW_SCHEDULED', 'HIGH', 'LinkedIn',
 'https://example.com/jobs/atlas-payments', '$160,000 – $185,000', date('now','localtime','-18 days'),
 'Payments platform role with a strong distributed-systems focus. Final panel is scheduled.',
 'Design backend services for payment orchestration, transaction processing, and risk controls. The role focuses on Java, APIs, distributed systems, and operational excellence.',
 strftime('%Y-%m-%dT10:15:00','now','localtime','-18 days'), strftime('%Y-%m-%dT11:30:00','now','localtime')),

(3, 'Cedar Cloud', 'Platform Engineer', 'Boulder, CO · Hybrid', 'RECRUITER_SCREEN', 'INTERVIEW_SCHEDULED', 'MEDIUM', 'Company Site',
 'https://example.com/jobs/cedar-cloud', '$140,000 – $165,000', date('now','localtime','-7 days'),
 'Interesting internal-platform role. Recruiter screen is scheduled for tomorrow.',
 'Help build internal developer tooling, deployment workflows, service templates, and observability standards for product engineering teams.',
 strftime('%Y-%m-%dT13:10:00','now','localtime','-7 days'), strftime('%Y-%m-%dT09:45:00','now','localtime')),

(4, 'Juniper Systems', 'Senior Java Engineer', 'Denver, CO', 'APPLIED', 'ACTIVE', 'HIGH', 'Recruiter',
 'https://example.com/jobs/juniper-java', '$145,000 – $170,000', date('now','localtime','-3 days'),
 'Recruiter-submitted role. Waiting for the hiring team to review the profile.',
 'Own Java services, database integrations, and production support for a growing B2B platform.',
 strftime('%Y-%m-%dT08:30:00','now','localtime','-3 days'), strftime('%Y-%m-%dT08:30:00','now','localtime','-3 days')),

(5, 'Orbit Health', 'Backend Software Engineer', 'Remote (US)', 'REJECTED', 'CLOSED', 'MEDIUM', 'LinkedIn',
 'https://example.com/jobs/orbit-health', '$135,000 – $160,000', date('now','localtime','-75 days'),
 'Reached the technical round. Team chose another candidate after the interview.',
 'Backend engineering role supporting patient-facing workflows, integrations, and operational tooling.',
 strftime('%Y-%m-%dT12:00:00','now','localtime','-75 days'), strftime('%Y-%m-%dT15:40:00','now','localtime','-55 days')),

(6, 'Summit Commerce', 'Staff Software Engineer', 'Denver, CO · Hybrid', 'OFFER', 'ACTIVE', 'HIGH', 'Referral',
 'https://example.com/jobs/summit-commerce', '$175,000 – $205,000', date('now','localtime','-48 days'),
 'Offer received after a strong final round. Comparing scope, team fit, and total compensation.',
 'Lead backend architecture for order, inventory, and fulfillment systems while mentoring engineers and improving platform reliability.',
 strftime('%Y-%m-%dT09:20:00','now','localtime','-48 days'), strftime('%Y-%m-%dT14:10:00','now','localtime','-2 days')),

(7, 'Beacon Financial', 'Software Engineer', 'Remote (US)', 'NO_RESPONSE', 'CLOSED', 'LOW', 'Company Site',
 'https://example.com/jobs/beacon-financial', '$125,000 – $150,000', date('now','localtime','-95 days'),
 'Application closed after no response.',
 'Build backend services and data integrations for financial planning workflows.',
 strftime('%Y-%m-%dT11:00:00','now','localtime','-95 days'), strftime('%Y-%m-%dT11:00:00','now','localtime','-65 days')),

(8, 'Pinecone Software', 'Java Platform Engineer', 'Fort Collins, CO · Hybrid', 'HIRING_MANAGER', 'AWAITING_FEEDBACK', 'MEDIUM', 'LinkedIn',
 'https://example.com/jobs/pinecone-platform', '$145,000 – $168,000', date('now','localtime','-32 days'),
 'Hiring-manager conversation went well. Waiting longer than expected for next steps.',
 'Build platform services, shared Java libraries, and deployment standards used across multiple product teams.',
 strftime('%Y-%m-%dT10:00:00','now','localtime','-32 days'), strftime('%Y-%m-%dT10:00:00','now','localtime','-9 days')),

(9, 'Lumen Mobility', 'Backend Engineer', 'Austin, TX · Hybrid', 'WITHDRAWN', 'CLOSED', 'LOW', 'Indeed',
 'https://example.com/jobs/lumen-mobility', '$130,000 – $155,000', date('now','localtime','-130 days'),
 'Withdrew after learning the required relocation timeline would not be a fit.',
 'Backend services for fleet telemetry, customer APIs, and operational workflows.',
 strftime('%Y-%m-%dT14:00:00','now','localtime','-130 days'), strftime('%Y-%m-%dT09:00:00','now','localtime','-118 days')),

(10, 'Harbor Data', 'Senior Software Engineer', 'Remote (US)', 'TECHNICAL_INTERVIEW', 'INTERVIEW_SCHEDULED', 'HIGH', 'Company Site',
 'https://example.com/jobs/harbor-data', '$155,000 – $182,000', date('now','localtime','-12 days'),
 'Technical interview is scheduled. Reviewing concurrency, API design, and data consistency tradeoffs.',
 'Build high-throughput data services and APIs with Java, relational databases, caching, and cloud infrastructure.',
 strftime('%Y-%m-%dT09:35:00','now','localtime','-12 days'), strftime('%Y-%m-%dT16:00:00','now','localtime')),

(11, 'Aurora Commerce', 'Software Engineer II', 'Denver, CO', 'RECRUITER_SCREEN', 'FOLLOW_UP_DUE', 'MEDIUM', 'LinkedIn',
 'https://example.com/jobs/aurora-commerce', '$130,000 – $152,000', date('now','localtime','-22 days'),
 'Recruiter screen completed. Follow-up is due after a quiet week.',
 'Develop services for catalog, pricing, and merchant integrations in a Java/Spring environment.',
 strftime('%Y-%m-%dT10:40:00','now','localtime','-22 days'), strftime('%Y-%m-%dT10:40:00','now','localtime','-8 days')),

(12, 'Ridgeway Systems', 'Backend Developer', 'Colorado Springs, CO', 'APPLIED', 'ON_HOLD', 'MEDIUM', 'Company Site',
 'https://example.com/jobs/ridgeway-backend', '$120,000 – $145,000', date('now','localtime','-60 days'),
 'Hiring was paused after the application was submitted. Keeping the role on the radar.',
 'Backend application development, REST APIs, SQL, and production support for enterprise workflows.',
 strftime('%Y-%m-%dT08:00:00','now','localtime','-60 days'), strftime('%Y-%m-%dT08:00:00','now','localtime','-45 days'));

-- v1.1 richer application metadata used by demo screenshots.
UPDATE job_applications SET location = 'Denver, CO', work_arrangement = 'Hybrid', years_experience_required = '4+', career_lane = 'Backend / Platform Reliability', cover_letter = 1,
    cover_letter_text = 'Dear Northstar Labs team,' || char(10) || char(10) ||
        'I am excited to apply for the Senior Backend Engineer role. My background building Java and Spring services aligns closely with the team''s focus on API ownership, reliability, and production readiness. I especially enjoy work where correctness and operational quality matter as much as feature delivery.' || char(10) || char(10) ||
        'I would welcome the opportunity to bring that experience to Northstar Labs while continuing to grow in distributed systems and platform reliability.',
    next_step = 'Awaiting feedback after the technical interview.' WHERE id = 1;
UPDATE job_applications SET location = 'United States', work_arrangement = 'Remote', years_experience_required = '3+', career_lane = 'Backend / Payments Platform', cover_letter = 1,
    cover_letter_text = 'Dear Atlas Payments team,' || char(10) || char(10) ||
        'The Software Engineer III opening stood out because it combines backend engineering with payment orchestration and distributed-systems concerns. I have experience designing Java services, APIs, and reliable request-processing flows, and I am drawn to systems where idempotency, consistency, and observability directly affect customer trust.' || char(10) || char(10) ||
        'I would be excited to contribute that background to Atlas Payments.',
    next_step = 'Final panel is scheduled; review architecture and manager questions.' WHERE id = 2;
UPDATE job_applications SET location = 'Boulder, CO', work_arrangement = 'Hybrid', years_experience_required = '3+', career_lane = 'Developer Platform / Cloud Infrastructure', cover_letter = 0 WHERE id = 3;
UPDATE job_applications SET location = 'Denver, CO', work_arrangement = 'Hybrid', years_experience_required = '5+', career_lane = 'Commerce / Backend Platform', cover_letter = 1 WHERE id = 6;
UPDATE job_applications SET location = 'United States', work_arrangement = 'Remote', years_experience_required = '4+', career_lane = 'Data Platform / Backend Services', cover_letter = 1, next_step = 'Technical interview scheduled; finish linked prep.' WHERE id = 10;


-- v1.2 analytics demo segmentation. These broader labels intentionally create
-- enough repeated categories for the decision-support visuals to be meaningful.
UPDATE job_applications SET career_lane = 'Backend / Platform', work_arrangement = 'Hybrid' WHERE id = 1;
UPDATE job_applications SET career_lane = 'Backend / Platform', work_arrangement = 'Hybrid', priority = 'HIGH' WHERE id = 8;
UPDATE job_applications SET career_lane = 'Backend / Platform', work_arrangement = 'Hybrid', priority = 'HIGH' WHERE id = 4;
UPDATE job_applications SET career_lane = 'Backend / Platform', work_arrangement = 'Remote' WHERE id IN (5, 10);
UPDATE job_applications SET career_lane = 'Backend / Platform', work_arrangement = 'On-site' WHERE id = 12;
UPDATE job_applications SET career_lane = 'Product / Domain Backend', work_arrangement = 'Remote', priority = 'STRETCH' WHERE id = 2;
UPDATE job_applications SET career_lane = 'Product / Domain Backend', work_arrangement = 'Hybrid', priority = 'STRETCH' WHERE id = 6;
UPDATE job_applications SET career_lane = 'Product / Domain Backend', work_arrangement = 'Remote' WHERE id = 7;
UPDATE job_applications SET career_lane = 'Product / Domain Backend', work_arrangement = 'Hybrid' WHERE id IN (9, 11);
UPDATE job_applications SET career_lane = 'Developer Platform / Infrastructure', work_arrangement = 'Hybrid' WHERE id = 3;
UPDATE job_applications SET priority = 'STRETCH' WHERE id = 10;


-- v1.3 normalized career taxonomy. The previous career_lane values remain in place
-- as legacy/original tags so the normalization workflow can demonstrate preservation.
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'ENTERPRISE_SAAS', career_focus = 'Platform reliability, APIs' WHERE id = 1;
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'FINTECH_PAYMENTS', career_focus = 'Payment orchestration, distributed systems' WHERE id = 2;
UPDATE job_applications SET role_family = 'CLOUD_INFRASTRUCTURE', industry_domain = 'DEVELOPER_TOOLS', career_focus = 'Internal developer platform' WHERE id = 3;
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'ENTERPRISE_SAAS', career_focus = 'Java services' WHERE id = 4;
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'HEALTHCARE', career_focus = 'Integrations, patient workflows' WHERE id = 5;
UPDATE job_applications SET role_family = 'PRODUCT_ENGINEERING', industry_domain = 'ECOMMERCE', career_focus = 'Order and fulfillment systems' WHERE id = 6;
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'FINANCIAL_SERVICES', career_focus = 'Financial planning integrations' WHERE id = 7;
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'ENTERPRISE_SAAS', career_focus = 'Shared Java platform services' WHERE id = 8;
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'AUTOMOTIVE', career_focus = 'Fleet telemetry, APIs' WHERE id = 9;
UPDATE job_applications SET role_family = 'BACKEND_PLATFORM', industry_domain = 'DEVELOPER_TOOLS', career_focus = 'High-throughput data services' WHERE id = 10;
-- Leave two demo records untagged so Data Quality visibly demonstrates missing-data workflows.

-- Detailed legacy tags remain intentionally richer than the normalized fields so
-- the Normalization Center can demonstrate review-before-write mapping.
UPDATE job_applications SET career_lane = 'Backend / Enterprise Platform / Trust & Telemetry' WHERE id = 1;
UPDATE job_applications SET career_lane = 'Backend / Debit Processing / Payments Platform' WHERE id = 2;
UPDATE job_applications SET career_lane = 'Cloud Observability / Backend Infrastructure' WHERE id = 3;
UPDATE job_applications SET career_lane = 'Java Backend / Operational SaaS / Platform' WHERE id = 4;
UPDATE job_applications SET career_lane = 'Backend / Healthcare Platform / API & Integration' WHERE id = 5;
UPDATE job_applications SET career_lane = 'Backend / E-Commerce Platform / Distributed Systems' WHERE id = 6;
UPDATE job_applications SET career_lane = 'Backend / Financial Services & Retirement Platform' WHERE id = 7;
UPDATE job_applications SET career_lane = 'Backend / Open Source Distributed Systems Platform' WHERE id = 8;
UPDATE job_applications SET career_lane = 'Automotive / Backend Commerce Platform' WHERE id = 9;
UPDATE job_applications SET career_lane = 'Backend / Data Platform / Distributed Systems' WHERE id = 10;
UPDATE job_applications SET career_lane = 'Full-Stack / Social Commerce / Creator Marketplace' WHERE id = 11;
UPDATE job_applications SET career_lane = 'Java Backend / Operational SaaS / Waste Management Platform' WHERE id = 12;

-- ---------------------------------------------------------------------------
-- Lifecycle / calendar events
-- ---------------------------------------------------------------------------
INSERT INTO application_events (application_id, event_type, title, event_date, event_time, contact_name, notes, created_at) VALUES
(1, 'APPLIED', 'Applied', date('now','localtime','-24 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT09:00:00','now','localtime','-24 days')),
(1, 'RECRUITER_CONTACT', 'Recruiter reached out', date('now','localtime','-20 days'), '10:15', 'Morgan Lee · Recruiter', 'Shared team context and scheduled an intro call.', strftime('%Y-%m-%dT10:15:00','now','localtime','-20 days')),
(1, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','-15 days'), '13:00', 'Morgan Lee · Recruiter', 'Discussed role scope, hybrid schedule, and interview process.', strftime('%Y-%m-%dT13:00:00','now','localtime','-15 days')),
(1, 'CODING_ASSESSMENT', 'Backend coding assessment', date('now','localtime','-11 days'), '18:00', NULL, 'Two implementation problems plus a short API-design exercise.', strftime('%Y-%m-%dT18:00:00','now','localtime','-11 days')),
(1, 'TECHNICAL_INTERVIEW', 'Technical interview', date('now','localtime','-1 day'), '14:30', 'Casey Nguyen · Lead Engineer', 'Java, caching, idempotency, and system-design discussion. Good conversation overall.', strftime('%Y-%m-%dT14:30:00','now','localtime','-1 day')),
(1, 'FOLLOW_UP', 'Follow up if no feedback', date('now','localtime','+4 days'), '09:00', NULL, 'Send a concise check-in if no update arrives before then.', strftime('%Y-%m-%dT09:00:00','now','localtime')),

(2, 'APPLIED', 'Applied', date('now','localtime','-18 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT10:15:00','now','localtime','-18 days')),
(2, 'RECRUITER_CONTACT', 'Recruiter outreach', date('now','localtime','-15 days'), '11:30', 'Jordan Patel · Talent Partner', 'Initial conversation about the payments platform team.', strftime('%Y-%m-%dT11:30:00','now','localtime','-15 days')),
(2, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','-12 days'), '15:00', 'Jordan Patel · Talent Partner', 'Compensation range and interview sequence aligned.', strftime('%Y-%m-%dT15:00:00','now','localtime','-12 days')),
(2, 'TECHNICAL_INTERVIEW', 'Technical panel', date('now','localtime','-7 days'), '13:30', 'Riley Chen · Senior Engineer', 'Discussed distributed transactions, observability, and API reliability.', strftime('%Y-%m-%dT13:30:00','now','localtime','-7 days')),
(2, 'INTERVIEW_SCHEDULED', 'Final panel scheduled', date('now','localtime','+2 days'), '10:00', 'Engineering Panel', 'Architecture, collaboration, and manager conversations.', strftime('%Y-%m-%dT10:00:00','now','localtime')),
(2, 'FINAL_ROUND', 'Final round', date('now','localtime','+2 days'), '10:00', 'Engineering Panel', 'Final interview loop.', strftime('%Y-%m-%dT10:00:00','now','localtime')),

(3, 'APPLIED', 'Applied', date('now','localtime','-7 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT13:10:00','now','localtime','-7 days')),
(3, 'RECRUITER_CONTACT', 'Recruiter reached out', date('now','localtime','-4 days'), '16:15', 'Taylor Brooks · Recruiter', 'Intro call scheduled for the following week.', strftime('%Y-%m-%dT16:15:00','now','localtime','-4 days')),
(3, 'INTERVIEW_SCHEDULED', 'Recruiter screen scheduled', date('now','localtime','+1 day'), '09:30', 'Taylor Brooks · Recruiter', '30-minute introductory conversation.', strftime('%Y-%m-%dT09:30:00','now','localtime')),
(3, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','+1 day'), '09:30', 'Taylor Brooks · Recruiter', 'Upcoming recruiter screen.', strftime('%Y-%m-%dT09:30:00','now','localtime')),

(4, 'APPLIED', 'Applied', date('now','localtime','-3 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT08:30:00','now','localtime','-3 days')),

(5, 'APPLIED', 'Applied', date('now','localtime','-75 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT12:00:00','now','localtime','-75 days')),
(5, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','-68 days'), '12:30', 'Avery Kim · Recruiter', NULL, strftime('%Y-%m-%dT12:30:00','now','localtime','-68 days')),
(5, 'TECHNICAL_INTERVIEW', 'Technical interview', date('now','localtime','-60 days'), '14:00', 'Backend Engineering Team', 'API design and debugging exercise.', strftime('%Y-%m-%dT14:00:00','now','localtime','-60 days')),
(5, 'REJECTED', 'Closed after technical round', date('now','localtime','-55 days'), NULL, NULL, 'Team moved forward with another candidate.', strftime('%Y-%m-%dT15:40:00','now','localtime','-55 days')),

(6, 'APPLIED', 'Applied', date('now','localtime','-48 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT09:20:00','now','localtime','-48 days')),
(6, 'RECRUITER_CONTACT', 'Referral introduction', date('now','localtime','-45 days'), '10:00', 'Sam Rivera · Recruiter', NULL, strftime('%Y-%m-%dT10:00:00','now','localtime','-45 days')),
(6, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','-40 days'), '11:00', 'Sam Rivera · Recruiter', NULL, strftime('%Y-%m-%dT11:00:00','now','localtime','-40 days')),
(6, 'TECHNICAL_INTERVIEW', 'Architecture interview', date('now','localtime','-31 days'), '14:00', 'Platform Team', 'System boundaries, scaling strategy, and incident response.', strftime('%Y-%m-%dT14:00:00','now','localtime','-31 days')),
(6, 'FINAL_ROUND', 'Final round', date('now','localtime','-22 days'), '09:00', 'Engineering Leadership', 'Leadership and cross-team collaboration panel.', strftime('%Y-%m-%dT09:00:00','now','localtime','-22 days')),
(6, 'OFFER', 'Offer received', date('now','localtime','-2 days'), '15:30', 'Sam Rivera · Recruiter', 'Offer package received for review.', strftime('%Y-%m-%dT15:30:00','now','localtime','-2 days')),

(7, 'APPLIED', 'Applied', date('now','localtime','-95 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT11:00:00','now','localtime','-95 days')),
(7, 'NO_RESPONSE', 'Closed · no response', date('now','localtime','-65 days'), NULL, NULL, 'Archived after a month without a response.', strftime('%Y-%m-%dT11:00:00','now','localtime','-65 days')),

(8, 'APPLIED', 'Applied', date('now','localtime','-32 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT10:00:00','now','localtime','-32 days')),
(8, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','-25 days'), '13:30', 'Jamie Foster · Recruiter', NULL, strftime('%Y-%m-%dT13:30:00','now','localtime','-25 days')),
(8, 'HIRING_MANAGER', 'Hiring manager interview', date('now','localtime','-18 days'), '10:30', 'Drew Morgan · Engineering Manager', 'Team ownership and platform-roadmap conversation.', strftime('%Y-%m-%dT10:30:00','now','localtime','-18 days')),

(9, 'APPLIED', 'Applied', date('now','localtime','-130 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT14:00:00','now','localtime','-130 days')),
(9, 'RECRUITER_CONTACT', 'Recruiter outreach', date('now','localtime','-125 days'), '09:30', 'Alex Stone · Recruiter', NULL, strftime('%Y-%m-%dT09:30:00','now','localtime','-125 days')),
(9, 'WITHDRAWN', 'Withdrew from process', date('now','localtime','-118 days'), NULL, NULL, 'Relocation timing was not a fit.', strftime('%Y-%m-%dT09:00:00','now','localtime','-118 days')),

(10, 'APPLIED', 'Applied', date('now','localtime','-12 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT09:35:00','now','localtime','-12 days')),
(10, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','-8 days'), '15:00', 'Cameron Wells · Recruiter', 'Moved directly to a technical round.', strftime('%Y-%m-%dT15:00:00','now','localtime','-8 days')),
(10, 'INTERVIEW_SCHEDULED', 'Technical interview scheduled', date('now','localtime','+5 days'), '13:00', 'Backend Engineering Team', 'Concurrency, APIs, data consistency, and system design.', strftime('%Y-%m-%dT13:00:00','now','localtime')),

(11, 'APPLIED', 'Applied', date('now','localtime','-22 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT10:40:00','now','localtime','-22 days')),
(11, 'RECRUITER_CONTACT', 'Recruiter outreach', date('now','localtime','-18 days'), '11:45', 'Quinn Harper · Recruiter', NULL, strftime('%Y-%m-%dT11:45:00','now','localtime','-18 days')),
(11, 'RECRUITER_SCREEN', 'Recruiter screen', date('now','localtime','-15 days'), '14:00', 'Quinn Harper · Recruiter', 'Good initial fit; waiting for team feedback.', strftime('%Y-%m-%dT14:00:00','now','localtime','-15 days')),
(11, 'FOLLOW_UP', 'Follow up with recruiter', date('now','localtime'), '09:00', 'Quinn Harper · Recruiter', 'Send a quick check-in on next steps.', strftime('%Y-%m-%dT09:00:00','now','localtime')),

(12, 'APPLIED', 'Applied', date('now','localtime','-60 days'), NULL, NULL, NULL, strftime('%Y-%m-%dT08:00:00','now','localtime','-60 days')),
(12, 'OTHER', 'Hiring paused', date('now','localtime','-45 days'), NULL, NULL, 'Company paused the opening but may reopen later.', strftime('%Y-%m-%dT08:00:00','now','localtime','-45 days'));

-- ---------------------------------------------------------------------------
-- Prep / Notes knowledge base
-- ---------------------------------------------------------------------------
INSERT INTO prep_items (
    id, item_type, title, category, tags, content, confidence, application_id,
    last_reviewed_at, review_count, created_at, updated_at
) VALUES
(1, 'TECHNICAL_TOPIC', 'HashMap vs ConcurrentHashMap', 'Java · Collections',
 'java, hashmap, concurrenthashmap, concurrency, collections',
 'Definition:\nHashMap is not thread-safe. ConcurrentHashMap is designed for concurrent access and provides atomic operations such as putIfAbsent and computeIfAbsent.\n\nWhen I would use it:\nUse HashMap for local or single-threaded state. Use ConcurrentHashMap when multiple request-processing threads need shared key/value state.\n\nTradeoffs:\nThread-safe collections do not automatically make a multi-step business operation atomic.',
 4, NULL, strftime('%Y-%m-%dT18:00:00','now','localtime','-5 days'), 3,
 strftime('%Y-%m-%dT10:00:00','now','localtime','-40 days'), strftime('%Y-%m-%dT18:00:00','now','localtime','-5 days')),

(2, 'TECHNICAL_TOPIC', 'Spring Transactions & @Transactional', 'Spring · Data',
 'spring, transactions, transactional, database, rollback',
 'Definition:\n@Transactional defines a transactional boundary around a Spring-managed method or class.\n\nReview:\nPropagation, rollback behavior, checked vs runtime exceptions, proxy-based interception, and why self-invocation can bypass the proxy.',
 2, NULL, NULL, 0,
 strftime('%Y-%m-%dT09:00:00','now','localtime','-20 days'), strftime('%Y-%m-%dT09:00:00','now','localtime','-20 days')),

(3, 'TECHNICAL_TOPIC', 'Idempotency for Request Processing', 'System Design · Reliability',
 'idempotency, request id, retries, caching, database',
 'Goal:\nMake repeated delivery of the same logical request safe.\n\nApproach:\nAccept an idempotency key, persist or cache the outcome, use a unique constraint where appropriate, and return the original result for safe retries.\n\nTradeoffs:\nRetention window, storage cost, race conditions, and what constitutes the same request.',
 4, NULL, strftime('%Y-%m-%dT17:00:00','now','localtime','-35 days'), 2,
 strftime('%Y-%m-%dT12:00:00','now','localtime','-70 days'), strftime('%Y-%m-%dT17:00:00','now','localtime','-35 days')),

(4, 'TECHNICAL_TOPIC', 'Caching Strategy & Invalidation', 'System Design · Performance',
 'cache, redis, ttl, invalidation, performance',
 'Start with the access pattern and correctness requirement. Decide what is safe to cache, choose an ownership model for invalidation, and measure hit rate rather than assuming a cache helps.\n\nCommon patterns: cache-aside, TTL-based expiry, write-through, and event-driven invalidation.',
 3, NULL, strftime('%Y-%m-%dT19:00:00','now','localtime','-8 days'), 1,
 strftime('%Y-%m-%dT12:00:00','now','localtime','-25 days'), strftime('%Y-%m-%dT19:00:00','now','localtime','-8 days')),

(5, 'STAR_STORY', 'Architecture conflict during critical delivery', 'Behavioral · Conflict / Ownership',
 'star, conflict, ownership, communication, architecture, delivery',
 'Situation:\nA delivery was blocked by disagreement over service boundaries and ownership.\n\nTask:\nKeep implementation moving while making sure the decision was technically sound and understood by the teams involved.\n\nAction:\nDocumented the concrete tradeoffs, separated must-have decisions from reversible ones, facilitated a focused technical discussion, and proposed a small validation path.\n\nResult:\nThe group aligned on a path forward and delivery resumed with clearer ownership.\n\nUseful for:\nConflict, influence without authority, ambiguity, ownership.',
 4, NULL, strftime('%Y-%m-%dT20:00:00','now','localtime','-12 days'), 2,
 strftime('%Y-%m-%dT12:00:00','now','localtime','-50 days'), strftime('%Y-%m-%dT20:00:00','now','localtime','-12 days')),

(6, 'STAR_STORY', 'Production incident ownership', 'Behavioral · Reliability / Leadership',
 'star, incident, production, ownership, debugging, communication',
 'Situation:\nA production issue caused elevated errors during a high-traffic period.\n\nAction:\nNarrowed the failure domain, coordinated mitigation, kept stakeholders updated, and followed through on a root-cause review.\n\nResult:\nService recovered quickly and the follow-up changes reduced the chance of recurrence.\n\nUseful for:\nOwnership, pressure, debugging, communication.',
 3, NULL, strftime('%Y-%m-%dT18:30:00','now','localtime','-20 days'), 1,
 strftime('%Y-%m-%dT09:30:00','now','localtime','-60 days'), strftime('%Y-%m-%dT18:30:00','now','localtime','-20 days')),

(7, 'COMPANY_RESEARCH', 'Northstar Labs · Team Research', 'Company / Team Research',
 'northstar, backend, reliability, platform',
 'Team focus:\nBackend services, API ownership, reliability, and developer velocity.\n\nWhy this role:\nStrong overlap with Java/Spring experience and interest in production systems.\n\nQuestions to ask:\n- Which services does the team own end-to-end?\n- What are the biggest reliability challenges today?\n- How are architecture decisions made?\n- What would success look like in the first six months?',
 4, 1, strftime('%Y-%m-%dT19:30:00','now','localtime','-2 days'), 1,
 strftime('%Y-%m-%dT13:00:00','now','localtime','-18 days'), strftime('%Y-%m-%dT19:30:00','now','localtime','-2 days')),

(8, 'COMPANY_RESEARCH', 'Atlas Payments · Team Research', 'Company / Team Research',
 'atlas, payments, distributed systems, transactions',
 'Team focus:\nPayment orchestration, transaction processing, reliability, and operational tooling.\n\nQuestions to ask:\n- How does the team handle duplicate or retried payment requests?\n- What consistency guarantees matter most?\n- What does on-call ownership look like?\n- How is technical strategy split between staff engineers and managers?',
 4, 2, strftime('%Y-%m-%dT18:00:00','now','localtime','-1 day'), 2,
 strftime('%Y-%m-%dT12:00:00','now','localtime','-15 days'), strftime('%Y-%m-%dT18:00:00','now','localtime','-1 day')),

(9, 'INTERVIEW_QUESTION', 'Questions for an Engineering Manager', 'Interview Questions',
 'manager, team, culture, ownership, questions',
 'Questions:\n- What distinguishes engineers who do especially well on this team?\n- What are the most important problems you want this person to solve in the first six months?\n- How does the team balance delivery speed with technical debt?\n- Where do architecture decisions usually happen?\n- How do engineers get feedback and grow?',
 5, NULL, strftime('%Y-%m-%dT17:00:00','now','localtime','-14 days'), 4,
 strftime('%Y-%m-%dT10:00:00','now','localtime','-90 days'), strftime('%Y-%m-%dT17:00:00','now','localtime','-14 days')),

(10, 'TECHNICAL_TOPIC', 'System Design · Rate Limiting', 'System Design · Scalability',
 'system design, rate limiting, token bucket, redis, scalability',
 'Start by defining what is being limited: user, API key, endpoint, tenant, or IP.\n\nAlgorithms:\nToken bucket allows bursts while enforcing an average rate. Sliding-window approaches provide tighter fairness at higher implementation cost.\n\nDistributed concerns:\nAtomic counters, clock windows, Redis/Lua, failure behavior, and whether limits must be globally consistent.',
 3, NULL, strftime('%Y-%m-%dT18:00:00','now','localtime','-18 days'), 1,
 strftime('%Y-%m-%dT10:00:00','now','localtime','-45 days'), strftime('%Y-%m-%dT18:00:00','now','localtime','-18 days'));

-- Reusable prep linked to active demo applications.
INSERT INTO prep_item_links (prep_item_id, application_id, created_at) VALUES
(1, 1, strftime('%Y-%m-%dT12:00:00','now','localtime','-10 days')),
(3, 1, strftime('%Y-%m-%dT12:05:00','now','localtime','-10 days')),
(2, 2, strftime('%Y-%m-%dT13:00:00','now','localtime','-6 days')),
(9, 2, strftime('%Y-%m-%dT13:05:00','now','localtime','-6 days')),
(10, 10, strftime('%Y-%m-%dT13:10:00','now','localtime','-5 days'));
