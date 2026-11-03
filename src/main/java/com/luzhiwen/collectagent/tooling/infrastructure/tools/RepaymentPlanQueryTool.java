package com.luzhiwen.collectagent.tooling.infrastructure.tools;

import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import com.luzhiwen.collectagent.tooling.domain.ToolResult;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.model.RepaymentPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.List;
import java.util.Map;

/**
 * 还款方案查询工具，根据客户编号返回本地测试还款方案。
 */
@Component
public class RepaymentPlanQueryTool implements AgentTool {

    /**
     * 工具唯一名称。
     */
    public static final String TOOL_NAME = "repayment_plan_query";

    /**
     * 本地测试客户数据服务。
     */
    private final MockCustomerService customerService;

    /**
     * 创建还款方案查询工具。
     *
     * @param customerService 本地测试客户数据服务
     */
    public RepaymentPlanQueryTool(MockCustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 获取工具名称。
     *
     * @return 工具名称
     */
    @Override
    public String name() {
        return TOOL_NAME;
    }

    /**
     * 获取工具用途说明。
     *
     * @return 工具用途说明
     */
    @Override
    public String description() {
        return "根据客户编号查询可用的还款方案、首付金额、期数和截止日期";
    }

    /**
     * 获取还款方案查询工具的 Function Calling 参数 Schema。
     *
     * @return 参数 Schema
     */
    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "customerId", Map.of(
                                "type", "string",
                                "description", "客户编号，例如 C10001"
                        )
                ),
                "required", List.of("customerId")
        );
    }
    /**
     * 执行还款方案查询。
     *
     * @param arguments 工具参数，必须包含 customerId
     * @return 还款方案查询结果
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        Object customerIdValue = arguments.get("customerId");
        if (customerIdValue == null || customerIdValue.toString().isBlank()) {
            return ToolResult.failure("缺少客户编号 customerId");
        }
        String customerId = customerIdValue.toString().trim().toUpperCase();
        List<RepaymentPlan> plans = customerService.findPlans(customerId);
        if (plans.isEmpty()) {
            return ToolResult.failure("客户" + customerId + "当前没有可用还款方案");
        }

        StringBuilder message = new StringBuilder("客户").append(customerId).append("当前可用方案：");
        List<Map<String, Object>> planDataList = new ArrayList<>();
        for (RepaymentPlan plan : plans) {
            message.append('\n')
                    .append(plan.getPlanName())
                    .append("：首付").append(plan.getDownPayment()).append("元，")
                    .append("分").append(plan.getInstallmentCount()).append("期，")
                    .append("每期").append(plan.getInstallmentAmount()).append("元，")
                    .append("截止日期").append(plan.getDeadline()).append("。");

            Map<String, Object> planData = new LinkedHashMap<>();
            planData.put("planId", plan.getPlanId());
            planData.put("planName", plan.getPlanName());
            planData.put("downPayment", plan.getDownPayment());
            planData.put("installmentCount", plan.getInstallmentCount());
            planData.put("installmentAmount", plan.getInstallmentAmount());
            planData.put("deadline", plan.getDeadline().toString());
            planData.put("description", plan.getDescription());
            planDataList.add(planData);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customerId", customerId);
        data.put("plans", planDataList);
        return ToolResult.success(message.toString(), data);
    }
}


