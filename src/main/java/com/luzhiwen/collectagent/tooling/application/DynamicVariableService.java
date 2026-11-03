package com.luzhiwen.collectagent.tooling.application;

import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionRequest;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionResponse;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableToolConfigRequest;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableToolConfigResponse;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableValueResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 动态变量能力门面，统一委托给定义、配置、值和指令服务。
 */
@Service
public class DynamicVariableService {

    /** 动态变量工具名称。 */
    public static final String TOOL_NAME = "dynamic_variable_extract";

    /** 动态变量定义服务。 */
    private final DynamicVariableDefinitionService definitionService;

    /** 动态变量工具配置服务。 */
    private final DynamicVariableToolConfigService toolConfigService;

    /** 动态变量值服务。 */
    private final DynamicVariableValueService valueService;

    /** 动态变量指令服务。 */
    private final DynamicVariableInstructionService instructionService;

    /**
     * 创建动态变量门面服务。
     *
     * @param definitionService 定义服务
     * @param toolConfigService 工具配置服务
     * @param valueService 变量值服务
     * @param instructionService 指令服务
     */
    public DynamicVariableService(
            DynamicVariableDefinitionService definitionService,
            DynamicVariableToolConfigService toolConfigService,
            DynamicVariableValueService valueService,
            DynamicVariableInstructionService instructionService
    ) {
        this.definitionService = definitionService;
        this.toolConfigService = toolConfigService;
        this.valueService = valueService;
        this.instructionService = instructionService;
    }

    /** @return 动态变量定义列表。 */
    public List<DynamicVariableDefinitionResponse> listDefinitions() {
        return definitionService.listDefinitions();
    }

    /** @param request 定义请求 @return 创建结果。 */
    public DynamicVariableDefinitionResponse createDefinition(DynamicVariableDefinitionRequest request) {
        return definitionService.createDefinition(request);
    }

    /** @param id 定义主键 @param request 定义请求 @return 更新结果。 */
    public DynamicVariableDefinitionResponse updateDefinition(Long id, DynamicVariableDefinitionRequest request) {
        return definitionService.updateDefinition(id, request);
    }

    /** @param id 定义主键。 */
    public void deleteDefinition(Long id) {
        definitionService.deleteDefinition(id);
    }

    /** @return 工具配置。 */
    public DynamicVariableToolConfigResponse getToolConfigResponse() {
        return toolConfigService.getResponse();
    }

    /** @param request 工具配置请求 @return 更新结果。 */
    public DynamicVariableToolConfigResponse updateToolConfig(DynamicVariableToolConfigRequest request) {
        return toolConfigService.update(request);
    }

    /** @return true 表示需要抽取。 */
    public boolean shouldExtract() {
        return toolConfigService.shouldExtract();
    }

    /** @return 动态变量提取指令。 */
    public String buildExtractionInstruction() {
        return instructionService.buildExtractionInstruction();
    }

    /** @param sessionId 会话标识 @return 变量值列表。 */
    public List<DynamicVariableValueResponse> listValues(String sessionId) {
        return valueService.listValues(sessionId);
    }

    /** @param sessionId 会话标识 @return 动态变量 JSON。 */
    public String findValuesJson(String sessionId) {
        return valueService.findValuesJson(sessionId);
    }

    /** @param traceId 链路标识 @param sessionId 会话标识 @param customerId 客户标识 @param sourceEventId 来源事件 @param values 变量值 @param confidence 置信度 @return 实际保存值。 */
    public Map<String, String> saveExtractedValues(
            String traceId,
            String sessionId,
            String customerId,
            Long sourceEventId,
            Map<String, String> values,
            Double confidence
    ) {
        return valueService.saveExtractedValues(
                traceId,
                sessionId,
                customerId,
                sourceEventId,
                values,
                confidence
        );
    }
}
