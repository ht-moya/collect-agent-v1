package com.luzhiwen.collectagent.conversation.application;

import com.luzhiwen.collectagent.memory.domain.AgentStage;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSessionStateRepository;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 超时会话自动关闭任务。
 */
@Component
public class AgentSessionTimeoutJob {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSessionTimeoutJob.class);

    /** 会话状态仓储。 */
    private final AgentSessionStateRepository stateRepository;

    /** 会话生命周期服务。 */
    private final AgentSessionLifecycleService lifecycleService;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /**
     * 创建超时任务。
     *
     * @param stateRepository 状态仓储
     * @param lifecycleService 生命周期服务
     * @param agentProperties Agent配置
     */
    public AgentSessionTimeoutJob(
            AgentSessionStateRepository stateRepository,
            AgentSessionLifecycleService lifecycleService,
            AgentProperties agentProperties
    ) {
        this.stateRepository = stateRepository;
        this.lifecycleService = lifecycleService;
        this.agentProperties = agentProperties;
    }

    /**
     * 定时扫描并关闭超时会话。
     */
    @Scheduled(fixedDelayString = "${agent.session.scan-interval-ms:60000}")
    public void closeTimedOutSessions() {
        if (!agentProperties.getSession().isAutoCloseEnabled()) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(agentProperties.getSession().getTimeoutMinutes());
        var sessions = stateRepository.findByStageNotAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                AgentStage.CLOSED.name(), cutoff, PageRequest.of(0, 100)
        );
        for (var state : sessions) {
            try {
                lifecycleService.closeSession(
                        "timeout-" + UUID.randomUUID().toString().replace("-", ""),
                        state.getSessionId(),
                        "SESSION_TIMEOUT"
                );
            } catch (Exception exception) {
                LOGGER.warn("超时会话关闭失败，sessionId={}", state.getSessionId(), exception);
            }
        }
    }
}
