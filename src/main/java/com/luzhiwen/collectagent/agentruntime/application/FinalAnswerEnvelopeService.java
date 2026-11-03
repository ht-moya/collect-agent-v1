package com.luzhiwen.collectagent.agentruntime.application;

import com.luzhiwen.collectagent.agentruntime.domain.FinalAnswerPayload;
import com.luzhiwen.collectagent.memory.domain.AgentSlotPatch;
import com.luzhiwen.collectagent.memory.domain.AgentStatePatch;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析最终 JSON 信封并持久化动态变量。
 *
 * <p>兼容新格式 reply + state_patch，以及旧动态变量格式 answer + variableMap。</p>
 */
@Service
public class FinalAnswerEnvelopeService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(FinalAnswerEnvelopeService.class);

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 动态变量服务。 */
    private final DynamicVariableService dynamicVariableService;

    /**
     * 创建最终回答信封服务。
     *
     * @param objectMapper JSON 序列化器
     * @param dynamicVariableService 动态变量服务
     */
    public FinalAnswerEnvelopeService(
            ObjectMapper objectMapper,
            DynamicVariableService dynamicVariableService
    ) {
        this.objectMapper = objectMapper;
        this.dynamicVariableService = dynamicVariableService;
    }

    /**
     * 解析最终回答并按需写入动态变量。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param sourceEventId 当前用户事件主键
     * @param content 模型最终内容
     * @return 最终回答载荷
     */
    public FinalAnswerPayload parseAndPersist(
            String traceId,
            String sessionId,
            String customerId,
            Long sourceEventId,
            String content
    ) {
        String json = stripCodeFence(content);
        if (!json.startsWith("{")) {
            return fallback(content);
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return fallback(content);
            }
            String answer = readAnswer(root);
            if (answer == null) {
                return fallback(content);
            }
            Map<String, String> variableMap = readVariableMap(root.path("variableMap"));
            if (!variableMap.isEmpty()) {
                dynamicVariableService.saveExtractedValues(
                        traceId,
                        sessionId,
                        customerId,
                        sourceEventId,
                        variableMap,
                        null
                );
            }
            AgentStatePatch statePatch = readStatePatch(root.path("state_patch"));
            return new FinalAnswerPayload(variableMap, answer, statePatch);
        } catch (JacksonException exception) {
            LOGGER.warn("最终 JSON 信封解析失败，回退为普通文本，sessionId={}", sessionId, exception);
            return fallback(content);
        }
    }

    /**
     * 读取最终业务回复，优先支持 reply，再兼容旧 answer。
     *
     * @param root 响应 JSON
     * @return 业务回复；不存在时返回 null
     */
    private String readAnswer(JsonNode root) {
        if (root.path("reply").isTextual()) {
            return root.path("reply").asString("");
        }
        if (root.path("answer").isTextual()) {
            return root.path("answer").asString("");
        }
        return null;
    }

    /**
     * 解析动态变量映射。
     *
     * @param variableMapNode 变量映射节点
     * @return 变量映射
     */
    private Map<String, String> readVariableMap(JsonNode variableMapNode) {
        Map<String, String> variableMap = new LinkedHashMap<>();
        if (!variableMapNode.isObject()) {
            return variableMap;
        }
        variableMapNode.properties().forEach(entry ->
                variableMap.put(entry.getKey(), entry.getValue().asString(""))
        );
        return variableMap;
    }

    /**
     * 解析 LLM 状态补丁。
     *
     * @param patchNode 状态补丁节点
     * @return 状态补丁
     */
    private AgentStatePatch readStatePatch(JsonNode patchNode) {
        AgentStatePatch patch = new AgentStatePatch();
        if (!patchNode.isObject()) {
            return patch;
        }
        patch.setStage(patchNode.path("stage").asString(""));
        JsonNode slotsNode = patchNode.path("slots");
        if (!slotsNode.isObject()) {
            return patch;
        }
        Map<String, AgentSlotPatch> slots = new LinkedHashMap<>();
        slotsNode.properties().forEach(entry -> {
            JsonNode slotNode = entry.getValue();
            if (slotNode.isObject() && slotNode.has("value")) {
                slots.put(entry.getKey(), new AgentSlotPatch(
                        toJavaValue(slotNode.path("value")),
                        slotNode.path("confidence").isNumber() ? slotNode.path("confidence").asDouble() : null
                ));
                return;
            }
            slots.put(entry.getKey(), new AgentSlotPatch(toJavaValue(slotNode), null));
        });
        patch.setSlots(slots);
        return patch;
    }

    /**
     * 将 JSON 值转换为 Java 标量。
     *
     * @param node JSON 节点
     * @return Java 标量
     */
    private Object toJavaValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isNumber()) {
            return new BigDecimal(node.asString());
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return node.asString("");
    }

    /**
     * 构建普通文本回退结果。
     *
     * @param content 原始内容
     * @return 最终回答载荷
     */
    private FinalAnswerPayload fallback(String content) {
        return new FinalAnswerPayload(Map.of(), content == null ? "" : content);
    }

    /**
     * 去除 Markdown 代码围栏。
     *
     * @param content 原始内容
     * @return 纯 JSON 文本
     */
    private String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
