package com.luzhiwen.collectagent.conversation.application;

import com.luzhiwen.collectagent.conversation.api.dto.SessionCloseResponse;
import com.luzhiwen.collectagent.eventing.application.EventAppendService;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.application.AgentSessionStateService;
import com.luzhiwen.collectagent.memory.application.ShortTermMemoryService;
import com.luzhiwen.collectagent.memory.domain.AgentStage;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSessionStateEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 会话生命周期服务。
 */
@Service
public class AgentSessionLifecycleService {

    /** 事件追加服务。 */
    private final EventAppendService eventAppendService;

    /** 会话状态服务。 */
    private final AgentSessionStateService stateService;

    /** 短期记忆缓存服务。 */
    private final ShortTermMemoryService shortTermMemoryService;

    /**
     * 创建生命周期服务。
     *
     * @param eventAppendService 事件追加服务
     * @param stateService 会话状态服务
     * @param shortTermMemoryService 短期记忆缓存服务
     */
    public AgentSessionLifecycleService(
            EventAppendService eventAppendService,
            AgentSessionStateService stateService,
            ShortTermMemoryService shortTermMemoryService
    ) {
        this.eventAppendService = eventAppendService;
        this.stateService = stateService;
        this.shortTermMemoryService = shortTermMemoryService;
    }

    /**
     * 显式关闭会话。
     *
     * @param traceId 链路ID
     * @param sessionId 会话ID
     * @param reason 关闭原因
     * @return 关闭响应
     */
    @Transactional
    public SessionCloseResponse closeSession(String traceId, String sessionId, String reason) {
        AgentSessionStateEntity state = stateService.lockState(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在：" + sessionId));
        if (AgentStage.CLOSED.name().equals(state.getStage())) {
            return response(sessionId, null, state.getLastSeq());
        }
        String safeReason = reason == null || reason.isBlank() ? "USER_CLOSED" : reason;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", safeReason);
        payload.put("closedAt", LocalDateTime.now().toString());
        EventLayerEntity event = eventAppendService.append(
                traceId, sessionId, state.getUserId(), EventType.SESSION_CLOSED, payload
        );
        stateService.applyEvent(event);
        shortTermMemoryService.refresh(sessionId, state.getUserId());
        return response(sessionId, event, event.getSessionSeq());
    }

    /**
     * 构造关闭响应。
     *
     * @param sessionId 会话ID
     * @param event 关闭事件，已关闭时为空
     * @param sessionSeq 关闭序号
     * @return 响应
     */
    private SessionCloseResponse response(String sessionId, EventLayerEntity event, long sessionSeq) {
        SessionCloseResponse response = new SessionCloseResponse();
        response.setSessionId(sessionId);
        response.setStatus("CLOSED");
        response.setClosedAt(LocalDateTime.now());
        response.setEventId(event == null ? null : event.getEventId());
        response.setSessionSeq(sessionSeq);
        return response;
    }
}
