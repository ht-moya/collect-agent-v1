package com.luzhiwen.collectagent.memory.infrastructure.summarizer;

import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.domain.AgentSummaryResult;

import java.util.List;

/**
 * Agent 会话摘要生成器。
 */
public interface AgentSummaryGenerator {

    /**
     * 根据旧摘要和新增事件生成新摘要。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param previousSummary 旧摘要
     * @param events 本次待压缩事件
     * @return 摘要结果
     */
    AgentSummaryResult summarize(
            String traceId,
            String sessionId,
            String customerId,
            String previousSummary,
            List<EventLayerEntity> events
    );
}
