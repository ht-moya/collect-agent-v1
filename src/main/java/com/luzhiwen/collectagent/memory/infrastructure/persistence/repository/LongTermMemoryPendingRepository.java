package com.luzhiwen.collectagent.memory.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.memory.domain.LongTermMemoryPendingRecord;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 待人工确认长期记忆 JDBC 仓储。
 */
@Repository
public class LongTermMemoryPendingRepository {

    /** JDBC 模板。 */
    private final JdbcTemplate jdbcTemplate;

    /** 长期记忆仓储，复用 JSON 工具。 */
    private final LongTermMemoryRepository longTermMemoryRepository;

    /**
     * 创建 pending 仓储。
     *
     * @param jdbcTemplate JDBC 模板
     * @param longTermMemoryRepository 长期记忆仓储
     */
    public LongTermMemoryPendingRepository(JdbcTemplate jdbcTemplate, LongTermMemoryRepository longTermMemoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.longTermMemoryRepository = longTermMemoryRepository;
    }

    /**
     * 保存低置信度候选。
     *
     * @param record 待审核记录
     */
    public void upsertPending(LongTermMemoryPendingRecord record) {
        Optional<LongTermMemoryPendingRecord> existing = findByDedup(
                record.getTenantId(), record.getUserId(), record.getMemoryType(), record.getContentHash());
        if (existing.isPresent()) {
            LongTermMemoryPendingRecord old = existing.get();
            double confidence = Math.max(old.getConfidence(), record.getConfidence());
            jdbcTemplate.update("""
                    UPDATE agent_memory_pending
                    SET source_event_ids=CAST(? AS JSON), structured=CAST(? AS JSON), confidence=?, updated_at=NOW(6)
                    WHERE id=?
                    """, longTermMemoryRepository.writeJson(record.getSourceEventIds()),
                    longTermMemoryRepository.writeJson(record.getStructured()), confidence, old.getId());
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO agent_memory_pending (
                    tenant_id, user_id, scope, memory_type, content, content_hash,
                    structured, source_event_ids, confidence, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?, 'pending', NOW(6), NOW(6))
                """, record.getTenantId(), record.getUserId(), record.getScope(),
                record.getMemoryType().getValue(), record.getContent(), record.getContentHash(),
                longTermMemoryRepository.writeJson(record.getStructured()),
                longTermMemoryRepository.writeJson(record.getSourceEventIds()), record.getConfidence());
    }

    /**
     * 查询指定状态 pending。
     *
     * @param status 状态
     * @param limit 数量
     * @return 列表
     */
    public List<LongTermMemoryPendingRecord> listByStatus(String status, int limit) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, reviewed_at,
                       reviewer, review_note, created_at
                FROM agent_memory_pending WHERE status=? ORDER BY created_at ASC LIMIT ?
                """, rowMapper(), status, limit);
    }

    /**
     * 按主键查询 pending。
     *
     * @param id 主键
     * @return pending
     */
    public Optional<LongTermMemoryPendingRecord> findById(long id) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, reviewed_at,
                       reviewer, review_note, created_at
                FROM agent_memory_pending WHERE id=?
                """, rowMapper(), id).stream().findFirst();
    }

    /** @param id 主键 @param reviewer 审核人 @param note 说明。 */
    public void markApproved(long id, String reviewer, String note) { updateReviewStatus(id, "approved", reviewer, note); }

    /** @param id 主键 @param reviewer 审核人 @param note 说明。 */
    public void markRejected(long id, String reviewer, String note) { updateReviewStatus(id, "rejected", reviewer, note); }

    /**
     * 更新审核状态。
     *
     * @param id 主键
     * @param status 状态
     * @param reviewer 审核人
     * @param note 说明
     */
    private void updateReviewStatus(long id, String status, String reviewer, String note) {
        jdbcTemplate.update("""
                UPDATE agent_memory_pending
                SET status=?, reviewer=?, review_note=?, reviewed_at=NOW(6), updated_at=NOW(6)
                WHERE id=? AND status='pending'
                """, status, reviewer, note, id);
    }

    /**
     * 按唯一键查询 pending。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param type 类型
     * @param hash 哈希
     * @return pending
     */
    private Optional<LongTermMemoryPendingRecord> findByDedup(
            String tenantId, String userId, LongTermMemoryType type, String hash
    ) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, reviewed_at,
                       reviewer, review_note, created_at
                FROM agent_memory_pending
                WHERE tenant_id=? AND user_id=? AND memory_type=? AND content_hash=? LIMIT 1
                """, rowMapper(), tenantId, userId, type.getValue(), hash).stream().findFirst();
    }

    /**
     * pending 行映射器。
     *
     * @return RowMapper
     */
    private RowMapper<LongTermMemoryPendingRecord> rowMapper() {
        return (resultSet, rowNum) -> {
            LongTermMemoryPendingRecord record = new LongTermMemoryPendingRecord();
            record.setId(resultSet.getLong("id"));
            record.setTenantId(resultSet.getString("tenant_id"));
            record.setUserId(resultSet.getString("user_id"));
            record.setScope(resultSet.getString("scope"));
            record.setMemoryType(LongTermMemoryType.fromValue(resultSet.getString("memory_type")));
            record.setContent(resultSet.getString("content"));
            record.setContentHash(resultSet.getString("content_hash"));
            record.setStructured(longTermMemoryRepository.readMap(resultSet.getString("structured")));
            record.setSourceEventIds(readStringList(resultSet.getString("source_event_ids")));
            record.setConfidence(resultSet.getDouble("confidence"));
            record.setStatus(resultSet.getString("status"));
            record.setReviewedAt(toLocalDateTime(resultSet.getTimestamp("reviewed_at")));
            record.setReviewer(resultSet.getString("reviewer"));
            record.setReviewNote(resultSet.getString("review_note"));
            record.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
            return record;
        };
    }

    /**
     * 解析来源事件ID数组。
     *
     * @param json JSON
     * @return 列表
     */
    private List<String> readStringList(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            for (var item : longTermMemoryRepository.getObjectMapper().readTree(json)) {
                result.add(item.asString());
            }
        } catch (Exception ignored) {
            // 损坏来源按空处理。
        }
        return result;
    }

    /**
     * 可空时间转换。
     *
     * @param timestamp SQL 时间
     * @return LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
