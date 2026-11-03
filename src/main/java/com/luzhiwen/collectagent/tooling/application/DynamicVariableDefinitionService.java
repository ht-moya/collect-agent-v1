package com.luzhiwen.collectagent.tooling.application;

import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionRequest;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionResponse;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableDefinitionEntity;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository.DynamicVariableDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 动态变量定义管理服务。
 */
@Service
public class DynamicVariableDefinitionService {

    /** 合法变量键正则表达式。 */
    private static final Pattern VARIABLE_KEY_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** 动态变量定义数据访问接口。 */
    private final DynamicVariableDefinitionRepository definitionRepository;

    /**
     * 创建动态变量定义服务。
     *
     * @param definitionRepository 动态变量定义数据访问接口
     */
    public DynamicVariableDefinitionService(DynamicVariableDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    /**
     * 查询全部动态变量定义。
     *
     * @return 动态变量定义列表
     */
    @Transactional(readOnly = true)
    public List<DynamicVariableDefinitionResponse> listDefinitions() {
        List<DynamicVariableDefinitionResponse> responses = new ArrayList<>();
        for (DynamicVariableDefinitionEntity entity : definitionRepository.findAllByOrderBySortOrderAscIdAsc()) {
            responses.add(toResponse(entity));
        }
        return responses;
    }

    /**
     * 创建动态变量定义。
     *
     * @param request 保存请求
     * @return 创建后的定义
     */
    @Transactional
    public DynamicVariableDefinitionResponse createDefinition(DynamicVariableDefinitionRequest request) {
        String variableKey = normalizeVariableKey(request.getVariableKey());
        if (definitionRepository.findByVariableKey(variableKey).isPresent()) {
            throw new IllegalArgumentException("变量键已存在：" + variableKey);
        }
        DynamicVariableDefinitionEntity entity = new DynamicVariableDefinitionEntity();
        applyRequest(entity, request, variableKey);
        return toResponse(definitionRepository.save(entity));
    }

    /**
     * 更新动态变量定义。
     *
     * @param id 定义主键
     * @param request 保存请求
     * @return 更新后的定义
     */
    @Transactional
    public DynamicVariableDefinitionResponse updateDefinition(Long id, DynamicVariableDefinitionRequest request) {
        DynamicVariableDefinitionEntity entity = definitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("动态变量不存在：" + id));
        String variableKey = normalizeVariableKey(request.getVariableKey());
        definitionRepository.findByVariableKey(variableKey)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("变量键已存在：" + variableKey);
                });
        applyRequest(entity, request, variableKey);
        return toResponse(definitionRepository.save(entity));
    }

    /**
     * 删除动态变量定义。
     *
     * @param id 定义主键
     */
    @Transactional
    public void deleteDefinition(Long id) {
        if (!definitionRepository.existsById(id)) {
            throw new IllegalArgumentException("动态变量不存在：" + id);
        }
        definitionRepository.deleteById(id);
    }

    /**
     * 查询启用的变量定义实体。
     *
     * @return 启用的变量定义
     */
    public List<DynamicVariableDefinitionEntity> listEnabledEntities() {
        return definitionRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    /**
     * 规范化变量键。
     *
     * @param variableKey 原始变量键
     * @return 规范化变量键
     */
    private String normalizeVariableKey(String variableKey) {
        if (variableKey == null || !VARIABLE_KEY_PATTERN.matcher(variableKey.trim()).matches()) {
            throw new IllegalArgumentException("变量键必须由字母、数字或下划线组成，且不能以数字开头");
        }
        return variableKey.trim();
    }

    /**
     * 将请求应用到变量定义实体。
     *
     * @param entity 变量定义实体
     * @param request 保存请求
     * @param variableKey 规范化变量键
     */
    private void applyRequest(
            DynamicVariableDefinitionEntity entity,
            DynamicVariableDefinitionRequest request,
            String variableKey
    ) {
        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new IllegalArgumentException("变量展示名称不能为空");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("变量用途说明不能为空");
        }
        entity.setVariableKey(variableKey);
        entity.setDisplayName(request.getDisplayName().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setExtractionHint(request.getExtractionHint() == null
                ? null
                : request.getExtractionHint().trim());
        entity.setEnabled(request.isEnabled());
        entity.setSortOrder(request.getSortOrder());
    }

    /**
     * 将定义实体转换为响应对象。
     *
     * @param entity 定义实体
     * @return 响应对象
     */
    private DynamicVariableDefinitionResponse toResponse(DynamicVariableDefinitionEntity entity) {
        DynamicVariableDefinitionResponse response = new DynamicVariableDefinitionResponse();
        response.setId(entity.getId());
        response.setVariableKey(entity.getVariableKey());
        response.setDisplayName(entity.getDisplayName());
        response.setDescription(entity.getDescription());
        response.setExtractionHint(entity.getExtractionHint());
        response.setEnabled(entity.isEnabled());
        response.setSortOrder(entity.getSortOrder());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
