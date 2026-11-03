package com.luzhiwen.collectagent;

import com.luzhiwen.collectagent.agentruntime.application.ConversationHistoryService;
import com.luzhiwen.collectagent.eventing.application.EventAppendService;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.memory.application.AgentSessionStateService;
import com.luzhiwen.collectagent.memory.application.AgentStateViewBuilder;
import com.luzhiwen.collectagent.memory.application.AgentSummaryService;
import com.luzhiwen.collectagent.memory.application.ShortTermMemoryService;
import com.luzhiwen.collectagent.memory.domain.AgentSlotPatch;
import com.luzhiwen.collectagent.memory.domain.AgentStatePatch;
import com.luzhiwen.collectagent.memory.domain.AgentSummaryStatus;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSummaryEntity;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSummaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 短期记忆、状态补丁和摘要查询集成测试。
 */
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
        "agent.embedding.enabled=false",
        "agent.llm.enabled=false",
        "agent.outbox.relay-interval-ms=100000",
        "agent.stm.summary-trigger-event-count=4",
        "agent.stm.summary-keep-recent-event-count=2"
})
class AgentMemoryIntegrationTest {

    /** 原始事件追加服务。 */
    @Autowired
    private EventAppendService eventAppendService;

    /** Agent 会话状态服务。 */
    @Autowired
    private AgentSessionStateService stateService;

    /** 状态视图构建器。 */
    @Autowired
    private AgentStateViewBuilder stateViewBuilder;

    /** 短期记忆服务。 */
    @Autowired
    private ShortTermMemoryService shortTermMemoryService;

    /** 摘要数据访问接口。 */
    @Autowired
    private AgentSummaryRepository summaryRepository;

    /** 对话历史回放服务。 */
    @Autowired
    private ConversationHistoryService conversationHistoryService;

    /** 会话摘要服务。 */
    @Autowired
    private AgentSummaryService summaryService;

    /**
     * 验证工具写入只读槽位、LLM 补丁白名单校验和短期缓存重建。
     */
    @Test
    void shouldMergeOnlyValidStatePatchAndRebuildShortMemory() {
        var started = eventAppendService.append(
                "trace-memory-001", "S-MEMORY-001", "C10001", EventType.SESSION_STARTED,
                Map.of("content", "会话开始")
        );
        stateService.applyEvent(started);
        var user = eventAppendService.append(
                "trace-memory-001", "S-MEMORY-001", "C10001", EventType.USER_MESSAGE,
                Map.of("content", "我欠多少钱，11月10日可以还5000")
        );
        stateService.applyEvent(user);
        var toolResult = eventAppendService.append(
                "trace-memory-001", "S-MEMORY-001", "C10001", EventType.TOOL_RESULT,
                Map.of(
                        "toolName", "customer_query",
                        "result", "客户当前应还12000元",
                        "data", Map.of("outstandingAmount", 12000, "overdueDays", 45)
                )
        );
        stateService.applyEvent(toolResult);

        AgentStatePatch patch = new AgentStatePatch();
        patch.setStage("REMINDING");
        patch.setSlots(Map.of(
                "promise_date", new AgentSlotPatch("2026-12-10", 0.98),
                "promise_amount", new AgentSlotPatch(5000, 0.98),
                "repayment_intent", new AgentSlotPatch("willing", 0.95),
                "debt_amount", new AgentSlotPatch(1, 0.99)
        ));
        var updated = stateService.applyPatch("S-MEMORY-001", patch, user.getId());

        assertEquals("REMINDING", updated.getStage());
        assertTrue(updated.getStateJson().contains("promise_amount"));
        assertTrue(updated.getStateJson().contains("debt_amount"));
        assertTrue(updated.getStateJson().contains("12000"));
        assertTrue(updated.getStateJson().contains("promise_date"));

        var snapshot = shortTermMemoryService.refresh("S-MEMORY-001", "C10001");
        assertEquals("S-MEMORY-001", snapshot.getSessionId());
        assertTrue(snapshot.getStateViewJson().contains("known_slots"));
        assertTrue(snapshot.getRecentEvents().stream()
                .anyMatch(event -> "USER_MESSAGE".equals(event.getEventType())));
    }

