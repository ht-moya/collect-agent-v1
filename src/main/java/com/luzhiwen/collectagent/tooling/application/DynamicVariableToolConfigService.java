package com.luzhiwen.collectagent.tooling.application;

import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableToolConfigRequest;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableToolConfigResponse;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableToolConfigEntity;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository.DynamicVariableDefinitionRepository;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository.DynamicVariableToolConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 动态变量工具配置服务。
 */
@Service
public class DynamicVariableToolConfigService {

    /** 默认工具描述。 */
    private static final String DEFAULT_TOOL_DESCRIPTION =
            "从当前用户消息中提取明确陈述的动态变量，不得推测、补全或编造。";

    /** 工具配置数据访问接口。 */
    private final DynamicVariableToolConfigRepository toolConfigRepository;

    /** 变量定义数据访问接口。 */
    private final DynamicVariableDefinitionRepository definitionRepository;

    /**
     * 创建动态变量工具配置服务。
     *
     * @param toolConfigRepository 工具配置数据访问接口
     * @param definitionRepository 变量定义数据访问接口
     */
    public DynamicVariableToolConfigService(
            DynamicVariableToolConfigRepository toolConfigRepository,
            DynamicVariableDefinitionRepository definitionRepository
    ) {
        this.toolConfigRepository = toolConfigRepository;
        this.definitionRepository = definitionRepository;
    }

    /**
     * 查询或创建工具配置。
     *
     * @return 工具配置实体
     */
    @Transactional
    public DynamicVariableToolConfigEntity getOrCreateEntity() {
        return toolConfigRepository.findByToolName(DynamicVariableService.TOOL_NAME)
                .orElseGet(() -> toolConfigRepository.save(new DynamicVariableToolConfigEntity(
                        DynamicVariableService.TOOL_NAME,
                        DEFAULT_TOOL_DESCRIPTION,
                        true,
                        true
                )));
    }

    /**
     * 查询工具配置响应。
     *
     * @return 工具配置响应
     */
    public DynamicVariableToolConfigResponse getResponse() {
        return toResponse(getOrCreateEntity());
    }

    /**
     * 更新工具配置。
     *
     * @param request 工具配置请求
     * @return 更新后的配置
     */
    @Transactional
    public DynamicVariableToolConfigResponse update(DynamicVariableToolConfigRequest request) {
        DynamicVariableToolConfigEntity entity = getOrCreateEntity();
        String description = request.getDescription();
        entity.setDescription(description == null || description.isBlank()
                ? DEFAULT_TOOL_DESCRIPTION
                : description.trim());
        entity.setForceCall(request.isForceCall());
        entity.setEnabled(request.isEnabled());
        return toResponse(toolConfigRepository.save(entity));
    }

    /**
     * 判断是否应该执行动态变量抽取。
     *
     * @return true 表示需要抽取
     */
    public boolean shouldExtract() {
        DynamicVariableToolConfigEntity config = getOrCreateEntity();
        return config.isEnabled()
                && config.isForceCall()
                && !definitionRepository.findByEnabledTrueOrderBySortOrderAscIdAsc().isEmpty();
    }

    /**
     * 获取工具基础描述。
     *
     * @return 工具描述
     */
    public String buildToolDescription() {
        return getOrCreateEntity().getDescription();
    }

    /**
     * 将配置实体转换为响应对象。
     *
     * @param entity 配置实体
     * @return 配置响应
     */
    private DynamicVariableToolConfigResponse toResponse(DynamicVariableToolConfigEntity entity) {
        DynamicVariableToolConfigResponse response = new DynamicVariableToolConfigResponse();
        response.setId(entity.getId());
        response.setToolName(entity.getToolName());
        response.setDescription(entity.getDescription());
        response.setForceCall(entity.isForceCall());
        response.setEnabled(entity.isEnabled());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
