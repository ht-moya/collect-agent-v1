package com.luzhiwen.collectagent.agentruntime.application;

import com.luzhiwen.collectagent.agentruntime.domain.AgentAction;
import com.luzhiwen.collectagent.agentruntime.domain.AgentContext;
import com.luzhiwen.collectagent.agentruntime.domain.FinalAnswerAction;
import com.luzhiwen.collectagent.agentruntime.domain.FinalAnswerPayload;
import com.luzhiwen.collectagent.agentruntime.domain.ToolCallAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolCallBatchAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolObservation;
import com.luzhiwen.collectagent.agentruntime.planner.AgentPlanner;
import com.luzhiwen.collectagent.conversation.application.SessionClosedException;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatRequest;
import com.luzhiwen.collectagent.conversation.api.dto.AgentChatResponse;
import com.luzhiwen.collectagent.conversation.api.dto.ToolCallView;
import com.luzhiwen.collectagent.eventing.application.EventAppendService;
import com.luzhiwen.collectagent.eventing.domain.EventType;
import com.luzhiwen.collectagent.eventing.infrastructure.persistence.entity.EventLayerEntity;
import com.luzhiwen.collectagent.memory.application.AgentSessionStateService;
import com.luzhiwen.collectagent.memory.application.AgentSummaryService;
import com.luzhiwen.collectagent.memory.application.LongTermMemoryRecallService;
import com.luzhiwen.collectagent.memory.application.ShortTermMemoryService;
import com.luzhiwen.collectagent.memory.domain.AgentStage;
import com.luzhiwen.collectagent.memory.domain.ShortTermMemoryEvent;
import com.luzhiwen.collectagent.memory.domain.ShortTermMemorySnapshot;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryView;
import com.luzhiwen.collectagent.memory.infrastructure.persistence.entity.AgentSessionStateEntity;
import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.model.application.LlmToolCall;
import com.luzhiwen.collectagent.shared.config.AgentProperties;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.TransferToHumanTool;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 精简版 Agent 编排器，负责事件追加、工具调用、状态补丁合并和最终回复。
 */
@Service
public class AgentOrchestrator {

    /** 最大 ReAct 步数。 */
    private static final int MAX_STEPS = 5;

    /** 固定系统提示词和工具定义的字符开销估算。 */
    private static final int FIXED_PROMPT_OVERHEAD_CHARS = 1600;

    /** 原始事件追加服务。 */
    private final EventAppendService eventAppendService;

    /** 长期记忆召回服务。 */
    private final LongTermMemoryRecallService longTermMemoryRecallService;

    /** Agent 会话状态服务。 */
    private final AgentSessionStateService stateService;

    /** Agent 会话摘要服务。 */
    private final AgentSummaryService summaryService;

    /** 短期记忆缓存投影服务。 */
    private final ShortTermMemoryService shortTermMemoryService;

    /** 动态变量服务。 */
    private final DynamicVariableService dynamicVariableService;

    /** 标准消息历史回放服务。 */
    private final ConversationHistoryService conversationHistoryService;

    /** 最终 JSON 信封解析服务。 */
    private final FinalAnswerEnvelopeService finalAnswerEnvelopeService;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** Agent 配置。 */
    private final AgentProperties agentProperties;

    /** Agent 规划器。 */
    private final AgentPlanner agentPlanner;

    /** 工具执行服务。 */
    private final AgentToolExecutionService agentToolExecutionService;

