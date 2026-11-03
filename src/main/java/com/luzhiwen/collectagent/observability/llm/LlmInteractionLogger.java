package com.luzhiwen.collectagent.observability.llm;

import com.luzhiwen.collectagent.model.application.LlmMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型交互日志器。
 *
 * <p>当前版本只记录发送给模型的 messages 和模型回复。</p>
 */
@Component
public class LlmInteractionLogger {

    /** 专用大模型日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger("LLM_INTERACTION");

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建大模型交互日志器。
     *
     * @param objectMapper JSON 序列化器
     */
    public LlmInteractionLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一次大模型调用。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param messages 发送给模型的消息
     * @param response 模型回复
     * @param latencyMs 调用耗时
     * @param error 错误信息，成功时为空
     */
    public void log(
            String traceId,
            String sessionId,
            String customerId,
            List<LlmMessage> messages,
            String response,
            long latencyMs,
            String error
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("traceId", traceId);
        event.put("sessionId", sessionId);
        event.put("customerId", customerId);
        event.put("latencyMs", latencyMs);
        event.put("messages", toMessages(messages));
        event.put("response", response);
        event.put("error", error);
        try {
            LOGGER.info(objectMapper.writeValueAsString(event));
        } catch (JacksonException exception) {
            LOGGER.error("大模型交互日志序列化失败", exception);
        }
    }

    /**
     * 将消息转换为日志结构。
     *
     * @param messages 模型消息
     * @return 消息 Map 列表
     */
    private List<Map<String, Object>> toMessages(List<LlmMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (LlmMessage message : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            if (message.getToolCallId() != null && !message.getToolCallId().isBlank()) {
                item.put("tool_call_id", message.getToolCallId());
            }
            if (!message.getToolCalls().isEmpty()) {
                item.put("tool_calls", message.getToolCalls());
            }
            result.add(item);
        }
        return result;
    }
}
