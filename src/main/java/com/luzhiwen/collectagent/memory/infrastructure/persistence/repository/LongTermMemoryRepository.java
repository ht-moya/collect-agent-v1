package com.luzhiwen.collectagent.memory.infrastructure.persistence.repository;

import com.luzhiwen.collectagent.memory.application.VectorSimilarityUtil;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryRecord;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryStatus;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 长期记忆 MySQL JDBC 仓储，负责 VECTOR、JSON 和 FULLTEXT 操作。
 */
@Repository
public class LongTermMemoryRepository {

    /** JDBC 查询模板。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 应用层向量比较最大候选数。 */
    private static final int MAX_DENSE_CANDIDATES = 5000;

    /**
     * 创建长期记忆仓储。
     *
     * @param jdbcTemplate JDBC 模板
     * @param objectMapper JSON 序列化器
     */
    public LongTermMemoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 新增活跃长期记忆。
     *
     * @param record 记忆记录
     */
    public void insertActive(LongTermMemoryRecord record) {
        jdbcTemplate.update("""
                INSERT INTO agent_long_term_memory (
                    memory_id, tenant_id, user_id, scope, memory_type, content, content_hash,
                    structured, source_event_ids, confidence, status, valid_from,
                    embedding, embedding_model, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?, ?,
                          ?, TO_VECTOR(?), ?, NOW(6), NOW(6))
                """, record.getMemoryId(), record.getTenantId(), record.getUserId(), record.getScope(),
                record.getMemoryType().getValue(), record.getContent(), record.getContentHash(),
                writeJson(record.getStructured()), writeJson(record.getSourceEventIds()),
                record.getConfidence(), LongTermMemoryStatus.ACTIVE,
                Timestamp.valueOf(LocalDateTime.now()), toVectorLiteral(record.getEmbedding()),
                record.getEmbeddingModel());
    }

    /**
     * 按精确哈希查询记忆。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param type 类型
     * @param contentHash 内容哈希
     * @return 记忆
     */
    public Optional<LongTermMemoryRecord> findExact(
            String tenantId, String userId, LongTermMemoryType type, String contentHash
    ) {
        return jdbcTemplate.query("""
                SELECT memory_id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, valid_from, valid_to,
                       last_accessed_at, access_count, embedding_model, created_at, updated_at
                FROM agent_long_term_memory
                WHERE tenant_id=? AND user_id=? AND memory_type=? AND content_hash=? AND status<>'deleted'
                LIMIT 1
                """, baseRowMapper(), tenantId, userId, type.getValue(), contentHash).stream().findFirst();
    }

    /**
     * 按结构化事实键查询当前活跃记忆。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param factKey 结构化事实键
     * @return 同一事实键的活跃记忆
     */
    public List<LongTermMemoryRecord> findActiveFactsByKey(String tenantId, String userId, String factKey) {
        return jdbcTemplate.query("""
                SELECT memory_id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, valid_from, valid_to,
                       last_accessed_at, access_count, embedding_model, created_at, updated_at
                FROM agent_long_term_memory
                WHERE tenant_id=? AND user_id=? AND memory_type='fact' AND status='active'
                  AND (valid_to IS NULL OR valid_to > NOW(6))
                  AND JSON_UNQUOTE(JSON_EXTRACT(structured, '$.fact_key'))=?
                ORDER BY confidence DESC, updated_at DESC
                """, baseRowMapper(), tenantId, userId, factKey);
    }

    /**
     * 查询写入去重稠密候选。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param type 类型
     * @param model 向量模型
     * @return 候选
     */
    public List<LongTermMemoryRecord> findDenseCandidates(
            String tenantId, String userId, LongTermMemoryType type, String model
    ) {
        return jdbcTemplate.query("""
                SELECT memory_id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, valid_from, valid_to,
                       last_accessed_at, access_count, embedding_model, created_at, updated_at,
                       FROM_VECTOR(embedding) AS embedding_text
                FROM agent_long_term_memory
                WHERE tenant_id=? AND user_id=? AND memory_type=? AND status='active'
                  AND (valid_to IS NULL OR valid_to > NOW(6))
                  AND embedding IS NOT NULL AND embedding_model=?
                ORDER BY updated_at DESC LIMIT ?
                """, vectorRowMapper(), tenantId, userId, type.getValue(), model, MAX_DENSE_CANDIDATES);
    }

