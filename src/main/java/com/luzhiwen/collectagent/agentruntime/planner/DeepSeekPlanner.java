package com.luzhiwen.collectagent.agentruntime.planner;

import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.agentruntime.domain.AgentAction;
import com.luzhiwen.collectagent.agentruntime.domain.AgentContext;
import com.luzhiwen.collectagent.agentruntime.domain.FinalAnswerAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolCallAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolCallBatchAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolObservation;
import com.luzhiwen.collectagent.model.infrastructure.deepseek.DeepSeekChatClient;
import com.luzhiwen.collectagent.model.application.LlmChatResult;
import com.luzhiwen.collectagent.model.application.LlmMessage;
import com.luzhiwen.collectagent.model.application.LlmToolCall;
import com.luzhiwen.collectagent.memory.domain.LongTermMemoryView;
import com.luzhiwen.collectagent.tooling.application.ToolRegistry;
import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.TransferToHumanTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 DeepSeek 原生 Function Calling 的 Agent 规划器。
 */
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
public class DeepSeekPlanner implements AgentPlanner {

    /** 当前类日志器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekPlanner.class);

    /** 最终 JSON 指令标记前缀。 */
    private static final String FINAL_JSON_MARKER_PREFIX = "[[FINAL_JSON_ENVELOPE_";

    /** DeepSeek 客户端。 */
    private final DeepSeekChatClient deepSeekChatClient;

    /** 工具注册中心。 */
    private final ToolRegistry toolRegistry;

    /** 动态变量服务。 */
    private final DynamicVariableService dynamicVariableService;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建 DeepSeek 规划器。
     *
     * @param deepSeekChatClient DeepSeek 客户端
     * @param toolRegistry 工具注册中心
     * @param dynamicVariableService 动态变量服务
     * @param objectMapper JSON 序列化器
     */
    public DeepSeekPlanner(
            DeepSeekChatClient deepSeekChatClient,
            ToolRegistry toolRegistry,
            DynamicVariableService dynamicVariableService,
            ObjectMapper objectMapper
    ) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.toolRegistry = toolRegistry;
        this.dynamicVariableService = dynamicVariableService;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据上下文决定下一步动作。
     *
     * @param context Agent 上下文
     * @return 工具调用或最终回复
     */
    @Override
    public AgentAction decide(AgentContext context) {
        try {
            // 补齐 system、user 和最终 JSON 阶段消息。
            prepareMessages(context);
            // 调用模型，并根据上下文决定是否启用 JSON 输出模式。
            LlmChatResult result = callModel(context, isJsonResponseMode(context));
            // 将模型响应转换为工具调用或最终回答动作。
            return toAgentAction(result);
        } catch (Exception exception) {
            // 模型调用失败时统一转人工，避免请求直接失败。
            return buildFailureAction(context, exception);
        }
    }

    /**
     * 准备本轮标准消息历史。
     *
     * @param context Agent 上下文
     */
    private void prepareMessages(AgentContext context) {
        if (!context.getObservations().isEmpty()) {
            return;
        }
        List<LlmMessage> messages = context.getNativeMessages();
        if (messages.isEmpty() || !"system".equals(messages.get(0).getRole())) {
            messages.add(0, new LlmMessage("system", buildSystemPrompt()));
        }
        messages.add(new LlmMessage("user", buildUserPrompt(context)));
    }

    /**
     * 判断是否进入最终 JSON 输出阶段。
     *
     * @param context Agent 上下文
     * @return true 表示已执行动态变量说明工具
     */
    private boolean isJsonResponseMode(AgentContext context) {
        return context.getObservations().stream()
                .anyMatch(observation -> DynamicVariableService.TOOL_NAME.equals(observation.getToolName()));
    }

    /**
     * 调用 DeepSeek 模型。
     *
     * @param context Agent 上下文
     * @param jsonResponseMode 是否使用 JSON 输出模式
     * @return 模型结果
     */
    private LlmChatResult callModel(AgentContext context, boolean jsonResponseMode) {
        if (jsonResponseMode) {
            appendFinalJsonInstruction(context.getNativeMessages(), context);
        }
        List<AgentTool> tools = jsonResponseMode ? List.of() : toolRegistry.listTools();
        return deepSeekChatClient.chat(
                context.getTraceId(),
                context.getSessionId(),
                context.getCustomerId(),
                context.getNativeMessages(),
                tools,
                null,
                jsonResponseMode
        );
    }

    /**
     * 将模型结果转换为 Agent 动作。
     *
     * @param result 模型结果
     * @return Agent 动作
     */
    private AgentAction toAgentAction(LlmChatResult result) {
        if (result.hasToolCall()) {
            return toToolCallAction(result);
        }
        if (result.getContent() == null || result.getContent().isBlank()) {
            throw new IllegalStateException("DeepSeek 没有返回文本或工具调用");
        }
        return new FinalAnswerAction(result.getContent(), List.of());
    }

    /**
     * 将模型工具调用转换为单个或批量动作。
     *
     * @param result 模型结果
     * @return 工具调用动作
     */
    private AgentAction toToolCallAction(LlmChatResult result) {
        List<ToolCallAction> actions = new ArrayList<>();
        for (LlmToolCall toolCall : result.getToolCalls()) {
            actions.add(new ToolCallAction(
                    toolCall.getToolCallId(),
                    toolCall.getToolName(),
                    parseArguments(toolCall.getArgumentsJson()),
                    "DeepSeek 原生工具调用"
            ));
        }
        return actions.size() == 1
                ? actions.get(0)
                : new ToolCallBatchAction(actions, "DeepSeek 并行工具调用");
    }

