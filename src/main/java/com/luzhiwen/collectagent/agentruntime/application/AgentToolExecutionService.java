package com.luzhiwen.collectagent.agentruntime.application;

import com.luzhiwen.collectagent.agentruntime.domain.ToolCallAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolObservation;
import com.luzhiwen.collectagent.eventing.application.EventAppendService;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.application.AgentSessionStateService;
import com.luzhiwen.collectagent.memory.application.ShortTermMemoryService;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.tooling.application.ToolRegistry;
import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import com.luzhiwen.collectagent.tooling.domain.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次 Agent 工具执行服务，负责执行工具并记录事件。
 */
@Service
public class AgentToolExecutionService {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentToolExecutionService.class);

    /** 原始事件追加服务。 */
    private final EventAppendService eventAppendService;

    /** Agent 会话状态服务。 */
    private final AgentSessionStateService stateService;

    /** 短期记忆缓存投影服务。 */
    private final ShortTermMemoryService shortTermMemoryService;

    /** 工具注册中心。 */
    private final ToolRegistry toolRegistry;

    /**
     * 创建工具执行服务。
     *
     * @param eventAppendService 原始事件追加服务
     * @param stateService Agent 会话状态服务
     * @param shortTermMemoryService 短期记忆缓存投影服务
     * @param toolRegistry 工具注册中心
     */
    public AgentToolExecutionService(
            EventAppendService eventAppendService,
            AgentSessionStateService stateService,
            ShortTermMemoryService shortTermMemoryService,
            ToolRegistry toolRegistry
    ) {
        this.eventAppendService = eventAppendService;
        this.stateService = stateService;
        this.shortTermMemoryService = shortTermMemoryService;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行工具并写入调用事件、结果事件和短期缓存投影。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param sourceEventId 当前用户事件主键
     * @param action 工具动作
     * @return 工具观察结果
     */
    public ToolObservation execute(
            String traceId,
            String sessionId,
            String customerId,
            Long sourceEventId,
            ToolCallAction action
    ) {
        // 先记录工具调用事件，开始执行后才记录结果事件。
        appendToolCallEvent(traceId, sessionId, customerId, action);
        long startNanos = System.nanoTime();
        // 执行工具并把异常转换为失败结果，避免中断 Agent 主循环。
        ToolResult result = invokeTool(traceId, sessionId, customerId, sourceEventId, action);
        long latencyMs = elapsedMillis(startNanos);
        appendToolResultEvent(traceId, sessionId, customerId, action, result, latencyMs);
        return new ToolObservation(
                action.getToolCallId(),
                action.getToolName(),
                result.isSuccess(),
                result.getMessage(),
                latencyMs
        );
    }

    /**
     * 追加工具调用事件并刷新状态与短期缓存。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param action 工具动作
     */
    private void appendToolCallEvent(
            String traceId,
            String sessionId,
            String customerId,
            ToolCallAction action
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolCallId", action.getToolCallId());
        payload.put("toolName", action.getToolName());
        payload.put("args", action.getArguments());
        payload.put("reason", action.getReason());
        EventLayerEntity event = eventAppendService.append(
                traceId, sessionId, customerId, EventType.TOOL_CALL, payload
        );
        stateService.applyEvent(event);
        shortTermMemoryService.refresh(sessionId, customerId);
    }

    /**
     * 执行实际工具。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param sourceEventId 当前用户事件主键
     * @param action 工具动作
     * @return 工具结果
     */
    private ToolResult invokeTool(
            String traceId,
            String sessionId,
            String customerId,
            Long sourceEventId,
            ToolCallAction action
    ) {
        try {
            AgentTool tool = toolRegistry.getRequiredTool(action.getToolName());
            return tool.execute(withRuntimeContext(traceId, sessionId, customerId, sourceEventId, action));
        } catch (Exception exception) {
            LOGGER.warn("工具执行失败，tool={}", action.getToolName(), exception);
            return ToolResult.failure("工具执行失败：" + exception.getMessage());
        }
    }

    /**
     * 为动态变量工具注入运行时上下文。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param sourceEventId 当前用户事件主键
     * @param action 工具动作
     * @return 注入后的参数
     */
    private Map<String, Object> withRuntimeContext(
            String traceId,
            String sessionId,
            String customerId,
            Long sourceEventId,
            ToolCallAction action
    ) {
        Map<String, Object> arguments = new LinkedHashMap<>(action.getArguments());
        if (DynamicVariableService.TOOL_NAME.equals(action.getToolName())) {
            arguments.put("_traceId", traceId);
            arguments.put("_sessionId", sessionId);
            arguments.put("_customerId", customerId);
            arguments.put("_sourceEventId", sourceEventId);
        }
        return arguments;
    }

    /**
     * 追加工具结果事件并刷新状态与短期缓存。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param action 工具动作
     * @param result 工具结果
     * @param latencyMs 工具执行耗时
     */
    private void appendToolResultEvent(
            String traceId,
            String sessionId,
            String customerId,
            ToolCallAction action,
            ToolResult result,
            long latencyMs
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolCallId", action.getToolCallId());
        payload.put("toolName", action.getToolName());
        payload.put("success", result.isSuccess());
        payload.put("result", result.getMessage());
        payload.put("latencyMs", latencyMs);
        payload.put("data", result.getData());
        EventLayerEntity event = eventAppendService.append(
                traceId, sessionId, customerId, EventType.TOOL_RESULT, payload
        );
        stateService.applyEvent(event);
        shortTermMemoryService.refresh(sessionId, customerId);
    }

    /**
     * 计算耗时毫秒数。
     *
     * @param startNanos 开始时间纳秒值
     * @return 耗时毫秒数
     */
    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
