package com.luzhiwen.collectagent.memory.application;

import com.luzhiwen.collectagent.memory.domain.AgentStage;
import com.luzhiwen.collectagent.memory.domain.AgentStatePolicy;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSessionStateEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 构建提供给 LLM 的会话状态视图。
 *
 * <p>视图只暴露已知槽位值、缺失槽位、只读标记位、可修改字段白名单和允许的阶段迁移，
 * 避免把数据库实体或完整状态直接交给模型。</p>
 */
@org.springframework.stereotype.Component
public class AgentStateViewBuilder {


    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建状态视图构建器。
     *
     * @param objectMapper JSON 序列化器
     */
    public AgentStateViewBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构建会话状态视图 JSON。
     *
     * @param state 会话状态实体
     * @return 状态视图 JSON
     */
    public String build(AgentSessionStateEntity state) {
        if (state == null) {
            return "{}";
        }
        ObjectNode stateRoot = readObjectSafely(state.getStateJson());
        ObjectNode view = objectMapper.createObjectNode();
        AgentStage stage = parseStage(state.getStage());
        view.put("stage", stage.name());
        view.set("known_slots", buildKnownSlots(stateRoot));
        view.set("missing_slots", buildMissingSlots(stage, stateRoot));
        view.set("flags", objectField(stateRoot, "flags").deepCopy());
        view.set("allowed_patch_fields", toArray(AgentStatePolicy.ALLOWED_PATCH_FIELDS));
        List<String> nextStages = AgentStatePolicy.allowedTransitions(stage)
                .stream()
                .map(Enum::name)
                .toList();
        view.set("allowed_stage_transitions", toArray(nextStages));
        return view.toString();
    }

    /**
     * 构建已知槽位视图，只输出槽位值，不暴露来源和置信度等内部字段。
     *
     * @param stateRoot 状态主体 JSON
     * @return 已知槽位对象
     */
    private ObjectNode buildKnownSlots(ObjectNode stateRoot) {
        ObjectNode result = objectMapper.createObjectNode();
        JsonNode slots = objectField(stateRoot, "slots");
        slots.properties().forEach(entry -> {
            JsonNode slot = entry.getValue();
            JsonNode value = slot.isObject() ? slot.path("value") : slot;
            if (!value.isMissingNode() && !value.isNull()) {
                result.set(entry.getKey(), value.deepCopy());
            }
        });
        return result;
    }

    /**
     * 根据阶段计算缺失槽位。
     *
     * @param stage 当前阶段
     * @param stateRoot 状态主体 JSON
     * @return 缺失槽位数组
     */
    private ArrayNode buildMissingSlots(AgentStage stage, ObjectNode stateRoot) {
        JsonNode slots = objectField(stateRoot, "slots");
        List<String> required = AgentStatePolicy.requiredSlots(stage);
        List<String> missing = new ArrayList<>();
        for (String slotName : required) {
            JsonNode slot = slots.path(slotName);
            JsonNode value = slot.isObject() ? slot.path("value") : slot;
            if (value.isMissingNode() || value.isNull() || value.asString("").isBlank()) {
                missing.add(slotName);
            }
        }
        return toArray(missing);
    }

    /**
     * 安全读取 JSON 对象。
     *
     * @param json JSON 文本
     * @return JSON 对象
     */
    private ObjectNode readObjectSafely(String json) {
        try {
            JsonNode root = objectMapper.readTree(json == null ? "{}" : json);
            return root instanceof ObjectNode objectNode ? objectNode : objectMapper.createObjectNode();
        } catch (JacksonException exception) {
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 获取或创建对象字段。
     *
     * @param parent 父对象
     * @param fieldName 字段名称
     * @return 对象字段
     */
    private ObjectNode objectField(ObjectNode parent, String fieldName) {
        JsonNode current = parent.get(fieldName);
        if (current instanceof ObjectNode objectNode) {
            return objectNode;
        }
        ObjectNode created = objectMapper.createObjectNode();
        parent.set(fieldName, created);
        return created;
    }

    /**
     * 将字符串列表转换为 JSON 数组。
     *
     * @param values 字符串列表
     * @return JSON 数组
     */
    private ArrayNode toArray(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }

    /**
     * 安全解析阶段。
     *
     * @param value 阶段文本
     * @return 阶段枚举
     */
    private AgentStage parseStage(String value) {
        try {
            return AgentStage.valueOf(value);
        } catch (Exception exception) {
            return AgentStage.OPENING;
        }
    }
}

