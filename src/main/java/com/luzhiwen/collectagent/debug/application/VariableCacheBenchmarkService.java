package com.luzhiwen.collectagent.debug.application;

import com.luzhiwen.collectagent.debug.api.dto.VariableCacheBenchmarkRequest;
import com.luzhiwen.collectagent.debug.api.dto.VariableCacheBenchmarkResponse;
import com.luzhiwen.collectagent.debug.api.dto.VariableCacheBenchmarkSample;
import com.luzhiwen.collectagent.model.application.LlmChatResult;
import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.model.application.LlmToolCall;
import com.luzhiwen.collectagent.model.infrastructure.deepseek.DeepSeekChatClient;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableInstructionService;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.DynamicVariableExtractionTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 动态变量缓存命中 A/B 基准测试服务。
 */
@Service
public class VariableCacheBenchmarkService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(VariableCacheBenchmarkService.class);

    /** DeepSeek 客户端。 */
    private final DeepSeekChatClient deepSeekChatClient;

    /** 动态变量指令服务。 */
    private final DynamicVariableInstructionService instructionService;

    /** 动态变量抽取工具。 */
    private final DynamicVariableExtractionTool dynamicVariableExtractionTool;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 固定规则文本，重复多次以放大系统提示词变化的影响。 */
    private static final String STABLE_RULE = "固定规则：只依据已提供的变量定义回答，禁止推测，禁止调用工具，回复简洁。";

    /**
     * 创建动态变量缓存基准测试服务。
     */
    public VariableCacheBenchmarkService(
            DeepSeekChatClient deepSeekChatClient,
            DynamicVariableInstructionService instructionService,
            DynamicVariableExtractionTool dynamicVariableExtractionTool,
            ObjectMapper objectMapper
    ) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.instructionService = instructionService;
        this.dynamicVariableExtractionTool = dynamicVariableExtractionTool;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行指定变体的缓存测试。
     *
     * @param request 基准测试请求
     * @return 基准测试结果
     */
    public VariableCacheBenchmarkResponse run(VariableCacheBenchmarkRequest request) {
        String variant = normalizeVariant(request.getVariant());
        int iterations = Math.max(1, Math.min(request.getIterations(), 20));
        long intervalMs = Math.max(0L, Math.min(request.getIntervalMs(), 5000L));
        String runId = request.getRunId() == null || request.getRunId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : request.getRunId().trim();
        String instruction = instructionService.buildExtractionInstruction();
        String userMessage = request.getUserMessage() == null || request.getUserMessage().isBlank()
                ? "你好"
                : request.getUserMessage().trim();
        List<LlmMessage> messages = buildMessages(variant, runId, instruction, userMessage);
        VariableCacheBenchmarkResponse response = new VariableCacheBenchmarkResponse();
        response.setVariant(variant);
        response.setRunId(runId);
        response.setVariableFingerprint(fingerprint(instruction));
        int totalPromptTokens = 0;
        int totalCachedTokens = 0;
        for (int iteration = 1; iteration <= iterations; iteration++) {
            VariableCacheBenchmarkSample sample = executeOnce(runId, iteration, messages);
            response.getSamples().add(sample);
            totalPromptTokens += sample.getPromptTokens();
            totalCachedTokens += sample.getCachedTokens();
            if (iteration < iterations && intervalMs > 0) {
                sleep(intervalMs);
            }
        }
        response.setTotalPromptTokens(totalPromptTokens);
        response.setTotalCachedTokens(totalCachedTokens);
        response.setAverageCacheHitRate(totalPromptTokens == 0
                ? 0.0D
                : (double) totalCachedTokens / totalPromptTokens);
        LOGGER.info("动态变量缓存 A/B 测试完成：variant={}，runId={}，iterations={}，avgCacheHitRate={}",
                variant, runId, iterations, response.getAverageCacheHitRate());
        return response;
    }

    /**
     * 执行一次模型调用并解析缓存 Token。
     */
    private VariableCacheBenchmarkSample executeOnce(String runId, int iteration, List<LlmMessage> messages) {
        long startNanos = System.nanoTime();
        LlmChatResult result = deepSeekChatClient.chat(
                "cache-benchmark-" + runId + "-" + iteration,
                "cache-benchmark-" + runId,
                "benchmark",
                messages,
                List.of(dynamicVariableExtractionTool),
                null,
                false
        );
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
        JsonNode usage = readUsage(result.getRawResponse());
        int promptTokens = usage.path("prompt_tokens").asInt();
        int cachedTokens = usage.path("prompt_cache_hit_tokens")
                .asInt(usage.path("prompt_tokens_details").path("cached_tokens").asInt(0));
        int cacheMissTokens = Math.max(0, promptTokens - cachedTokens);
        VariableCacheBenchmarkSample sample = new VariableCacheBenchmarkSample();
        sample.setIteration(iteration);
        sample.setPromptTokens(promptTokens);
        sample.setCachedTokens(cachedTokens);
        sample.setCacheMissTokens(cacheMissTokens);
        sample.setCacheHitRate(promptTokens == 0 ? 0.0D : (double) cachedTokens / promptTokens);
        sample.setLatencyMs(latencyMs);
        return sample;
    }

    /**
     * 构造 A/B 两种请求消息。
     */
    private List<LlmMessage> buildMessages(
            String variant,
            String runId,
            String instruction,
            String userMessage
    ) {
        List<LlmMessage> messages = new ArrayList<>();
        String system = "你是动态变量缓存基准测试助手。\n本次运行标识：" + runId + "\n"
                + ("B".equals(variant)
                ? "动态变量定义（位于系统提示词前部，变化后会使后续固定规则失去前缀缓存）：\n" + instruction + "\n"
                : "")
                + stableRules();
        messages.add(new LlmMessage("system", system));
        messages.add(new LlmMessage("user", "请确认当前动态变量定义已经加载。"));
        String toolCallId = "call_cache_" + runId;
        messages.add(new LlmMessage(
                "assistant",
                null,
                null,
                List.of(new LlmToolCall(toolCallId, DynamicVariableService.TOOL_NAME, "{}"))
        ));
        String toolResult = "A".equals(variant)
                ? "请根据以下动态变量定义执行提取：\n" + instruction
                : "动态变量定义已写入系统提示词，无需再次加载。";
        messages.add(new LlmMessage("tool", toolResult, toolCallId, List.of()));
        messages.add(new LlmMessage("user", userMessage));
        return messages;
    }

    /**
     * 构造固定规则文本，用于放大变量插入位置对前缀缓存的影响。
     */
    private String stableRules() {
        StringBuilder builder = new StringBuilder("后续固定规则：\n");
        for (int index = 0; index < 8; index++) {
            builder.append(index + 1).append(". ").append(STABLE_RULE).append('\n');
        }
        return builder.toString();
    }

    /**
     * 解析模型响应使用量。
     */
    private JsonNode readUsage(String responseJson) {
        try {
            return objectMapper.readTree(responseJson).path("usage");
        } catch (Exception exception) {
            throw new IllegalStateException("解析缓存基准响应失败", exception);
        }
    }

    /**
     * 规范化并校验测试变体。
     */
    private String normalizeVariant(String variant) {
        String normalized = variant == null ? "" : variant.trim().toUpperCase(Locale.ROOT);
        if (!"A".equals(normalized) && !"B".equals(normalized)) {
            throw new IllegalArgumentException("variant 必须是 A 或 B");
        }
        return normalized;
    }

    /**
     * 计算变量定义指纹。
     */
    private String fingerprint(String instruction) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(instruction.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception exception) {
            throw new IllegalStateException("计算变量指纹失败", exception);
        }
    }

    /**
     * 等待指定毫秒数。
     */
    private void sleep(long intervalMs) {
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("缓存基准测试被中断", exception);
        }
    }
}
