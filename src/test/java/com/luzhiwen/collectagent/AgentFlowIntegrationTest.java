package com.luzhiwen.collectagent;

import com.luzhiwen.collectagent.agentruntime.application.AgentOrchestrator;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatRequest;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatResponse;
import com.luzhiwen.collectagent.eventing.application.OutboxRelayService;
import com.luzhiwen.collectagent.eventing.domain.OutboxStatus;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.OutboxEventRepository;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSessionStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Agent 主链路集成测试。
 */
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
        "agent.embedding.enabled=false",
        "agent.llm.enabled=false",
        "agent.outbox.relay-interval-ms=100000"
})
class AgentFlowIntegrationTest {

    /** Agent 编排器。 */
    @Autowired
    private AgentOrchestrator agentOrchestrator;

    /** 原始事件数据访问接口。 */
    @Autowired
    private EventLayerRepository eventLayerRepository;

    /** Agent 会话状态数据访问接口。 */
    @Autowired
    private AgentSessionStateRepository stateRepository;

    /** Outbox 数据访问接口。 */
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    /** Outbox Relay。 */
    @Autowired
    private OutboxRelayService outboxRelayService;

    /**
     * 验证聊天、事件 seq、状态、Outbox 和 LTM 的完整链路。
     */
    @Test
    void shouldCreateEventStateAndOutbox() {
        AgentChatRequest request = new AgentChatRequest();
        request.setCustomerId("C10001");
        request.setSessionId("S-FLOW-001");
        request.setMessage("我失业了，压力很大，能分期还款吗？");

        AgentChatResponse response = agentOrchestrator.chat(request, "trace-flow-001");

        assertNotNull(response.getAnswer());
        assertFalse(response.getToolCalls().isEmpty());
        assertEquals(7, eventLayerRepository.count());
        assertEquals(1, stateRepository.count());
        assertEquals(7, outboxEventRepository.count());

        var events = eventLayerRepository.findBySessionIdOrderBySessionSeqAsc("S-FLOW-001");
        for (int index = 0; index < events.size(); index++) {
            assertEquals((long) index + 1, events.get(index).getSessionSeq());
        }

        outboxRelayService.relay();

        long processed = outboxEventRepository.findAll().stream()
                .filter(event -> event.getStatus() == OutboxStatus.PROCESSED)
                .count();
        assertEquals(7, processed);
        assertEquals(7L, stateRepository.findById("S-FLOW-001").orElseThrow().getLastSeq());
    }
}