    /**
     * 验证模型只回放摘要位点之后最近 N 轮，而不是整个会话历史。
     */
    @Test
    void shouldOnlyLoadRecentTurnsAfterSummaryBoundary() {
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.SESSION_STARTED, Map.of("content", "会话开始"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.USER_MESSAGE, Map.of("content", "第一轮"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.ASSISTANT_MESSAGE, Map.of("content", "第一轮回复"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.USER_MESSAGE, Map.of("content", "第二轮"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.ASSISTANT_MESSAGE, Map.of("content", "第二轮回复"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.USER_MESSAGE, Map.of("content", "第三轮"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.ASSISTANT_MESSAGE, Map.of("content", "第三轮回复"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.USER_MESSAGE, Map.of("content", "第四轮"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.ASSISTANT_MESSAGE, Map.of("content", "第四轮回复"));
        eventAppendService.append("trace-window", "S-WINDOW-001", "C10001",
                EventType.USER_MESSAGE, Map.of("content", "当前消息"));

        var messages = conversationHistoryService.loadRecentTurns("S-WINDOW-001", 5L, 10L, 2);

        assertEquals(4, messages.size());
        assertEquals("第三轮", messages.get(0).getContent());
        assertEquals("第三轮回复", messages.get(1).getContent());
        assertEquals("第四轮", messages.get(2).getContent());
        assertEquals("第四轮回复", messages.get(3).getContent());
    }

    /**
     * 验证轮次结束时能够从事件事实源生成摘要，并保留最近事件原文。
     */
    @Test
    void shouldGenerateSummaryAtTurnEnd() {
        eventAppendService.append("trace-summary-001", "S-SUMMARY-GEN-001", "C10001",
                EventType.SESSION_STARTED, Map.of("content", "会话开始"));
        eventAppendService.append("trace-summary-001", "S-SUMMARY-GEN-001", "C10001",
                EventType.USER_MESSAGE, Map.of("content", "我欠多少钱"));
        eventAppendService.append("trace-summary-001", "S-SUMMARY-GEN-001", "C10001",
                EventType.TOOL_CALL, Map.of("toolName", "customer_query", "args", Map.of("customerId", "C10001")));
        eventAppendService.append("trace-summary-001", "S-SUMMARY-GEN-001", "C10001",
                EventType.TOOL_RESULT, Map.of("toolName", "customer_query", "result", "当前应还12000元"));
        eventAppendService.append("trace-summary-001", "S-SUMMARY-GEN-001", "C10001",
                EventType.USER_MESSAGE, Map.of("content", "我想分期"));
        var assistant = eventAppendService.append("trace-summary-001", "S-SUMMARY-GEN-001", "C10001",
                EventType.ASSISTANT_MESSAGE, Map.of("content", "可以为您介绍分期方案"));

        assertTrue(summaryService.compressAtTurnEnd(assistant));

        var latest = summaryService.getLatestActiveSummary("S-SUMMARY-GEN-001").orElseThrow();
        assertEquals(4L, latest.getCoversSeqTo());
        assertTrue(latest.getSummary().contains("confirmedFacts"));
    }

    /**
     * 验证最新 active 摘要按 covers_seq_to、created_at、id 排序返回。
     */
    @Test
    void shouldReadLatestActiveSummaryByCoveredSequence() {
        summaryRepository.save(new AgentSummaryEntity(
                AgentSessionStateService.DEFAULT_TENANT_ID,
                "S-SUMMARY-001",
                "{\"customerGoal\":\"旧摘要\"}",
                1,
                2,
                "RULE",
                "v1",
                10
        ));
        summaryRepository.save(new AgentSummaryEntity(
                AgentSessionStateService.DEFAULT_TENANT_ID,
                "S-SUMMARY-001",
                "{\"customerGoal\":\"新摘要\"}",
                1,
                4,
                "RULE",
                "v1",
                10
        ));

        var latest = summaryRepository
                .findFirstByTenantIdAndSessionIdAndStatusOrderByCoversSeqToDescCreatedAtDescIdDesc(
                        AgentSessionStateService.DEFAULT_TENANT_ID,
                        "S-SUMMARY-001",
                        AgentSummaryStatus.ACTIVE
                )
                .orElseThrow();

        assertEquals(4L, latest.getCoversSeqTo());
        assertTrue(latest.getSummary().contains("新摘要"));
    }
}






