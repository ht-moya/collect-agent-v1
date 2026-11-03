package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.repository.EventLayerRepository;
import com.luzhiwen.collectagent.memory.domain.AgentSummaryResult;
import com.luzhiwen.collectagent.memory.domain.AgentSummaryStatus;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSummaryEntity;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSummaryRepository;
import com.luzhiwen.collectagent.memory.infrastructure.summarizer.AgentSummaryGenerator;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Agent 会话摘要服务。
 *
 * <p>摘要只追加到 agent_summary，不更新 agent_session_state，也不参与主链路状态乐观锁。</p>
 */
@Service
public class AgentSummaryService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSummaryService.class);

    /** 摘要数据访问接口。 */
    private final AgentSummaryRepository summaryRepository;

    /** 原始事件数据访问接口。 */
    private final EventLayerRepository eventLayerRepository;

    /** 摘要生成器。 */
    private final AgentSummaryGenerator summaryGenerator;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /** 事务模板。 */
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建会话摘要服务。
     *
     * @param summaryRepository 摘要数据访问接口
     * @param eventLayerRepository 原始事件数据访问接口
     * @param summaryGenerator 摘要生成器
     * @param agentProperties Agent 配置
     * @param transactionManager 事务管理器
     */
    public AgentSummaryService(
            AgentSummaryRepository summaryRepository,
            EventLayerRepository eventLayerRepository,
            AgentSummaryGenerator summaryGenerator,
            AgentProperties agentProperties,
            PlatformTransactionManager transactionManager
    ) {
        this.summaryRepository = summaryRepository;
        this.eventLayerRepository = eventLayerRepository;
        this.summaryGenerator = summaryGenerator;
        this.agentProperties = agentProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** @param turnEndEvent 轮次结束事件 @return true 表示生成了新摘要。 */
    public boolean compressAtTurnEnd(EventLayerEntity turnEndEvent) {
        return compress(turnEndEvent, false, null).isPresent();
    }

    /** @param triggerEvent 触发事件 @param estimatedContextTokens 当前上下文估算 Token 数 @param currentSummaryJson 当前已读取摘要 @return 新摘要文本。 */
    public Optional<String> compressIfContextBudgetExceeded(
            EventLayerEntity triggerEvent,
            int estimatedContextTokens,
            String currentSummaryJson
    ) {
        int budget = agentProperties.getStm().getContextBudgetTokenEstimate();
        if (estimatedContextTokens < budget) {
            return Optional.empty();
        }
        return compress(triggerEvent, true, currentSummaryJson).map(AgentSummaryEntity::getSummary);
    }

    /** @param sessionId 会话唯一标识 @return 最新有效摘要。 */
    public Optional<AgentSummaryEntity> getLatestActiveSummary(String sessionId) {
        return summaryRepository.findFirstByTenantIdAndSessionIdAndStatusOrderByCoversSeqToDescCreatedAtDescIdDesc(
                AgentSessionStateService.DEFAULT_TENANT_ID,
                sessionId,
                AgentSummaryStatus.ACTIVE
        );
    }

    /** @param sessionId 会话唯一标识 @return 最新摘要文本。 */
    public String getActiveSummaryJson(String sessionId) {
        return getLatestActiveSummary(sessionId).map(AgentSummaryEntity::getSummary).orElse("{}");
    }

    /**
     * 执行摘要压缩并持久化新版本。
     *
     * @param triggerEvent 触发事件
     * @param forceByContextBudget 是否由上下文预算强制触发
     * @param currentSummaryJson 调用方已读取的当前摘要
     * @return 新摘要实体
     */
    private Optional<AgentSummaryEntity> compress(
            EventLayerEntity triggerEvent,
            boolean forceByContextBudget,
            String currentSummaryJson
    ) {
        Optional<AgentSummaryEntity> latestSummary = getLatestActiveSummary(triggerEvent.getSessionId());
        long lastCoveredSeq = latestSummary.map(AgentSummaryEntity::getCoversSeqTo).orElse(0L);
        List<EventLayerEntity> unsummarized = eventLayerRepository
                .findBySessionIdAndSessionSeqGreaterThanOrderBySessionSeqAsc(
                        triggerEvent.getSessionId(),
                        lastCoveredSeq
                );
        if (!forceByContextBudget && !shouldCompressAtTurnEnd(unsummarized)) {
            return Optional.empty();
        }
        int keepRecent = Math.max(1, agentProperties.getStm().getSummaryKeepRecentEventCount());
        if (unsummarized.size() <= keepRecent) {
            return Optional.empty();
        }
        List<EventLayerEntity> eventsToSummarize = unsummarized.subList(0, unsummarized.size() - keepRecent);
        String previousSummary = currentSummaryJson == null || "{}".equals(currentSummaryJson)
                ? latestSummary.map(AgentSummaryEntity::getSummary).orElse("{}")
                : currentSummaryJson;
        try {
            AgentSummaryResult result = summaryGenerator.summarize(
                    triggerEvent.getTraceId(),
                    triggerEvent.getSessionId(),
                    triggerEvent.getCustomerId(),
                    previousSummary,
                    eventsToSummarize
            );
            return Optional.of(persistSummary(triggerEvent, latestSummary, result, eventsToSummarize));
        } catch (Exception exception) {
            LOGGER.warn("会话摘要生成失败，保留旧摘要，sessionId={}", triggerEvent.getSessionId(), exception);
            return Optional.empty();
        }
    }

    /**
     * 持久化新摘要，并把旧活动摘要标记为已替代。
     *
     * @param triggerEvent 触发事件
     * @param latestSummary 旧活动摘要
     * @param result 新摘要结果
     * @param sourceEvents 摘要来源事件
     * @return 新摘要实体
     */
    private AgentSummaryEntity persistSummary(
            EventLayerEntity triggerEvent,
            Optional<AgentSummaryEntity> latestSummary,
            AgentSummaryResult result,
            List<EventLayerEntity> sourceEvents
    ) {
        return transactionTemplate.execute(status -> {
            long coversSeqFrom = sourceEvents.get(0).getSessionSeq();
            long coversSeqTo = sourceEvents.get(sourceEvents.size() - 1).getSessionSeq();
            latestSummary.ifPresent(oldSummary -> {
                oldSummary.setStatus(AgentSummaryStatus.SUPERSEDED);
                summaryRepository.save(oldSummary);
            });
            return summaryRepository.save(new AgentSummaryEntity(
                    AgentSessionStateService.DEFAULT_TENANT_ID,
                    triggerEvent.getSessionId(),
                    result.getSummary(),
                    coversSeqFrom,
                    coversSeqTo,
                    result.getModelName(),
                    agentProperties.getStm().getSummaryPromptVersion(),
                    result.getTokenCount()
            ));
        });
    }

    /**
     * 判断轮次结束时是否达到摘要阈值。
     *
     * @param events 尚未摘要的事件
     * @return true 表示需要压缩
     */
    private boolean shouldCompressAtTurnEnd(List<EventLayerEntity> events) {
        if (events.size() >= agentProperties.getStm().getSummaryTriggerEventCount()) {
            return true;
        }
        int characterCount = events.stream().mapToInt(event -> event.getPayloadJson().length()).sum();
        return characterCount / 2 >= agentProperties.getStm().getSummaryTriggerTokenEstimate();
    }
}
