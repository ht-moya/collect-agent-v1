package com.luzhiwen.collectagent.tooling.infrastructure.tools;

import com.luzhiwen.collectagent.tooling.domain.AgentTool;
import com.luzhiwen.collectagent.tooling.domain.ToolResult;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.model.CustomerProfile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 客户查询工具，根据客户编号返回脱敏后的客户信息。
 */
@Component
public class CustomerQueryTool implements AgentTool {

    /**
     * 工具唯一名称。
     */
    public static final String TOOL_NAME = "customer_query";

    /**
     * 本地测试客户数据服务。
     */
    private final MockCustomerService customerService;

    /**
     * 创建客户查询工具。
     *
     * @param customerService 本地测试客户数据服务
     */
    public CustomerQueryTool(MockCustomerService customerService) {
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
        return "根据客户编号查询脱敏姓名、逾期天数、应还金额和客户状态";
    }

    /**
     * 获取客户查询工具的 Function Calling 参数 Schema。
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
     * 执行客户查询。
     *
     * @param arguments 工具参数，必须包含 customerId
     * @return 客户查询结果
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        Object customerIdValue = arguments.get("customerId");
        if (customerIdValue == null || customerIdValue.toString().isBlank()) {
            return ToolResult.failure("缺少客户编号 customerId");
        }
        String customerId = customerIdValue.toString().trim().toUpperCase();
        Optional<CustomerProfile> customerOptional = customerService.findCustomer(customerId);
        if (customerOptional.isEmpty()) {
            return ToolResult.failure("未找到客户：" + customerId);
        }

        CustomerProfile customer = customerOptional.get();
        String message = String.format(
                "客户%s，姓名%s，逾期%d天，当前应还%s元，状态%s，最近联系日期%s。",
                customer.getCustomerId(),
                customer.getMaskedName(),
                customer.getOverdueDays(),
                customer.getOutstandingAmount(),
                customer.getStatus(),
                customer.getLastContactDate()
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customerId", customer.getCustomerId());
        data.put("maskedName", customer.getMaskedName());
        data.put("overdueDays", customer.getOverdueDays());
        data.put("outstandingAmount", customer.getOutstandingAmount());
        data.put("status", customer.getStatus());
        data.put("lastContactDate", customer.getLastContactDate().toString());
        return ToolResult.success(message, data);
    }
}


