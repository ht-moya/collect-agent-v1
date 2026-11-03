package com.luzhiwen.collectagent;

import com.luzhiwen.collectagent.memory.application.LongTermMemoryCandidateValidator;
import com.luzhiwen.collectagent.memory.application.LongTermMemoryContentUtil;
import com.luzhiwen.collectagent.memory.application.LongTermMemoryWriteService;
import com.luzhiwen.collectagent.memory.application.UlidGenerator;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryCandidate;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryRecord;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryType;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.LongTermMemoryRepository;
import com.luzhiwen.collectagent.model.application.EmbeddingClient;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 长期记忆纯逻辑单元测试。
 */
class LongTermMemoryUnitTest {

    /** 候选校验器。 */
    private final LongTermMemoryCandidateValidator validator = new LongTermMemoryCandidateValidator();

    /**
     * 验证内容归一化、MD5 和 ULID。
     */
    @Test
    void shouldNormalizeHashAndGenerateUlid() {
        assertEquals("用户 更接受短信联系", LongTermMemoryContentUtil.normalize(" 用户   更接受短信联系 "));
        assertEquals(32, LongTermMemoryContentUtil.md5("用户更接受短信联系").length());
        assertEquals(26, UlidGenerator.next().length());
    }

    /**
     * 验证非法structured字段被移除，合法来源保留。
     */
    @Test
    void shouldValidateCandidateSchema() {
        LongTermMemoryCandidate candidate = new LongTermMemoryCandidate(
                LongTermMemoryType.PREFERENCE,
                "用户更接受短信联系",
                Map.of("channel", "sms", "illegal", "x"),
                List.of("evt-1", "evt-2"),
                0.82D
        );
        var result = validator.validate(candidate, Set.of("evt-1")).orElseThrow();
        assertEquals(Map.of("channel", "sms"), result.getStructured());
        assertEquals(List.of("evt-1"), result.getSourceEventIds());
    }

    /**
     * 验证身份证或银行卡等长数字内容被拒绝。
     */
    /**
     * 验证经济压力可以使用独立 financial_status 事实键。
     */
    @Test
    void shouldAcceptFinancialStatusFact() {
        LongTermMemoryCandidate candidate = new LongTermMemoryCandidate(
                LongTermMemoryType.FACT,
                "用户当前经济紧张",
                Map.of("fact_key", "financial_status", "fact_value", "经济紧张"),
                List.of("evt-1"),
                0.8D
        );
        var result = validator.validate(candidate, Set.of("evt-1")).orElseThrow();
        assertEquals("financial_status", result.getStructured().get("fact_key"));
    }

    /**
     * 验证内容不同但 fact_key 和 fact_value 相同的记忆会合并，而不是新增重复记录。
     */
    @Test
    void shouldMergeSameStructuredFactAcrossDifferentContent() {
        AtomicReference<LongTermMemoryRecord> merged = new AtomicReference<>();
        LongTermMemoryRecord existing = new LongTermMemoryRecord();
        existing.setMemoryId("old-memory");
        existing.setMemoryType(LongTermMemoryType.FACT);
        existing.setStructured(Map.of("fact_key", "financial_status", "fact_value", "经济紧张"));
        existing.setSourceEventIds(new ArrayList<>(List.of("event-1")));
        existing.setConfidence(0.70D);

        LongTermMemoryRepository repository = new LongTermMemoryRepository(null, null) {
            @Override
            public Optional<LongTermMemoryRecord> findExact(
                    String tenantId, String userId, LongTermMemoryType type, String contentHash
            ) {
                return Optional.empty();
            }

            @Override
            public List<LongTermMemoryRecord> findActiveFactsByKey(
                    String tenantId, String userId, String factKey
            ) {
                return List.of(existing);
            }

            @Override
            public void mergeActive(LongTermMemoryRecord record) {
                merged.set(record);
            }

            @Override
            public void insertActive(LongTermMemoryRecord record) {
                throw new AssertionError("相同事实值不应新增记录");
            }
        };
        EmbeddingClient embeddingClient = new EmbeddingClient() {
            @Override public boolean isAvailable() { return true; }
            @Override public String modelName() { return "test-model"; }
            @Override public int dimension() { return 1024; }
            @Override public List<List<Float>> embed(List<String> texts) {
                return List.of(java.util.Collections.nCopies(1024, 0.0F));
            }
        };
        LongTermMemoryWriteService service = new LongTermMemoryWriteService(
                repository, null, embeddingClient, new AgentProperties()
        );
        service.writeCandidates("tenant-1", "C10001", List.of(new LongTermMemoryCandidate(
                LongTermMemoryType.FACT,
                "用户自述目前经济紧张",
                Map.of("fact_key", "financial_status", "fact_value", "经济紧张"),
                List.of("event-2"),
                0.80D
        )));

        assertEquals(0.80D, merged.get().getConfidence());
        assertTrue(merged.get().getSourceEventIds().containsAll(List.of("event-1", "event-2")));
    }

    /**
     * 验证身份证或银行卡等长数字内容被拒绝。
     */
    @Test
    void shouldRejectSensitiveContent() {
        LongTermMemoryCandidate candidate = new LongTermMemoryCandidate(
                LongTermMemoryType.FACT,
                "银行卡号6222021234567890123",
                Map.of(),
                List.of("evt-1"),
                0.9D
        );
        assertTrue(validator.validate(candidate, Set.of("evt-1")).isEmpty());
    }
}
