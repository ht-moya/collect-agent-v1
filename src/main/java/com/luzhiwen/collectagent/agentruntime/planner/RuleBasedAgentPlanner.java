package com.luzhiwen.collectagent.agentruntime.planner;

import com.luzhiwen.collectagent.agentruntime.domain.AgentAction;
import com.luzhiwen.collectagent.agentruntime.domain.AgentContext;
import com.luzhiwen.collectagent.agentruntime.domain.FinalAnswerAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolCallAction;
import com.luzhiwen.collectagent.agentruntime.domain.ToolObservation;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.CustomerQueryTool;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.RepaymentPlanQueryTool;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.TransferToHumanTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的 Agent 规划器。
 *
 * <p>当前版本不调用大模型，先用确定性规则验证 ReAct 循环、工具调用和日志链路。
 * 后续接入 LLM 时只需要新增一个 AgentPlanner 实现，无需修改编排器。</p>
 */
@Component
@ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class RuleBasedAgentPlanner implements AgentPlanner {

    /**
     * 从用户消息中提取 C 开头五位客户编号的正则表达式。
     */
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("(?i)\\bC\\d{5}\\b");

    /**
     * 表示用户正在询问还款方案的业务关键词。
     */
    private static final List<String> REPAYMENT_KEYWORDS = List.of(
            "还款", "方案", "分期", "首付", "怎么还", "协商", "结清"
    );

    /**
     * 表示用户希望人工介入的业务关键词。
     */
    private static final List<String> HUMAN_KEYWORDS = List.of(
            "人工", "转人工", "投诉", "客服", "真人"
    );

    /**
     * 根据上下文决定下一步动作。
     *
     * @param context Agent 当前上下文
     * @return 工具调用或最终回复动作
     */
    @Override
    public AgentAction decide(AgentContext context) {
        String message = context.getCurrentMessage();
        String customerId = context.getCustomerId();
        if (customerId == null || customerId.isBlank()) {
            customerId = extractCustomerId(message);
        }
        boolean repaymentIntent = containsAny(message, REPAYMENT_KEYWORDS);
        List<ToolObservation> observations = context.getObservations();

        if (!observations.isEmpty()) {
            return decideAfterObservation(context, customerId, repaymentIntent, observations);
        }

        if (containsAny(message, HUMAN_KEYWORDS)) {
            return buildTransferAction("用户明确要求人工介入", message);
        }

        if (customerId != null) {
            return buildCustomerQueryAction(customerId, repaymentIntent);
        }

        if (repaymentIntent) {
            return new FinalAnswerAction(
                    "请提供您的客户编号，例如 C10001，我会先查询客户信息，再为您查看可用还款方案。",
                    List.of()
            );
        }

        return new FinalAnswerAction(
                "您好，我可以协助查询客户信息、可用还款方案，或在需要时为您转接人工。请提供客户编号和具体问题。",
                List.of()
        );
    }

    /**
     * 根据已经执行的工具结果决定下一步动作。
     *
     * @param context Agent 当前上下文
     * @param customerId 当前消息中的客户编号
     * @param repaymentIntent 是否包含还款意图
     * @param observations 已有工具观察结果
     * @return 下一步动作
     */
    private AgentAction decideAfterObservation(
            AgentContext context,
            String customerId,
            boolean repaymentIntent,
            List<ToolObservation> observations
    ) {
        // 动态变量说明工具只负责补充变量定义，不参与业务答复内容选择。
        List<ToolObservation> businessObservations = observations.stream()
                .filter(observation -> !DynamicVariableService.TOOL_NAME.equals(observation.getToolName()))
                .toList();
        if (businessObservations.isEmpty()) {
            return new FinalAnswerAction("变量定义已加载，请继续说明您的还款问题。", List.of());
        }
        ToolObservation latest = businessObservations.get(businessObservations.size() - 1);
        if (!latest.isSuccess() && !TransferToHumanTool.TOOL_NAME.equals(latest.getToolName())) {
            return buildTransferAction("工具执行失败：" + latest.getContent(), context.getCurrentMessage());
        }

        if (CustomerQueryTool.TOOL_NAME.equals(latest.getToolName()) && repaymentIntent && customerId != null) {
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("customerId", customerId);
            return new ToolCallAction(
                    RepaymentPlanQueryTool.TOOL_NAME,
                    arguments,
                    "客户查询完成，继续查询可用还款方案"
            );
        }

        if (TransferToHumanTool.TOOL_NAME.equals(latest.getToolName())) {
            return new FinalAnswerAction(latest.getContent() + " 请保持电话畅通，工作人员会尽快联系您。", List.of());
        }

        return new FinalAnswerAction(latest.getContent(), List.of());
    }

    /**
     * 构建客户查询工具动作。
     *
     * @param customerId 客户编号
     * @param repaymentIntent 是否包含还款意图
     * @return 客户查询动作
     */
    private ToolCallAction buildCustomerQueryAction(String customerId, boolean repaymentIntent) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("customerId", customerId);
        String reason = repaymentIntent
                ? "需要先确认客户身份和逾期状态，再查询还款方案"
                : "用户提供了客户编号，需要查询客户信息";
        return new ToolCallAction(CustomerQueryTool.TOOL_NAME, arguments, reason);
    }

    /**
     * 构建转人工工具动作。
     *
     * @param reason 转人工原因
     * @param summary 当前对话摘要
     * @return 转人工动作
     */
    private ToolCallAction buildTransferAction(String reason, String summary) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("reason", reason);
        arguments.put("summary", summary);
        return new ToolCallAction(TransferToHumanTool.TOOL_NAME, arguments, reason);
    }

    /**
     * 从用户消息中提取客户编号。
     *
     * @param message 用户消息
     * @return 客户编号；未找到时返回 null
     */
    private String extractCustomerId(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = CUSTOMER_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }

    /**
     * 判断文本是否包含任意关键词。
     *
     * @param text 待检查文本
     * @param keywords 关键词列表
     * @return true 表示至少包含一个关键词
     */
    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}





