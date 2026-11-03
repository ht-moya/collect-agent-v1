package com.luzhiwen.collectagent.eventing.application;

import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 原始事件载荷读取器。
 */
@Component
public class EventPayloadReader {

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建事件载荷读取器。
     *
     * @param objectMapper JSON 序列化器
     */
    public EventPayloadReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 读取事件 JSON 载荷。
     *
     * @param event 原始事件
     * @return JSON 节点，无法解析时返回空对象
     */
    public JsonNode read(EventLayerEntity event) {
        try {
            return objectMapper.readTree(event.getPayloadJson());
        } catch (JacksonException exception) {
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 读取事件载荷中的字符串字段。
     *
     * @param event 原始事件
     * @param field 字段名
     * @return 字段值，不存在时返回空字符串
     */
    public String text(EventLayerEntity event, String field) {
        return read(event).path(field).asString("");
    }
}