    /**
     * 创建 Agent 编排器。
     *
     * @param eventAppendService 原始事件追加服务
     * @param longTermMemoryRecallService 长期记忆召回服务
     * @param stateService Agent 会话状态服务
     * @param summaryService Agent 会话摘要服务
     * @param shortTermMemoryService 短期记忆缓存投影服务
     * @param dynamicVariableService 动态变量服务
     * @param conversationHistoryService 标准消息历史回放服务
     * @param finalAnswerEnvelopeService 最终 JSON 信封解析服务
     * @param objectMapper JSON 序列化器
     * @param agentProperties Agent 配置
     * @param agentPlanner Agent 规划器
     * @param agentToolExecutionService 工具执行服务
     */
    public AgentOrchestrator(
            EventAppendService eventAppendService,
            LongTermMemoryRecallService longTermMemoryRecallService,
            AgentSessionStateService stateService,
            AgentSummaryService summaryService,
            ShortTermMemoryService shortTermMemoryService,
            DynamicVariableService dynamicVariableService,
            ConversationHistoryService conversationHistoryService,
            FinalAnswerEnvelopeService finalAnswerEnvelopeService,
            ObjectMapper objectMapper,
            AgentProperties agentProperties,
            AgentPlanner agentPlanner,
            AgentToolExecutionService agentToolExecutionService
    ) {
        this.eventAppendService = eventAppendService;
        this.longTermMemoryRecallService = longTermMemoryRecallService;
        this.stateService = stateService;
        this.summaryService = summaryService;
        this.shortTermMemoryService = shortTermMemoryService;
        this.dynamicVariableService = dynamicVariableService;
        this.conversationHistoryService = conversationHistoryService;
        this.finalAnswerEnvelopeService = finalAnswerEnvelopeService;
        this.objectMapper = objectMapper;
        this.agentProperties = agentProperties;
        this.agentPlanner = agentPlanner;
        this.agentToolExecutionService = agentToolExecutionService;
    }

    /**
     * 处理一次聊天请求。
     *
     * @param request 聊天请求
     * @param traceId 请求链路标识
     * @return 聊天响应
     */
    public AgentChatResponse chat(AgentChatRequest request, String traceId) {
        // 创建本次请求上下文，同时解析 sessionId 并回放历史消息。
        AgentTurnContext turnContext = createTurn(request, traceId);
        // 首次进入会话时追加 SESSION_STARTED，并初始化状态行。
        ensureSessionStarted(turnContext);
        // 写入用户消息事件，同步更新状态并刷新短期缓存。
        appendUserMessage(turnContext);
        // 只回放摘要覆盖位点之后最近 N 轮，避免长会话把全部历史发送给模型。
        loadBoundedConversationHistory(turnContext);
        // 执行 ReAct 循环，最终解析 reply + state_patch。
        return executeAgentLoop(turnContext);
    }

    /**
     * 创建本次请求的执行上下文。
     *
     * @param request 聊天请求
     * @param traceId 请求链路标识
     * @return 执行上下文
     */
    private AgentTurnContext createTurn(AgentChatRequest request, String traceId) {
        String sessionId = resolveSessionId(request.getSessionId());
        // 消息历史在写入当前用户事件后再装载，以便按当前序号截断。
        return new AgentTurnContext(
                traceId,
                sessionId,
                request.getCustomerId(),
                request.getMessage(),
                System.nanoTime(),
                request,
                new ArrayList<>()
        );
    }

