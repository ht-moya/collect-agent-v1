package com.luzhiwen.collectagent.agentruntime.domain;

import com.luzhiwen.collectagent.memory.domain.AgentStatePatch;

import java.util.Map;

/**
 * 最终 JSON 信封解析结果。
 */
public class FinalAnswerPayload {

    /** 从 ReAct 过程中抽取出的变量映射。 */
    private final Map<String, String> variableMap;

    /** 面向客户的业务回答。 */
    private final String answer;

    /** LLM 返回的状态变化补丁。 */
    private final AgentStatePatch statePatch;

    /**
     * 创建不含状态补丁的最终回答载荷。
     *
     * @param variableMap 变量映射
     * @param answer 业务回答
     */
    public FinalAnswerPayload(Map<String, String> variableMap, String answer) {
        this(variableMap, answer, new AgentStatePatch());
    }

    /**
     * 创建最终回答载荷。
     *
     * @param variableMap 变量映射
     * @param answer 业务回答
     * @param statePatch 状态变化补丁
     */
    public FinalAnswerPayload(Map<String, String> variableMap, String answer, AgentStatePatch statePatch) {
        this.variableMap = Map.copyOf(variableMap);
        this.answer = answer;
        this.statePatch = statePatch == null ? new AgentStatePatch() : statePatch;
    }

    /** @return 变量映射。 */
    public Map<String, String> getVariableMap() { return variableMap; }

    /** @return 业务回答。 */
    public String getAnswer() { return answer; }

    /** @return 状态变化补丁。 */
    public AgentStatePatch getStatePatch() { return statePatch; }
}
