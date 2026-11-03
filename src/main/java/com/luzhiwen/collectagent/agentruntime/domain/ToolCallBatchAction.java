package com.luzhiwen.collectagent.agentruntime.domain;

import java.util.List;

/**
 * 一次模型响应中的多个工具调用动作。
 */
public class ToolCallBatchAction implements AgentAction {

    /** 本批次工具调用列表。 */
    private final List<ToolCallAction> actions;

    /** 批次说明。 */
    private final String reason;

    /**
     * 创建工具调用批次。
     *
     * @param actions 工具调用列表
     * @param reason 批次说明
     */
    public ToolCallBatchAction(List<ToolCallAction> actions, String reason) {
        this.actions = List.copyOf(actions);
        this.reason = reason;
    }

    /** @return 工具调用列表。 */
    public List<ToolCallAction> getActions() { return actions; }

    /** @return 批次说明。 */
    public String getReason() { return reason; }
}
