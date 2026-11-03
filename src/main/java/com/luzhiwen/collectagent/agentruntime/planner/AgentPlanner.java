package com.luzhiwen.collectagent.agentruntime.planner;

import com.luzhiwen.collectagent.agentruntime.domain.AgentAction;
import com.luzhiwen.collectagent.agentruntime.domain.AgentContext;

/**
 * Agent 规划器接口。
 *
 * <p>规划器根据完整上下文决定下一步调用工具还是直接回复。
 * 当前使用规则实现，后续可替换为 LLM 实现。</p>
 */
public interface AgentPlanner {

    /**
     * 根据当前上下文决定下一步动作。
     *
     * @param context Agent 当前完整上下文
     * @return 工具调用动作或最终回复动作
     */
    AgentAction decide(AgentContext context);
}

