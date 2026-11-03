package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.eventing.application.EventPayloadReader;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.memory.domain.AgentSummaryStatus;
import com.luzhiwen.collectagent.memory.domain.ShortTermMemoryEvent;
import com.luzhiwen.collectagent.memory.domain.ShortTermMemorySnapshot;
import com.luzhiwen.collectagent.memory.infrastructure.cache.ShortTermMemoryCache;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSummaryEntity;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSummaryRepository;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 短期记忆缓存投影服务。
 *
 * <p>缓存内容由 event_layer、agent_session_state、agent_summary 重建；缓存丢失不会丢失业务事实。</p>
 */
@Service
public class ShortTermMemoryService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortTermMemoryService.class);

    /** 短期记忆缓存。 */
    private final ShortTermMemoryCache memoryCache;

    /** 原始事件数据访问接口。 */
    private final EventLayerRepository eventLayerRepository;

    /** 会话状态服务。 */
    private final AgentSessionStateService stateService;

    /** 状态视图构建器。 */
    private final AgentStateViewBuilder stateViewBuilder;

    /** 摘要数据访问接口。 */
    private final AgentSummaryRepository summaryRepository;

    /** 事件载荷读取器。 */
    private final EventPayloadReader eventPayloadReader;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建短期记忆服务。
     *
     * @param memoryCache 短期记忆缓存
     * @param eventLayerRepository 原始事件数据访问接口
     * @param stateService 会话状态服务
     * @param stateViewBuilder 状态视图构建器
     * @param summaryRepository 摘要数据访问接口
     * @param eventPayloadReader 事件载荷读取器
     * @param objectMapper JSON 序列化器
     * @param agentProperties Agent 配置
     */
    public ShortTermMemoryService(
            ShortTermMemoryCache memoryCache,
            EventLayerRepository eventLayerRepository,
            AgentSessionStateService stateService,
            AgentStateViewBuilder stateViewBuilder,
            AgentSummaryRepository summaryRepository,
            EventPayloadReader eventPayloadReader,
            ObjectMapper objectMapper,
            AgentProperties agentProperties
    ) {
        this.memoryCache = memoryCache;
        this.eventLayerRepository = eventLayerRepository;
        this.stateService = stateService;
        this.stateViewBuilder = stateViewBuilder;
        this.summaryRepository = summaryRepository;
        this.eventPayloadReader = eventPayloadReader;
        this.objectMapper = objectMapper;
        this.agentProperties = agentProperties;
    }

    /**
     * 读取短期记忆；缓存未命中时从数据库重建。
     *
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @return 短期记忆快照
     */
    public ShortTermMemorySnapshot getOrRebuild(String sessionId, String customerId) {
        String cacheKey = cacheKey(sessionId);
        Optional<String> cached = memoryCache.get(cacheKey);
        if (cached.isPresent()) {
            try {
                return objectMapper.readValue(cached.get(), ShortTermMemorySnapshot.class);
            } catch (JacksonException exception) {
                LOGGER.warn("短期记忆缓存反序列化失败，改为从数据库重建，sessionId={}", sessionId, exception);
                memoryCache.invalidate(cacheKey);
            }
        }
        return refresh(sessionId, customerId);
    }

    /**
     * 从数据库事实源重建并刷新短期记忆缓存。
     *
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @return 重建后的短期记忆快照
     */
    public ShortTermMemorySnapshot refresh(String sessionId, String customerId) {
        ShortTermMemorySnapshot snapshot = buildSnapshot(sessionId, customerId);
        try {
            memoryCache.put(cacheKey(sessionId), objectMapper.writeValueAsString(snapshot));
        } catch (JacksonException exception) {
            LOGGER.warn("短期记忆缓存序列化失败，sessionId={}", sessionId, exception);
        }
        return snapshot;
    }

    /**
     * 从事件、状态和摘要表重建快照。
     *
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @return 短期记忆快照
     */
    private ShortTermMemorySnapshot buildSnapshot(String sessionId, String customerId) {
        List<ShortTermMemoryEvent> recentEvents = loadRecentEvents(sessionId);
        Optional<com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSessionStateEntity> state =
                stateService.getState(sessionId);
        String stateJson = state.map(value -> value.getStateJson()).orElse("{}");
        String stateViewJson = state.map(stateViewBuilder::build).orElse("{}");
        Optional<AgentSummaryEntity> summary = summaryRepository
                .findFirstByTenantIdAndSessionIdAndStatusOrderByCoversSeqToDescCreatedAtDescIdDesc(
                        AgentSessionStateService.DEFAULT_TENANT_ID,
                        sessionId,
                        AgentSummaryStatus.ACTIVE
                );
        return new ShortTermMemorySnapshot(
                AgentSessionStateService.DEFAULT_TENANT_ID,
                sessionId,
                customerId,
                recentEvents,
                stateJson,
                stateViewJson,
                summary.map(AgentSummaryEntity::getSummary).orElse("{}"),
                summary.map(AgentSummaryEntity::getCoversSeqTo).orElse(null),
                LocalDateTime.now()
        );
    }

    /**
     * 查询最近 N 条对话相关事件，并按 seq 正序返回。
     *
     * @param sessionId 会话唯一标识
     * @return 最近事件列表
     */
    private List<ShortTermMemoryEvent> loadRecentEvents(String sessionId) {
        int limit = Math.max(2, agentProperties.getStm().getRecentEventLimit());
        List<EventLayerEntity> events = eventLayerRepository.findBySessionIdOrderBySessionSeqDesc(
                sessionId,
                PageRequest.of(0, limit)
        );
        List<ShortTermMemoryEvent> result = new ArrayList<>();
        for (EventLayerEntity event : events) {
            if (shouldProject(event.getEventType())) {
                result.add(new ShortTermMemoryEvent(
                        event.getEventId(),
                        event.getSessionSeq(),
                        event.getEventType().name(),
                        extractContent(event),
                        event.getOccurredAt()
                ));
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 判断事件是否进入短期记忆。
     *
     * @param eventType 事件类型
     * @return true 表示需要投影
     */
    private boolean shouldProject(EventType eventType) {
        return eventType == EventType.USER_MESSAGE
                || eventType == EventType.ASSISTANT_MESSAGE
                || eventType == EventType.TOOL_RESULT;
    }

    /**
     * 提取事件的可读内容。
     *
     * @param event 原始事件
     * @return 可读内容
     */
    private String extractContent(EventLayerEntity event) {
        var payload = eventPayloadReader.read(event);
        String content = payload.path("content").asString("");
        if (!content.isBlank()) {
            return content;
        }
        String result = payload.path("result").asString("");
        if (!result.isBlank()) {
            String toolName = payload.path("toolName").asString("tool");
            return toolName + "：" + result;
        }
        return event.getPayloadJson();
    }

    /**
     * 构建短期记忆缓存键。
     *
     * @param sessionId 会话唯一标识
     * @return 缓存键
     */
    private String cacheKey(String sessionId) {
        return "shortmem:" + AgentSessionStateService.DEFAULT_TENANT_ID + ":" + sessionId;
    }
}