    /**
     * 解析或生成会话标识。
     *
     * @param sessionId 请求中的会话标识
     * @return 有效会话标识
     */
    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "S" + UUID.randomUUID().toString().replace("-", "");
        }
        return sessionId;
    }

    /**
     * 确保会话状态已初始化。
     *
     * @param turnContext 执行上下文
     */
    private void ensureSessionStarted(AgentTurnContext turnContext) {
        Optional<AgentSessionStateEntity> state = stateService.getState(turnContext.getSessionId());
        if (state.isPresent()) {
            // 已关闭会话不能再追加 USER_MESSAGE，否则会污染原会话并导致后续抽取语义失真。
            if (AgentStage.CLOSED.name().equals(state.get().getStage())) {
                throw new SessionClosedException(turnContext.getSessionId());
            }
            return;
        }
        EventLayerEntity startedEvent = eventAppendService.append(
                turnContext.getTraceId(),
                turnContext.getSessionId(),
                turnContext.getCustomerId(),
                EventType.SESSION_STARTED,
                Map.of("content", "会话开始")
        );
        stateService.applyEvent(startedEvent);
        shortTermMemoryService.refresh(turnContext.getSessionId(), turnContext.getCustomerId());
    }

    /**
     * 按摘要边界和最近轮次限制装载对话历史。
     *
     * @param turnContext 执行上下文
     */
    private void loadBoundedConversationHistory(AgentTurnContext turnContext) {
        ShortTermMemorySnapshot snapshot = turnContext.getShortTermMemory();
        long summaryCoveredSeq = snapshot.getLatestSummaryCoversSeqTo() == null
                ? 0L
                : snapshot.getLatestSummaryCoversSeqTo();
        long currentUserSeq = turnContext.getUserEvent().getSessionSeq();
        int historyTurnLimit = Math.max(1, agentProperties.getStm().getHistoryTurnLimit());
        turnContext.getNativeMessages().addAll(conversationHistoryService.loadRecentTurns(
                turnContext.getSessionId(),
                summaryCoveredSeq,
                currentUserSeq,
                historyTurnLimit
        ));
    }

    /**
     * 追加当前用户消息并同步投影状态。
     *
     * @param turnContext 执行上下文
     */
    private void appendUserMessage(AgentTurnContext turnContext) {
        EventLayerEntity userEvent = eventAppendService.append(
                turnContext.getTraceId(),
                turnContext.getSessionId(),
                turnContext.getCustomerId(),
                EventType.USER_MESSAGE,
                Map.of("content", turnContext.getCurrentMessage())
        );
        turnContext.setSourceEventId(userEvent.getId());
        turnContext.setUserEvent(userEvent);
        stateService.applyEvent(userEvent);
        turnContext.setShortTermMemory(shortTermMemoryService.refresh(
                turnContext.getSessionId(),
                turnContext.getCustomerId()
        ));
    }

    /**
     * 执行 Agent ReAct 主循环。
     *
     * @param turnContext 执行上下文
     * @return 聊天响应
     */
    private AgentChatResponse executeAgentLoop(AgentTurnContext turnContext) {
        for (int step = 1; step <= MAX_STEPS; step++) {
            // 组装本轮 Planner 所需的完整上下文。
            AgentContext context = prepareContext(turnContext);
            // Planner 决定继续调用工具或输出最终回答。
            AgentAction action = agentPlanner.decide(context);
            if (action instanceof FinalAnswerAction finalAnswerAction) {
                Optional<AgentChatResponse> response = handleFinalAnswer(turnContext, finalAnswerAction);
                if (response.isPresent()) {
                    return response.get();
                }
                continue;
            }
            // 执行模型返回的单个或批量工具调用。
            executeToolAction(turnContext, toToolActions(action));
        }
        return executeFallback(turnContext);
    }

    /**
     * 构造 Planner 上下文，并在超过预算时同步生成摘要。
     *
     * @param turnContext 执行上下文
     * @return Agent 上下文
     */
    private AgentContext prepareContext(AgentTurnContext turnContext) {
        ShortTermMemorySnapshot snapshot = turnContext.getShortTermMemory();
        AgentContext context = buildContext(turnContext, snapshot);
        if (turnContext.isContextBudgetCompressionAttempted()) {
            return context;
        }
        int estimatedTokens = estimateContextTokens(context);
        if (estimatedTokens < agentProperties.getStm().getContextBudgetTokenEstimate()) {
            return context;
        }
        turnContext.setContextBudgetCompressionAttempted(true);
        Optional<String> compressed = summaryService.compressIfContextBudgetExceeded(
                currentUserEvent(turnContext),
                estimatedTokens,
                snapshot.getLatestSummary()
        );
        if (compressed.isEmpty()) {
            return context;
        }
        // 摘要只更新 agent_summary；主链路随后从事实源重建短期快照。
        ShortTermMemorySnapshot refreshed = shortTermMemoryService.refresh(
                turnContext.getSessionId(),
                turnContext.getCustomerId()
        );
        turnContext.setShortTermMemory(refreshed);
        return buildContext(turnContext, refreshed);
    }

    /**
     * 处理最终回答，先执行动态变量说明，再合并状态补丁。
     *
     * @param turnContext 执行上下文
     * @param action 最终回答动作
     * @return 已完成响应，空表示仍需继续 ReAct
     */
    private Optional<AgentChatResponse> handleFinalAnswer(
            AgentTurnContext turnContext,
            FinalAnswerAction action
    ) {
        if (needsDynamicVariableExtraction(turnContext)) {
            executeToolAction(turnContext, List.of(dynamicVariableExtractionAction()));
            return Optional.empty();
        }
        FinalAnswerPayload payload = finalAnswerEnvelopeService.parseAndPersist(
                turnContext.getTraceId(),
                turnContext.getSessionId(),
                turnContext.getCustomerId(),
                turnContext.getSourceEventId(),
                action.getAnswer()
        );
        // LLM 只给候选补丁，最终白名单校验和合并由后端完成。
        stateService.applyPatch(
                turnContext.getSessionId(),
                payload.getStatePatch(),
                turnContext.getSourceEventId()
        );
        return Optional.of(finish(
                turnContext.getTraceId(),
                turnContext.getSessionId(),
                turnContext.getCustomerId(),
                payload.getAnswer(),
                turnContext.getObservations(),
                turnContext.getStartNanos()
        ));
    }

    /**
     * 判断最终回答前是否还需要动态变量说明。
     *
     * @param turnContext 执行上下文
     * @return true 表示需要执行
     */
    private boolean needsDynamicVariableExtraction(AgentTurnContext turnContext) {
        boolean completed = turnContext.getObservations().stream()
                .anyMatch(observation -> DynamicVariableService.TOOL_NAME.equals(observation.getToolName()));
        return dynamicVariableService.shouldExtract() && !completed;
    }

    /**
     * 创建动态变量说明工具动作。
     *
     * @return 工具动作
     */
    private ToolCallAction dynamicVariableExtractionAction() {
        return new ToolCallAction(
                DynamicVariableService.TOOL_NAME,
                Map.of(),
                "最终回答前获取动态变量定义"
        );
    }

    /**
     * 将单个或批量动作统一转换为工具动作列表。
     *
     * @param action Agent 动作
     * @return 工具动作列表
     */
    private List<ToolCallAction> toToolActions(AgentAction action) {
        if (action instanceof ToolCallAction toolCallAction) {
            return List.of(toolCallAction);
        }
        if (action instanceof ToolCallBatchAction batchAction) {
            return batchAction.getActions();
        }
        throw new IllegalStateException("未知 Agent 动作");
    }

    /**
     * 执行一组工具并写入标准消息历史。
     *
     * @param turnContext 执行上下文
     * @param actions 工具动作列表
     */
    private void executeToolAction(AgentTurnContext turnContext, List<ToolCallAction> actions) {
        List<ToolCallAction> normalizedActions = normalizeToolActions(actions);
        appendAssistantToolCallMessage(turnContext.getNativeMessages(), normalizedActions);
        for (ToolCallAction action : normalizedActions) {
            ToolObservation observation = agentToolExecutionService.execute(
                    turnContext.getTraceId(),
                    turnContext.getSessionId(),
                    turnContext.getCustomerId(),
                    turnContext.getSourceEventId(),
                    action
            );
            turnContext.getObservations().add(observation);
            appendToolResultMessage(turnContext.getNativeMessages(), observation);
        }
        refreshMemory(turnContext);
    }

    /**
     * 从短期缓存重建服务刷新执行上下文。
     *
     * @param turnContext 执行上下文
     */
    private void refreshMemory(AgentTurnContext turnContext) {
        turnContext.setShortTermMemory(shortTermMemoryService.refresh(
                turnContext.getSessionId(),
                turnContext.getCustomerId()
        ));
    }

    /**
     * 获取当前用户事件。
     *
     * @param turnContext 执行上下文
     * @return 当前用户事件
     */
    private EventLayerEntity currentUserEvent(AgentTurnContext turnContext) {
        return turnContext.getUserEvent();
    }

    /**
     * 达到最大步数后执行转人工兜底。
     *
     * @param turnContext 执行上下文
     * @return 聊天响应
     */
    private AgentChatResponse executeFallback(AgentTurnContext turnContext) {
        ToolObservation fallback = agentToolExecutionService.execute(
                turnContext.getTraceId(),
                turnContext.getSessionId(),
                turnContext.getCustomerId(),
                turnContext.getSourceEventId(),
                new ToolCallAction(
                        TransferToHumanTool.TOOL_NAME,
                        Map.of("reason", "达到最大执行步数", "summary", turnContext.getCurrentMessage()),
                        "最大步数转人工"
                )
        );
        turnContext.getObservations().add(fallback);
        return finish(
                turnContext.getTraceId(),
                turnContext.getSessionId(),
                turnContext.getCustomerId(),
                fallback.getContent(),
                turnContext.getObservations(),
                turnContext.getStartNanos()
        );
    }

    /**
     * 构造单次 Planner 使用的 Agent 上下文。
     *
     * @param turnContext 执行上下文
     * @param snapshot 短期记忆快照
     * @return Agent 上下文
     */
    private AgentContext buildContext(AgentTurnContext turnContext, ShortTermMemorySnapshot snapshot) {
        if (!turnContext.isLongTermMemoryLoaded()) {
            turnContext.setLongTermMemories(longTermMemoryRecallService.recall(
                    turnContext.getCustomerId(),
                    turnContext.getCurrentMessage(),
                    snapshot.getStateViewJson()
            ));
            turnContext.setLongTermMemoryLoaded(true);
        }
        List<LongTermMemoryView> memories = turnContext.getLongTermMemories();
        String dynamicVariablesJson = dynamicVariableService.findValuesJson(turnContext.getSessionId());
        return new AgentContext(
                turnContext.getTraceId(),
                turnContext.getSessionId(),
                turnContext.getCustomerId(),
                turnContext.getCurrentMessage(),
                turnContext.getSourceEventId(),
                snapshot.getRecentEvents(),
                snapshot.getStateViewJson(),
                snapshot.getLatestSummary(),
                dynamicVariablesJson,
                memories,
                turnContext.getObservations(),
                turnContext.getNativeMessages()
        );
    }

    /**
     * 为缺少工具调用标识的本地动作补齐唯一标识。
     *
     * @param actions 原始工具调用动作
     * @return 补齐标识后的动作
     */
    private List<ToolCallAction> normalizeToolActions(List<ToolCallAction> actions) {
        List<ToolCallAction> normalizedActions = new ArrayList<>();
        for (ToolCallAction action : actions) {
            if (action.getToolCallId() == null || action.getToolCallId().isBlank()) {
                normalizedActions.add(new ToolCallAction(
                        "call_local_" + UUID.randomUUID().toString().replace("-", ""),
                        action.getToolName(),
                        action.getArguments(),
                        action.getReason()
                ));
            } else {
                normalizedActions.add(action);
            }
        }
        return normalizedActions;
    }

    /**
     * 将本轮工具调用追加为标准 assistant tool_calls 消息。
     *
     * @param messages 标准消息历史
     * @param actions 工具调用动作
     */
    private void appendAssistantToolCallMessage(
            List<LlmMessage> messages,
            List<ToolCallAction> actions
    ) {
        List<LlmToolCall> toolCalls = new ArrayList<>();
        for (ToolCallAction action : actions) {
            toolCalls.add(new LlmToolCall(
                    action.getToolCallId(),
                    action.getToolName(),
                    serializeArguments(action.getArguments())
            ));
        }
        messages.add(new LlmMessage("assistant", null, null, toolCalls));
    }

    /**
     * 将工具执行结果追加为标准 tool 消息。
     *
     * @param messages 标准消息历史
     * @param observation 工具观察结果
     */
    private void appendToolResultMessage(
            List<LlmMessage> messages,
            ToolObservation observation
    ) {
        messages.add(new LlmMessage(
                "tool",
                observation.getContent(),
                observation.getToolCallId(),
                List.of()
        ));
    }

    /**
     * 将工具参数序列化为 JSON。
     *
     * @param arguments 工具参数
     * @return 参数 JSON
     */
    private String serializeArguments(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JacksonException exception) {
            throw new IllegalStateException("工具调用参数序列化失败", exception);
        }
    }

    /**
     * 估算当前上下文 Token 数。
     *
     * @param context Agent 上下文
     * @return 估算 Token 数
     */
    private int estimateContextTokens(AgentContext context) {
        int characterCount = FIXED_PROMPT_OVERHEAD_CHARS;
        characterCount += safeLength(context.getCurrentMessage());
        characterCount += safeLength(context.getStateViewJson());
        characterCount += safeLength(context.getSummaryJson());
        characterCount += safeLength(context.getDynamicVariablesJson());
        for (LlmMessage message : context.getNativeMessages()) {
            characterCount += safeLength(message.getRole());
            characterCount += safeLength(message.getContent());
            for (LlmToolCall toolCall : message.getToolCalls()) {
                characterCount += safeLength(toolCall.getToolName());
                characterCount += safeLength(toolCall.getArgumentsJson());
            }
        }
        for (LongTermMemoryView memory : context.getLongTermMemories()) {
            characterCount += memory.getMemoryType() == null ? 0 : safeLength(memory.getMemoryType().getValue());
            characterCount += safeLength(memory.getContent());
        }
        for (ToolObservation observation : context.getObservations()) {
            characterCount += safeLength(observation.getToolName());
            characterCount += safeLength(observation.getContent());
        }
        int charsPerToken = Math.max(1, agentProperties.getStm().getContextCharsPerToken());
        return Math.max(1, characterCount / charsPerToken);
    }

    /**
     * 安全获取字符串长度。
     *
     * @param value 字符串
     * @return 字符串长度，空值返回 0
     */
    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    /**
     * 写入最终回复、应用状态事件并构造响应。
     *
     * @param traceId 请求链路标识
     * @param sessionId 会话唯一标识
     * @param customerId 客户唯一标识
     * @param answer 最终回复
     * @param observations 工具观察结果
     * @param startNanos 请求开始时间
     * @return 聊天响应
     */
    private AgentChatResponse finish(
            String traceId,
            String sessionId,
            String customerId,
            String answer,
            List<ToolObservation> observations,
            long startNanos
    ) {
        EventLayerEntity assistantEvent = eventAppendService.append(
                traceId,
                sessionId,
                customerId,
                EventType.ASSISTANT_MESSAGE,
                Map.of("content", answer)
        );
        stateService.applyEvent(assistantEvent);
        shortTermMemoryService.refresh(sessionId, customerId);
        List<ToolCallView> toolCalls = new ArrayList<>();
        for (ToolObservation observation : observations) {
            toolCalls.add(new ToolCallView(
                    observation.getToolName(),
                    observation.isSuccess(),
                    observation.getContent(),
                    observation.getLatencyMs()
            ));
        }
        return new AgentChatResponse(
                sessionId,
                traceId,
                answer,
                agentPlanner.getClass().getSimpleName(),
                toolCalls,
                elapsedMillis(startNanos)
        );
    }

    /**
     * 计算耗时毫秒数。
     *
     * @param startNanos 开始时间
     * @return 耗时毫秒数
     */
    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}


