package com.luzhiwen.collectagent.tooling.application;

import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableValueResponse;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableDefinitionEntity;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableValueEntity;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository.DynamicVariableDefinitionRepository;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository.DynamicVariableValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话级动态变量值服务。
 */
@Service
public class DynamicVariableValueService {

    /** 变量值数据访问接口。 */
    private final DynamicVariableValueRepository valueRepository;

    /** 变量定义数据访问接口。 */
    private final DynamicVariableDefinitionRepository definitionRepository;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建动态变量值服务。
     *
     * @param valueRepository 变量值数据访问接口
     * @param definitionRepository 变量定义数据访问接口
     * @param objectMapper JSON 序列化器
     */
    public DynamicVariableValueService(
            DynamicVariableValueRepository valueRepository,
            DynamicVariableDefinitionRepository definitionRepository,
            ObjectMapper objectMapper
    ) {
        this.valueRepository = valueRepository;
        this.definitionRepository = definitionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询指定会话的动态变量值。
     *
     * @param sessionId 会话唯一标识
     * @return 动态变量值列表
     */
    @Transactional(readOnly = true)
    public List<DynamicVariableValueResponse> listValues(String sessionId) {
        List<DynamicVariableValueResponse> responses = new ArrayList<>();
        for (DynamicVariableValueEntity entity : valueRepository.findBySessionIdOrderByVariableKeyAsc(sessionId)) {
            responses.add(toResponse(entity));
        }
        return responses;
    }

    /**
     * 查询指定会话的动态变量 JSON。
     *
     * @param sessionId 会话唯一标识
     * @return 动态变量 JSON
     */
    public String findValuesJson(String sessionId) {
        Map<String, String> values = new LinkedHashMap<>();
        for (DynamicVariableValueEntity entity : valueRepository.findBySessionIdOrderByVariableKeyAsc(sessionId)) {
            values.put(entity.getVariableKey(), entity.getVariableValue());
        }
        return writeJson(values);
    }

    /**
     * 保存模型抽取出的动态变量值。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param sourceEventId 来源事件主键
     * @param extractedValues 抽取出的变量值
     * @param confidence 抽取置信度
     * @return 实际写入的变量值
     */
    @Transactional
    public Map<String, String> saveExtractedValues(
            String traceId,
            String sessionId,
            String customerId,
            Long sourceEventId,
            Map<String, String> extractedValues,
            Double confidence
    ) {
        Map<String, DynamicVariableDefinitionEntity> definitions = loadEnabledDefinitions();
        Map<String, String> savedValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : extractedValues.entrySet()) {
            if (!definitions.containsKey(entry.getKey()) || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            DynamicVariableValueEntity entity = valueRepository
                    .findBySessionIdAndVariableKey(sessionId, entry.getKey())
                    .orElseGet(DynamicVariableValueEntity::new);
            fillValue(entity, traceId, sessionId, customerId, sourceEventId, entry.getKey(), entry.getValue(), confidence);
            valueRepository.save(entity);
            savedValues.put(entry.getKey(), entry.getValue().trim());
        }
        return savedValues;
    }

    /**
     * 加载启用的变量定义。
     *
     * @return 变量键到定义的映射
     */
    private Map<String, DynamicVariableDefinitionEntity> loadEnabledDefinitions() {
        Map<String, DynamicVariableDefinitionEntity> definitions = new LinkedHashMap<>();
        for (DynamicVariableDefinitionEntity definition :
                definitionRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            definitions.put(definition.getVariableKey(), definition);
        }
        return definitions;
    }

    /**
     * 填充变量值实体。
     *
     * @param entity 变量值实体
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param sourceEventId 来源事件主键
     * @param key 变量键
     * @param value 变量值
     * @param confidence 置信度
     */
    private void fillValue(
            DynamicVariableValueEntity entity,
            String traceId,
            String sessionId,
            String customerId,
            Long sourceEventId,
            String key,
            String value,
            Double confidence
    ) {
        entity.setSessionId(sessionId);
        entity.setCustomerId(customerId);
        entity.setVariableKey(key);
        entity.setVariableValue(value.trim());
        entity.setSourceEventId(sourceEventId);
        entity.setTraceId(traceId);
        entity.setConfidence(confidence);
    }

    /**
     * 将变量值实体转换为响应对象。
     *
     * @param entity 变量值实体
     * @return 变量值响应
     */
    private DynamicVariableValueResponse toResponse(DynamicVariableValueEntity entity) {
        DynamicVariableValueResponse response = new DynamicVariableValueResponse();
        response.setSessionId(entity.getSessionId());
        response.setCustomerId(entity.getCustomerId());
        response.setVariableKey(entity.getVariableKey());
        response.setVariableValue(entity.getVariableValue());
        response.setSourceEventId(entity.getSourceEventId());
        response.setTraceId(entity.getTraceId());
        response.setConfidence(entity.getConfidence());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    /**
     * 将对象序列化为 JSON。
     *
     * @param value 待序列化对象
     * @return JSON 文本
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("动态变量 JSON 序列化失败", exception);
        }
    }
}
