package com.luzhiwen.collectagent.eventing.application;

import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.application.AgentSessionStateService;
import com.luzhiwen.collectagent.memory.application.AgentSummaryService;
import com.luzhiwen.collectagent.memory.application.LongTermMemoryExtractionService;
import com.luzhiwen.collectagent.memory.application.ShortTermMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 本地记忆 Worker，负责事件幂等投影、短期缓存刷新、LTM 抽取和轮次结束摘要。
 */
@Component
public class MemoryWorker {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryWorker.class);

    /** Agent 会话状态服务。 */
    private final AgentSessionStateService stateService;

    /** 短期记忆缓存投影服务。 */
    private final ShortTermMemoryService shortTermMemoryService;

    /** 长期记忆抽取服务。 */
    private final LongTermMemoryExtractionService longTermMemoryExtractionService;

    /** Agent 会话摘要服务。 */
    private final AgentSummaryService summaryService;

    /**
     * 创建本地记忆 Worker。
     *
     * @param stateService Agent 会话状态服务
     * @param shortTermMemoryService 短期记忆缓存投影服务
     * @param longTermMemoryExtractionService 长期记忆抽取服务
     * @param summaryService Agent 会话摘要服务
     */
    public MemoryWorker(
            AgentSessionStateService stateService,
            ShortTermMemoryService shortTermMemoryService,
            LongTermMemoryExtractionService longTermMemoryExtractionService,
            AgentSummaryService summaryService
    ) {
        this.stateService = stateService;
        this.shortTermMemoryService = shortTermMemoryService;
        this.longTermMemoryExtractionService = longTermMemoryExtractionService;
        this.summaryService = summaryService;
    }

    /**
     * 处理一条原始事件。
     *
     * @param event 原始事件
     */
    public void process(EventLayerEntity event) {
        // 状态服务按 session_seq 幂等，主链路已处理过的事件不会重复覆盖。
        stateService.applyEvent(event);
        shortTermMemoryService.refresh(event.getSessionId(), event.getCustomerId());
        // 每10个完整轮次异步抽取一次长期记忆。
        longTermMemoryExtractionService.maybeExtract(event);
        // 只有轮次结束事件才检查摘要阈值，避免每个事件都调用大模型。
        boolean summaryCreated = compressAtTurnEnd(event);
        if (summaryCreated) {
            shortTermMemoryService.refresh(event.getSessionId(), event.getCustomerId());
        }
    }

    /**
     * 在轮次结束事件上尝试生成摘要。
     *
     * @param event 原始事件
     * @return true 表示生成了新摘要
     */
    private boolean compressAtTurnEnd(EventLayerEntity event) {
        if (event.getEventType() == EventType.ASSISTANT_MESSAGE
                || event.getEventType() == EventType.SESSION_CLOSED) {
            return summaryService.compressAtTurnEnd(event);
        }
        return false;
    }
}

