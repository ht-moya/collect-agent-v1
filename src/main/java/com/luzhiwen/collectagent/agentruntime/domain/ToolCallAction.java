package com.luzhiwen.collectagent.agentruntime.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 决定调用工具时返回的动作。
 */
public class ToolCallAction implements AgentAction {

    /**
     * 工具调用唯一标识，本地规则 Planner 可为空。
     */
    private final String toolCallId;

    /**
     * 工具名称。
     */
    private final String toolName;

    /**
     * 传给工具的参数。
     */
    private final Map<String, Object> arguments;

    /**
     * Agent 选择该工具的原因，用于日志和调试。
     */
    private final String reason;

    /**
     * 创建工具调用动作。
     *
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @param reason 选择原因
     */
    public ToolCallAction(String toolName, Map<String, Object> arguments, String reason) {
        this(null, toolName, arguments, reason);
    }

    /**
     * 创建带工具调用唯一标识的工具调用动作。
     *
     * @param toolCallId 工具调用唯一标识
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @param reason 选择原因
     */
    public ToolCallAction(
            String toolCallId,
            String toolName,
            Map<String, Object> arguments,
            String reason
    ) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        this.reason = reason;
    }

    /** @return 工具调用唯一标识。 */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * 获取工具名称。
     *
     * @return 工具名称
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * 获取工具参数。
     *
     * @return 不可修改的工具参数
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * 获取工具选择原因。
     *
     * @return 工具选择原因
     */
    public String getReason() {
        return reason;
    }
}