    /**
     * 构造模型调用失败时的转人工动作。
     *
     * @param context Agent 上下文
     * @param exception 异常
     * @return 转人工动作
     */
    private AgentAction buildFailureAction(AgentContext context, Exception exception) {
        LOGGER.error("DeepSeek 规划失败，自动转人工", exception);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("reason", "DeepSeek 规划失败：" + exception.getMessage());
         arguments.put("summary", context.getCurrentMessage());
        return new ToolCallAction(TransferToHumanTool.TOOL_NAME, arguments, "模型失败转人工");
    }

    /**
     * 在标准消息历史末尾追加最终 JSON 输出指令。
     *
     * @param messages 标准消息历史
     * @param context Agent 上下文
     */
    private void appendFinalJsonInstruction(List<LlmMessage> messages, AgentContext context) {
        String marker = FINAL_JSON_MARKER_PREFIX + context.getSourceEventId() + "]]";
        boolean exists = messages.stream().anyMatch(message ->
                "user".equals(message.getRole())
                        && message.getContent() != null
                        && message.getContent().contains(marker)
        );
        if (exists) {
            return;
        }
        messages.add(new LlmMessage(
                "user",
                marker + "\n现在进入最终输出阶段。基于完整 ReAct 历史、工具结果和状态视图，"
                        + "只输出一个 JSON 对象，格式为："
                        + "{\"reply\":\"面向客户的业务回复\",\"state_patch\":{\"stage\":\"阶段\",\"slots\":{}}}。"
                        + "state_patch 只输出需要变化的阶段和白名单槽位，不输出完整 state、flags 或 pending。"
                        + "如果本次已经执行动态变量说明工具，可额外输出 variableMap；没有变量时不要输出或输出空对象。"
                        + "reply 只能包含正常业务回复，不得包含 JSON 结构。"
                        + "不要输出 Markdown 代码围栏或额外说明。"
        ));
    }

    /**
     * 构建系统提示词。
     *
     * <p>系统提示词只包含角色、业务边界和合规规则，工具定义由 API tools 参数提供。</p>
     *
     * @return 系统提示词  且针对每次user的会话内容强制调用 dynamic_variable_extract工具，
     */
    private String buildSystemPrompt() {
        return """
                你是智能催收客服 Agent，只处理欠款查询、还款方案、还款时间咨询和会话变量提取。
                只能依据工具结果和历史记忆回答，不得编造金额、期限、方案或减免承诺。
                系统已经提供工具，请按需直接调用原生工具，不要输出自定义工具调用 JSON。
                会话状态视图只包含已知槽位、缺失槽位、只读 flags、可修改字段白名单和允许的阶段迁移。
                债务金额和逾期天数只能引用工具结果，禁止通过 state_patch 修改。
                flags 和 pending 由后端维护，禁止修改。

                ReAct 工具调用阶段：
                - 根据用户问题调用 customer_query、repayment_plan_query 或 transfer_to_human。
                - 工具结果足够后，如果动态变量提取已启用，先调用 dynamic_variable_extract。
                - 不要在工具结果尚未返回时猜测金额、期限或方案。

                最终输出阶段：
                当不再需要调用工具时，输出且只输出一个 JSON 对象：
                {"reply":"面向客户的业务回复","state_patch":{"stage":"可选阶段","slots":{"promise_amount":{"value":5000,"confidence":0.95}}}}
                state_patch 只允许修改当前状态视图 allowed_patch_fields 中的槽位，并且只能使用 allowed_stage_transitions 中的迁移。
                用户没有明确提供的日期或金额，不得写入 state_patch。
                如果本次已执行动态变量说明工具，可以额外输出 variableMap；没有变量时不要输出或输出空对象。
                reply 只包含正常催收业务回复，使用简洁中文并说明下一步操作，不得包含 JSON 结构。
                不要输出 Markdown 代码围栏或 JSON 之外的说明。
                """;
    }

    /**
     * 构建用户上下文提示词。
     *
     * @param context Agent 上下文
     * @return 用户提示词
     */
    private String buildUserPrompt(AgentContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("客户编号：").append(context.getCustomerId()).append('\n');
        builder.append("当前消息：").append(context.getCurrentMessage()).append("\n\n");
        builder.append("会话状态视图 JSON：\n").append(context.getStateViewJson()).append("\n\n");
        builder.append("历史摘要 JSON：\n").append(context.getSummaryJson()).append("\n\n");
        builder.append("会话动态变量 JSON：\n").append(context.getDynamicVariablesJson()).append("\n\n");
        builder.append("长期记忆：\n");
        if (context.getLongTermMemories().isEmpty()) {
            builder.append("无\n");
        } else {
            for (LongTermMemoryView memory : context.getLongTermMemories()) {
                builder.append(memory.getMemoryType().getValue()).append("：").append(memory.getContent()).append('\n');
            }
        }
        builder.append("\n已执行工具：\n");
        if (context.getObservations().isEmpty()) {
            builder.append("无\n");
        } else {
            for (ToolObservation observation : context.getObservations()) {
                builder.append(observation.getToolName())
                        .append(" success=").append(observation.isSuccess())
                        .append(" result=").append(observation.getContent())
                        .append('\n');
            }
        }
        builder.append("\n请直接回答或调用需要的工具。");
        return builder.toString();
    }

    /**
     * 序列化当前会话结构化状态。
     *
     * @param context Agent 上下文
     * @return 状态 JSON
     */
    private String serializeState(AgentContext context) {
        return context.getStateViewJson() == null ? "{}" : context.getStateViewJson();
    }
    /**
     * 解析工具调用参数 JSON。
     *
     * @param argumentsJson 参数 JSON
     * @return 参数 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("解析工具调用参数失败", exception);
        }
    }
}