    /**
     * 查询召回稠密候选。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param model 模型
     * @return 候选
     */
    public List<LongTermMemoryRecord> findDenseRecallCandidates(String tenantId, String userId, String model) {
        return jdbcTemplate.query("""
                SELECT memory_id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, valid_from, valid_to,
                       last_accessed_at, access_count, embedding_model, created_at, updated_at,
                       FROM_VECTOR(embedding) AS embedding_text
                FROM agent_long_term_memory
                WHERE tenant_id=? AND user_id=? AND status='active'
                  AND (valid_to IS NULL OR valid_to > NOW(6))
                  AND embedding IS NOT NULL AND embedding_model=?
                ORDER BY updated_at DESC LIMIT ?
                """, vectorRowMapper(), tenantId, userId, model, MAX_DENSE_CANDIDATES);
    }

    /**
     * 查询写入去重关键词候选。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param type 类型
     * @param query 查询
     * @param limit 数量
     * @return 候选
     */
    public List<LongTermMemoryRecord> findKeywordCandidates(
            String tenantId, String userId, LongTermMemoryType type, String query, int limit
    ) {
        return jdbcTemplate.query("""
                SELECT memory_id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, valid_from, valid_to,
                       last_accessed_at, access_count, embedding_model, created_at, updated_at
                FROM agent_long_term_memory
                WHERE tenant_id=? AND user_id=? AND memory_type=? AND status='active'
                  AND (valid_to IS NULL OR valid_to > NOW(6))
                  AND MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) > 0
                ORDER BY MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) DESC LIMIT ?
                """, baseRowMapper(), tenantId, userId, type.getValue(), query, query, limit);
    }

    /**
     * 查询召回关键词候选。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param query 查询
     * @param limit 数量
     * @return 候选
     */
    public List<LongTermMemoryRecord> findKeywordRecallCandidates(
            String tenantId, String userId, String query, int limit
    ) {
        return jdbcTemplate.query("""
                SELECT memory_id, tenant_id, user_id, scope, memory_type, content, content_hash,
                       structured, source_event_ids, confidence, status, valid_from, valid_to,
                       last_accessed_at, access_count, embedding_model, created_at, updated_at
                FROM agent_long_term_memory
                WHERE tenant_id=? AND user_id=? AND status='active'
                  AND (valid_to IS NULL OR valid_to > NOW(6))
                  AND MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) > 0
                ORDER BY MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) DESC LIMIT ?
                """, baseRowMapper(), tenantId, userId, query, query, limit);
    }

    /**
     * 合并重复记忆。
     *
     * @param record 记录
     */
    public void mergeActive(LongTermMemoryRecord record) {
        jdbcTemplate.update("""
                UPDATE agent_long_term_memory
                SET source_event_ids=CAST(? AS JSON), confidence=?, structured=CAST(? AS JSON),
                    status='active', valid_to=NULL, updated_at=NOW(6)
                WHERE memory_id=?
                """, writeJson(record.getSourceEventIds()), record.getConfidence(),
                writeJson(record.getStructured()), record.getMemoryId());
    }

    /**
     * 细化内容并重算向量。
     *
     * @param record 记录
     */
    public void refineContent(LongTermMemoryRecord record) {
        jdbcTemplate.update("""
                UPDATE agent_long_term_memory
                SET content=?, content_hash=?, structured=CAST(? AS JSON),
                    source_event_ids=CAST(? AS JSON), confidence=?,
                    embedding=TO_VECTOR(?), embedding_model=?, updated_at=NOW(6)
                WHERE memory_id=?
                """, record.getContent(), record.getContentHash(), writeJson(record.getStructured()),
                writeJson(record.getSourceEventIds()), record.getConfidence(),
                toVectorLiteral(record.getEmbedding()), record.getEmbeddingModel(), record.getMemoryId());
    }

    /**
     * 标记旧记忆已被替代。
     *
     * @param memoryId 记忆ID
     */
    public void markSuperseded(String memoryId) {
        jdbcTemplate.update("""
                UPDATE agent_long_term_memory
                SET status='superseded', valid_to=NOW(6), updated_at=NOW(6)
                WHERE memory_id=? AND status='active'
                """, memoryId);
    }

