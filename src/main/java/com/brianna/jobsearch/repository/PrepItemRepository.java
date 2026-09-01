package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.PrepItem;
import com.brianna.jobsearch.model.PrepItemType;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PrepItemRepository {

    private static final String REVIEW_DUE_CONDITION = """
            (
                p.confidence <= 2
                OR (p.confidence = 3 AND datetime(COALESCE(p.last_reviewed_at, p.updated_at)) <= datetime('now', '-14 days'))
                OR (p.confidence = 4 AND datetime(COALESCE(p.last_reviewed_at, p.updated_at)) <= datetime('now', '-30 days'))
                OR (p.confidence >= 5 AND datetime(COALESCE(p.last_reviewed_at, p.updated_at)) <= datetime('now', '-60 days'))
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PrepItem> rowMapper = (rs, rowNum) -> {
        PrepItem item = new PrepItem();
        item.setId(rs.getLong("id"));
        item.setType(PrepItemType.valueOf(rs.getString("item_type")));
        item.setTitle(rs.getString("title"));
        item.setCategory(rs.getString("category"));
        item.setTags(rs.getString("tags"));
        item.setContent(rs.getString("content"));
        item.setConfidence(rs.getInt("confidence"));

        long applicationId = rs.getLong("application_id");
        item.setApplicationId(rs.wasNull() ? null : applicationId);
        item.setApplicationCompany(rs.getString("application_company"));
        item.setApplicationRole(rs.getString("application_role"));

        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");
        String lastReviewedAt = rs.getString("last_reviewed_at");
        item.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt));
        item.setUpdatedAt(updatedAt == null ? null : LocalDateTime.parse(updatedAt));
        item.setLastReviewedAt(lastReviewedAt == null || lastReviewedAt.isBlank() ? null : LocalDateTime.parse(lastReviewedAt));
        item.setReviewCount(rs.getInt("review_count"));
        return item;
    };

    public PrepItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PrepItem> findAll(String query, PrepItemType type, Long applicationId) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.*,
                       ja.company AS application_company,
                       ja.role AS application_role
                FROM prep_items p
                LEFT JOIN job_applications ja ON ja.id = p.application_id
                WHERE 1 = 1
                """);
        List<Object> args = new java.util.ArrayList<>();

        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim() + "%";
            sql.append("""
                     AND (
                         LOWER(p.title) LIKE LOWER(?)
                         OR LOWER(COALESCE(p.category, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(p.tags, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(p.content, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(ja.company, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(ja.role, '')) LIKE LOWER(?)
                     )
                    """);
            for (int i = 0; i < 6; i++) {
                args.add(like);
            }
        }

        if (type != null) {
            sql.append(" AND p.item_type = ?\n");
            args.add(type.name());
        }

        if (applicationId != null) {
            sql.append("""
                     AND (
                         p.application_id = ?
                         OR EXISTS (
                             SELECT 1
                             FROM prep_item_links pil
                             WHERE pil.prep_item_id = p.id
                               AND pil.application_id = ?
                         )
                     )
                    """);
            args.add(applicationId);
            args.add(applicationId);
        }

        sql.append(" ORDER BY p.updated_at DESC");
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }

    public Optional<PrepItem> findById(long id) {
        return jdbcTemplate.query("""
                SELECT p.*,
                       ja.company AS application_company,
                       ja.role AS application_role
                FROM prep_items p
                LEFT JOIN job_applications ja ON ja.id = p.application_id
                WHERE p.id = ?
                """, rowMapper, id).stream().findFirst();
    }

    public long save(PrepItem item) {
        String now = LocalDateTime.now().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO prep_items (
                        item_type, title, category, tags, content, confidence,
                        application_id, last_reviewed_at, review_count, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, item.getType().name());
            statement.setString(2, item.getTitle().trim());
            statement.setString(3, blankToNull(item.getCategory()));
            statement.setString(4, blankToNull(item.getTags()));
            statement.setString(5, blankToNull(item.getContent()));
            statement.setInt(6, item.getSafeConfidence());
            if (item.getApplicationId() == null) {
                statement.setObject(7, null);
            } else {
                statement.setLong(7, item.getApplicationId());
            }
            statement.setObject(8, null);
            statement.setInt(9, 0);
            statement.setString(10, now);
            statement.setString(11, now);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("SQLite did not return the new prep item id.");
        }
        return key.longValue();
    }

    public void update(PrepItem item) {
        jdbcTemplate.update("""
                UPDATE prep_items
                SET item_type = ?,
                    title = ?,
                    category = ?,
                    tags = ?,
                    content = ?,
                    confidence = ?,
                    application_id = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                item.getType().name(),
                item.getTitle().trim(),
                blankToNull(item.getCategory()),
                blankToNull(item.getTags()),
                blankToNull(item.getContent()),
                item.getSafeConfidence(),
                item.getApplicationId(),
                LocalDateTime.now().toString(),
                item.getId());
    }

    public void markReviewed(long id, int confidence) {
        jdbcTemplate.update("""
                UPDATE prep_items
                SET confidence = ?,
                    last_reviewed_at = ?,
                    review_count = COALESCE(review_count, 0) + 1
                WHERE id = ?
                """, Math.max(1, Math.min(5, confidence)), LocalDateTime.now().toString(), id);
    }

    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM prep_item_links WHERE prep_item_id = ?", id);
        jdbcTemplate.update("DELETE FROM prep_items WHERE id = ?", id);
    }

    public void clearLinksForPrepItem(long prepItemId) {
        jdbcTemplate.update("DELETE FROM prep_item_links WHERE prep_item_id = ?", prepItemId);
    }

    public long countAll() {
        return valueOrZero(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prep_items", Long.class));
    }

    public long countByType(PrepItemType type) {
        return valueOrZero(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prep_items WHERE item_type = ?", Long.class, type.name()));
    }

    public long countLinked() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM prep_items p
                WHERE p.application_id IS NOT NULL
                   OR EXISTS (
                       SELECT 1
                       FROM prep_item_links pil
                       WHERE pil.prep_item_id = p.id
                   )
                """, Long.class));
    }

    public long countForApplication(long applicationId) {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT p.id)
                FROM prep_items p
                WHERE p.application_id = ?
                   OR EXISTS (
                       SELECT 1
                       FROM prep_item_links pil
                       WHERE pil.prep_item_id = p.id
                         AND pil.application_id = ?
                   )
                """, Long.class, applicationId, applicationId));
    }

    public List<PrepItem> findForApplication(long applicationId) {
        return jdbcTemplate.query("""
                SELECT p.*,
                       ja.company AS application_company,
                       ja.role AS application_role
                FROM prep_items p
                LEFT JOIN job_applications ja ON ja.id = p.application_id
                WHERE p.application_id = ?
                   OR EXISTS (
                       SELECT 1
                       FROM prep_item_links pil
                       WHERE pil.prep_item_id = p.id
                         AND pil.application_id = ?
                   )
                ORDER BY
                    CASE WHEN p.application_id = ? THEN 0 ELSE 1 END,
                    p.updated_at DESC
                """, rowMapper, applicationId, applicationId, applicationId);
    }

    public List<PrepItem> findLinkableReusable(long applicationId) {
        return jdbcTemplate.query("""
                SELECT p.*,
                       ja.company AS application_company,
                       ja.role AS application_role
                FROM prep_items p
                LEFT JOIN job_applications ja ON ja.id = p.application_id
                WHERE p.application_id IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM prep_item_links pil
                      WHERE pil.prep_item_id = p.id
                        AND pil.application_id = ?
                  )
                ORDER BY p.updated_at DESC, p.title ASC
                """, rowMapper, applicationId);
    }

    public void linkToApplication(long prepItemId, long applicationId) {
        jdbcTemplate.update("""
                INSERT OR IGNORE INTO prep_item_links (prep_item_id, application_id, created_at)
                VALUES (?, ?, ?)
                """, prepItemId, applicationId, LocalDateTime.now().toString());
    }

    public void unlinkFromApplication(long prepItemId, long applicationId) {
        jdbcTemplate.update("""
                DELETE FROM prep_item_links
                WHERE prep_item_id = ?
                  AND application_id = ?
                """, prepItemId, applicationId);
    }

    public long countNeedsReview() {
        return valueOrZero(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prep_items p WHERE " + REVIEW_DUE_CONDITION, Long.class));
    }

    public List<PrepItem> findNeedsReview(int limit) {
        return jdbcTemplate.query("""
                SELECT p.*,
                       ja.company AS application_company,
                       ja.role AS application_role
                FROM prep_items p
                LEFT JOIN job_applications ja ON ja.id = p.application_id
                WHERE """ + REVIEW_DUE_CONDITION + """
                ORDER BY
                    CASE WHEN p.confidence <= 2 THEN 0 ELSE 1 END,
                    p.confidence ASC,
                    datetime(COALESCE(p.last_reviewed_at, p.updated_at)) ASC
                LIMIT ?
                """, rowMapper, limit);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
