package com.luzhiwen.collectagent.tooling.application;

import com.luzhiwen.collectagent.tooling.domain.AgentTool;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心，负责按名称查找 Agent 可调用工具。
 */
@Component
public class ToolRegistry {

    /**
     * 工具名称到工具实例的映射。
     */
    private final Map<String, AgentTool> toolsByName;

    /**
     * 创建工具注册中心并注册 Spring 容器中的全部工具。
     *
     * @param tools 容器中的全部 Agent 工具
     */
    public ToolRegistry(List<AgentTool> tools) {
        Map<String, AgentTool> registeredTools = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            if (!tool.visibleToPlanner()) {
                continue;
            }
            AgentTool previous = registeredTools.put(tool.name(), tool);
            if (previous != null) {
                throw new IllegalStateException("存在重复工具名称：" + tool.name());
            }
        }
        this.toolsByName = Collections.unmodifiableMap(registeredTools);
    }

    /**
     * 根据名称获取工具，不存在时抛出参数异常。
     *
     * @param toolName 工具名称
     * @return 工具实例
     */
    public AgentTool getRequiredTool(String toolName) {
        AgentTool tool = toolsByName.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("未知工具：" + toolName);
        }
        return tool;
    }

    /**
     * 获取全部已注册工具。
     *
     * @return 不可修改的工具列表
     */
    public List<AgentTool> listTools() {
        return List.copyOf(toolsByName.values());
    }
}

