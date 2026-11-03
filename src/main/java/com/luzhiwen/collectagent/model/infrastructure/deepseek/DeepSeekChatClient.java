package com.luzhiwen.collectagent.model.infrastructure.deepseek;

import com.luzhiwen.collectagent.model.application.LlmChatResult;
import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.observability.llm.LlmInteractionLogger;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek Chat Completions 客户端。
 */
@Component
public class DeepSeekChatClient {

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /** 请求工厂。 */
    private final DeepSeekRequestFactory requestFactory;

    /** 响应解析器。 */
    private final DeepSeekResponseParser responseParser;

    /** DeepSeek HTTP 客户端。 */
    private final RestClient restClient;

    /** 大模型交互日志器。 */
    private final LlmInteractionLogger interactionLogger;

    /**
     * 创建 DeepSeek 客户端。
     *
     * @param agentProperties Agent 配置
     * @param requestFactory 请求工厂
     * @param responseParser 响应解析器
     * @param interactionLogger 大模型交互日志器
     */
    public DeepSeekChatClient(
            AgentProperties agentProperties,
            DeepSeekRequestFactory requestFactory,
            DeepSeekResponseParser responseParser,
            LlmInteractionLogger interactionLogger
    ) {
        this.agentProperties = agentProperties;
        this.requestFactory = requestFactory;
        this.responseParser = responseParser;
        this.interactionLogger = interactionLogger;
        this.restClient = buildRestClient();
    }

    /**
     * 判断 API Key 是否已配置。
     *
     * @return true 表示已配置
     */
    public boolean isConfigured() {
        String apiKey = agentProperties.getLlm().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 使用默认自动工具选择调用模型。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param messages 聊天消息
     * @param tools 工具列表
     * @return 聊天结果
     */
    public LlmChatResult chat(
            String traceId,
            String sessionId,
            String customerId,
            List<LlmMessage> messages,
            List<AgentTool> tools
    ) {
        return chat(traceId, sessionId, customerId, messages, tools, null, false);
    }

    /**
     * 调用 DeepSeek Chat Completions。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param messages 聊天消息
     * @param tools 工具列表
     * @param forcedToolName 强制工具名称
     * @param jsonObject 是否要求 JSON 输出
     * @return 聊天结果
     */
    public LlmChatResult chat(
            String traceId,
            String sessionId,
            String customerId,
            List<LlmMessage> messages,
            List<AgentTool> tools,
            String forcedToolName,
            boolean jsonObject
    ) {
        requireConfigured();
        long startNanos = System.nanoTime();
        String responseJson = null;
        try {
            responseJson = execute(requestFactory.build(messages, tools, forcedToolName, jsonObject));
            LlmChatResult result = responseParser.parse(responseJson);
            log(traceId, sessionId, customerId, messages, responseJson, startNanos, null);
            return result;
        } catch (Exception exception) {
            log(traceId, sessionId, customerId, messages, responseJson, startNanos, exception.getMessage());
            throw exception;
        }
    }

    /**
     * 构建 DeepSeek RestClient。
     *
     * @return RestClient
     */
    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(agentProperties.getLlm().getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(agentProperties.getLlm().getTimeoutSeconds()));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(agentProperties.getLlm().getBaseUrl())
                .requestFactory(requestFactory);
        String apiKey = agentProperties.getLlm().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }

    /**
     * 执行 HTTP 请求。
     *
     * @param request 请求对象
     * @return 响应 JSON
     */
    private String execute(Map<String, Object> request) {
        return restClient.post()
                .uri("chat/completions")
                .header("Content-Type", "application/json")
                .body(requestFactory.serialize(request))
                .retrieve()
                .body(String.class);
    }

    /** 校验 API Key 配置。 */
    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("未配置 DeepSeek API Key，请设置 DEEPSEEK_API_KEY");
        }
    }

    /**
     * 记录模型交互日志。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param messages 聊天消息
     * @param response 响应 JSON
     * @param startNanos 开始时间
     * @param error 错误信息
     */
    private void log(
            String traceId,
            String sessionId,
            String customerId,
            List<LlmMessage> messages,
            String response,
            long startNanos,
            String error
    ) {
        interactionLogger.log(
                traceId,
                sessionId,
                customerId,
                messages,
                response,
                (System.nanoTime() - startNanos) / 1_000_000L,
                error
        );
    }
}
