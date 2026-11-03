package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.memory.domain.AgentStage;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSessionStateEntity;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.repository.AgentSessionStateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话状态行并发初始化器。
 *
 * <p>使用独立事务创建初始状态，避免并发首次请求的唯一键冲突污染主事务。</p>
 */
@Component
public class AgentSessionStateProvisioner {

    /** 状态数据访问接口。 */
    private final AgentSessionStateRepository stateRepository;

    /**
     * 创建状态初始化器。
     *
     * @param stateRepository 状态数据访问接口
     */
    public AgentSessionStateProvisioner(AgentSessionStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    /**
     * 在状态不存在时创建初始行。
     *
     * @param sessionId 会话唯一标识
     * @param userId 用户唯一标识
     * @param traceId 链路标识
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIfAbsent(String sessionId, String userId, String traceId) {
        if (stateRepository.existsById(sessionId)) {
            return;
        }
        String initialState = "{\"slots\":{},\"pending\":[],\"flags\":{}}";
        stateRepository.saveAndFlush(new AgentSessionStateEntity(
                sessionId,
                AgentSessionStateService.DEFAULT_TENANT_ID,
                userId,
                "repayment_reminder",
                AgentStage.OPENING.name(),
                initialState
        ));
    }
}
