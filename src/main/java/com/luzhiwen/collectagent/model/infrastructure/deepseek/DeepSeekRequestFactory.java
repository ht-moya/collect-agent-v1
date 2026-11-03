package com.luzhiwen.collectagent.model.infrastructure.deepseek;

import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.model.application.LlmToolCall;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek Chat Completions 请求工厂。
 */
@Component
public class DeepSeekRequestFactory {

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建 DeepSeek 请求工厂。
     *
     * @param agentProperties Agent 配置
     * @param objectMapper JSON 序列化器
     */
    public DeepSeekRequestFactory(AgentProperties agentProperties, ObjectMapper objectMapper) {
        this.agentProperties = agentProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 构造 DeepSeek 请求对象。
     *
     * @param messages 聊天消息
     * @param tools 可用工具
     * @param forcedToolName 强制工具名称
     * @param jsonObject 是否要求 JSON 输出
     * @return 请求对象
     */
    public Map<String, Object> build(
            List<LlmMessage> messages,
            List<AgentTool> tools,
            String forcedToolName,
            boolean jsonObject
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", agentProperties.getLlm().getChatModel());
        request.put("messages", buildMessagePayload(messages));
        request.put("temperature", agentProperties.getLlm().getTemperature());
        if (tools != null && !tools.isEmpty()) {
            request.put("tools", buildToolPayloads(tools));
            request.put("tool_choice", buildToolChoice(forcedToolName));
        }
        if (jsonObject) {
            request.put("response_format", Map.of("type", "json_object"));
        }
        return request;
    }

    /**
     * 将请求对象序列化为 JSON。
     *
     * @param value 请求对象
     * @return JSON 文本
     */
    public String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("构造 DeepSeek 请求 JSON 失败", exception);
        }
    }

    /**
     * 构建消息请求结构。
     *
     * @param messages 聊天消息
     * @return 消息列表
     */
    private List<Map<String, Object>> buildMessagePayload(List<LlmMessage> messages) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (LlmMessage message : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            if (message.getToolCallId() != null && !message.getToolCallId().isBlank()) {
                item.put("tool_call_id", message.getToolCallId());
            }
            item.put("content", message.getContent());
            if (!message.getToolCalls().isEmpty()) {
                item.put("tool_calls", buildMessageToolCalls(message));
            }
            payload.add(item);
        }
        return payload;
    }

    /**
     * 将工具注册信息转换为 tools 请求结构。
     *
     * @param tools 工具列表
     * @return tools 请求结构
     */
    private List<Map<String, Object>> buildToolPayloads(List<AgentTool> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentTool tool : tools) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parameters());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "function");
            item.put("function", function);
            result.add(item);
        }
        return result;
    }

    /**
     * 将 assistant 消息中的工具调用转换为请求结构。
     *
     * @param message assistant 消息
     * @return tool_calls 请求结构
     */
    private List<Map<String, Object>> buildMessageToolCalls(LlmMessage message) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (LlmToolCall toolCall : message.getToolCalls()) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", toolCall.getToolName());
            function.put("arguments", toolCall.getArgumentsJson());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", toolCall.getToolCallId());
            item.put("type", "function");
            item.put("function", function);
            result.add(item);
        }
        return result;
    }

    /**
     * 构建工具选择参数。
     *
     * @param forcedToolName 强制工具名称
     * @return auto 或指定函数对象
     */
    private Object buildToolChoice(String forcedToolName) {
        if (forcedToolName == null || forcedToolName.isBlank()) {
            return "auto";
        }
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", forcedToolName);
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("type", "function");
        choice.put("function", function);
        return choice;
    }
}
