package com.luzhiwen.collectagent.model.infrastructure.deepseek;

import com.luzhiwen.collectagent.model.application.LlmChatResult;
import com.luzhiwen.collectagent.model.application.LlmToolCall;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek Chat Completions 响应解析器。
 */
@Component
public class DeepSeekResponseParser {

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建响应解析器。
     *
     * @param objectMapper JSON 序列化器
     */
    public DeepSeekResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 DeepSeek 响应。
     *
     * @param responseJson 原始响应 JSON
     * @return 聊天结果
     */
    public LlmChatResult parse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode message = root.path("choices").path(0).path("message");
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                return new LlmChatResult(
                        responseJson,
                        message.path("content").asString(""),
                        parseToolCalls(toolCalls)
                );
            }
            String content = message.path("content").asString();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("DeepSeek 响应缺少 content 和 tool_calls");
            }
            return new LlmChatResult(responseJson, content, List.of());
        } catch (JacksonException exception) {
            throw new IllegalStateException("解析 DeepSeek 聊天响应失败", exception);
        }
    }

    /**
     * 解析全部工具调用。
     *
     * @param toolCalls 工具调用节点
     * @return 工具调用列表
     */
    private List<LlmToolCall> parseToolCalls(JsonNode toolCalls) {
        List<LlmToolCall> result = new ArrayList<>();
        for (JsonNode toolCall : toolCalls) {
            JsonNode function = toolCall.path("function");
            result.add(new LlmToolCall(
                    toolCall.path("id").asString(),
                    function.path("name").asString(),
                    function.path("arguments").asString("{}")
            ));
        }
        return result;
    }
}
