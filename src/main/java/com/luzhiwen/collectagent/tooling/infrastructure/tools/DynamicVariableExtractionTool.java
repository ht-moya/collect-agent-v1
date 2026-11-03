package com.luzhiwen.collectagent.tooling.infrastructure.tools;

import com.luzhiwen.collectagent.tooling.application.DynamicVariableInstructionService;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import com.luzhiwen.collectagent.tooling.domain.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 动态变量说明工具。
 *
 * <p>该工具不接收 LLM 参数，只返回 MySQL 中当前启用的变量名称和描述。</p>
 */
@Component
public class DynamicVariableExtractionTool implements AgentTool {

    /** 动态变量指令服务。 */
    private final DynamicVariableInstructionService instructionService;

    /**
     * 创建动态变量说明工具。
     *
     * @param instructionService 动态变量指令服务
     */
    public DynamicVariableExtractionTool(DynamicVariableInstructionService instructionService) {
        this.instructionService = instructionService;
    }

    /**
     * 获取工具名称。
     *
     * @return 工具名称
     */
    @Override
    public String name() {
        return DynamicVariableService.TOOL_NAME;
    }

    /**
     * 获取工具描述。
     *
     * @return 空字符串
     */
    @Override
    public String description() {
        return "";
    }

    /**
     * 获取无参数工具 Schema。
     *
     * @return 空对象参数 Schema
     */
    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "additionalProperties", false
        );
    }

    /**
     * 返回当前启用的动态变量名称和描述。
     *
     * @param arguments LLM 工具参数，当前工具不接收参数
     * @return 包含动态变量说明的工具结果
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String instruction = instructionService.buildExtractionInstruction();
        return ToolResult.success(instruction, Map.of("instruction", instruction));
    }
}