    /**
     * 更新召回热度。
     *
     * @param memoryIds 记忆ID列表
     */
    public void recordAccess(List<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(memoryIds.size(), "?"));
        Object[] args = new Object[memoryIds.size() + 1];
        args[0] = Timestamp.valueOf(LocalDateTime.now());
        for (int index = 0; index < memoryIds.size(); index++) {
            args[index + 1] = memoryIds.get(index);
        }
        jdbcTemplate.update("""
                UPDATE agent_long_term_memory
                SET last_accessed_at=?, access_count=access_count+1
                WHERE memory_id IN (%s)
                """.formatted(placeholders), args);
    }

    /**
     * 按余弦相似度排序并截断。
     *
     * @param queryEmbedding 查询向量
     * @param candidates 候选
     * @param limit 数量
     * @return 排序结果
     */
    public List<LongTermMemoryRecord> rankByCosine(
            List<Float> queryEmbedding, List<LongTermMemoryRecord> candidates, int limit
    ) {
        return candidates.stream()
                .peek(record -> record.setScore(VectorSimilarityUtil.cosine(queryEmbedding, record.getEmbedding())))
                .sorted(Comparator.comparingDouble(LongTermMemoryRecord::getScore).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 序列化为 JSON。
     *
     * @param value 值
     * @return JSON
     */
    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("长期记忆 JSON 序列化失败", exception);
        }
    }

    /** @return JSON 序列化器。 */
    public ObjectMapper getObjectMapper() { return objectMapper; }

    /**
     * 读取 JSON Map。
     *
     * @param json JSON
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JacksonException exception) {
            return Map.of();
        }
    }

    /**
     * 基础行映射器。
     *
     * @return RowMapper
     */
    private RowMapper<LongTermMemoryRecord> baseRowMapper() {
        return (resultSet, rowNum) -> {
            LongTermMemoryRecord record = new LongTermMemoryRecord();
            record.setMemoryId(resultSet.getString("memory_id"));
            record.setTenantId(resultSet.getString("tenant_id"));
            record.setUserId(resultSet.getString("user_id"));
            record.setScope(resultSet.getString("scope"));
            record.setMemoryType(LongTermMemoryType.fromValue(resultSet.getString("memory_type")));
            record.setContent(resultSet.getString("content"));
            record.setContentHash(resultSet.getString("content_hash"));
            record.setStructured(readMap(resultSet.getString("structured")));
            record.setSourceEventIds(readStringList(resultSet.getString("source_event_ids")));
            record.setConfidence(resultSet.getDouble("confidence"));
            record.setStatus(resultSet.getString("status"));
            record.setValidFrom(toLocalDateTime(resultSet.getTimestamp("valid_from")));
            record.setValidTo(toLocalDateTime(resultSet.getTimestamp("valid_to")));
            record.setLastAccessedAt(toLocalDateTime(resultSet.getTimestamp("last_accessed_at")));
            record.setAccessCount(resultSet.getInt("access_count"));
            record.setEmbeddingModel(resultSet.getString("embedding_model"));
            record.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
            record.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
            return record;
        };
    }

    /**
     * 带向量行映射器。
     *
     * @return RowMapper
     */
    private RowMapper<LongTermMemoryRecord> vectorRowMapper() {
        return (resultSet, rowNum) -> {
            LongTermMemoryRecord record = baseRowMapper().mapRow(resultSet, rowNum);
            record.setEmbedding(parseVector(resultSet.getString("embedding_text")));
            return record;
        };
    }

    /**
     * 解析字符串列表。
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
            for (JsonNode item : objectMapper.readTree(json)) {
                result.add(item.asString());
            }
        } catch (JacksonException ignored) {
            // 损坏数据按空来源处理。
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

    /**
     * 解析 FROM_VECTOR 文本。
     *
     * @param text 向量文本
     * @return 向量
     */
    private List<Float> parseVector(String text) {
        if (text == null || text.length() < 2) {
            return List.of();
        }
        String[] values = text.substring(1, text.length() - 1).split(",");
        List<Float> vector = new ArrayList<>(values.length);
        for (String value : values) {
            vector.add(Float.parseFloat(value.trim()));
        }
        return vector;
    }

    /**
     * 向量转 TO_VECTOR 文本。
     *
     * @param embedding 向量
     * @return 文本
     */
    private String toVectorLiteral(List<Float> embedding) {
        if (embedding == null || embedding.size() != 1024) {
            throw new IllegalArgumentException("长期记忆向量维度必须为1024");
        }
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(embedding.get(index));
        }
        return builder.append(']').toString();
    }
}

