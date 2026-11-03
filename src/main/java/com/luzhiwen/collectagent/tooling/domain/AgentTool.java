package com.luzhiwen.collectagent.tooling.domain;

import java.util.Map;

/**
 * Agent 可调用工具的统一接口。
 */
public interface AgentTool {

    /**
     * 获取工具唯一名称。
     *
     * @return 工具名称
     */
    String name();

    /**
     * 获取工具用途说明。
     *
     * @return 工具用途说明
     */
    String description();

    /**
     * 判断工具是否暴露给普通 Planner 自动选择。
     *
     * @return true 表示允许普通 Planner 自动选择
     */
    default boolean visibleToPlanner() {
        return true;
    }

    /**
     * 获取 OpenAI 兼容 Function Calling 参数 Schema。
     *
     * @return JSON Schema 结构
     */
    Map<String, Object> parameters();

    /**
     * 执行工具。
     *
     * @param arguments 工具参数
     * @return 工具执行结果
     */
    ToolResult execute(Map<String, Object> arguments);
}


