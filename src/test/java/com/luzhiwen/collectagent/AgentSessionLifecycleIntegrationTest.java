package com.luzhiwen.collectagent;

import com.luzhiwen.collectagent.agentruntime.application.AgentOrchestrator;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatRequest;
import com.luzhiwen.collectagent.conversation.application.SessionClosedException;
import com.luzhiwen.collectagent.conversation.application.AgentSessionLifecycleService;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.memory.domain.AgentStage;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSessionStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 会话关闭生命周期集成测试。
 */
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class AgentSessionLifecycleIntegrationTest {

    /** Agent编排器。 */
    @Autowired
    private AgentOrchestrator orchestrator;

    /** 生命周期服务。 */
    @Autowired
    private AgentSessionLifecycleService lifecycleService;

    /** 状态仓储。 */
    @Autowired
    private AgentSessionStateRepository stateRepository;

    /** 事件仓储。 */
    @Autowired
    private EventLayerRepository eventRepository;

    /**
     * 验证显式关闭会话、关闭事件写入和幂等性。
     */
    @Test
    void shouldCloseSessionIdempotently() {
        AgentChatRequest request = new AgentChatRequest();
        request.setCustomerId("C10001");
        request.setSessionId("S-CLOSE-TEST-001");
        request.setMessage("我欠多少钱？");
        orchestrator.chat(request, "trace-close-chat");

        var firstClose = lifecycleService.closeSession(
                "trace-close-001", "S-CLOSE-TEST-001", "USER_CLOSED"
        );
        long eventsAfterFirstClose = eventRepository.count();
        var events = eventRepository.findBySessionIdOrderBySessionSeqAsc("S-CLOSE-TEST-001");

        assertNotNull(firstClose.getEventId());
        assertEquals("CLOSED", firstClose.getStatus());
        assertEquals(AgentStage.CLOSED.name(),
                stateRepository.findById("S-CLOSE-TEST-001").orElseThrow().getStage());
        assertEquals(EventType.SESSION_CLOSED, events.get(events.size() - 1).getEventType());

        lifecycleService.closeSession("trace-close-002", "S-CLOSE-TEST-001", "DUPLICATE");
        assertEquals(eventsAfterFirstClose, eventRepository.count());

        // 已关闭会话不能继续聊天，也不能再追加 USER_MESSAGE 污染原会话。
        assertThrows(SessionClosedException.class,
                () -> orchestrator.chat(request, "trace-close-chat-after-closed"));
        assertEquals(eventsAfterFirstClose, eventRepository.count());
    }
}
