package com.luzhiwen.collectagent.tooling.infrastructure.tools;

import com.luzhiwen.collectagent.tooling.infrastructure.tools.model.CustomerProfile;
import com.luzhiwen.collectagent.tooling.infrastructure.tools.model.RepaymentPlan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 本地测试客户数据服务。
 *
 * <p>所有数据均为虚构数据，仅用于本地 Agent 开发，不包含真实客户信息。</p>
 */
@Service
public class MockCustomerService {

    /**
     * 客户编号到客户资料的映射。
     */
    private final Map<String, CustomerProfile> customers = new LinkedHashMap<>();

    /**
     * 客户编号到还款方案的映射。
     */
    private final Map<String, List<RepaymentPlan>> plansByCustomer = new LinkedHashMap<>();

    /**
     * 创建本地测试客户数据服务并初始化虚构数据。
     */
    public MockCustomerService() {
        initializeCustomers();
        initializeRepaymentPlans();
    }

    /**
     * 根据客户编号查询客户资料。
     *
     * @param customerId 客户编号
     * @return 匹配的客户资料，可能为空
     */
    public Optional<CustomerProfile> findCustomer(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(customers.get(customerId.toUpperCase()));
    }

    /**
     * 根据客户编号查询全部还款方案。
     *
     * @param customerId 客户编号
     * @return 还款方案列表，不会返回 null
     */
    public List<RepaymentPlan> findPlans(String customerId) {
        if (customerId == null) {
            return List.of();
        }
        return List.copyOf(plansByCustomer.getOrDefault(customerId.toUpperCase(), List.of()));
    }

    /**
     * 初始化虚构客户数据。
     */
    private void initializeCustomers() {
        customers.put("C10001", new CustomerProfile(
                "C10001",
                "张*",
                35,
                new BigDecimal("12800.00"),
                "OVERDUE",
                LocalDate.now().minusDays(2)
        ));
        customers.put("C10002", new CustomerProfile(
                "C10002",
                "李*",
                8,
                new BigDecimal("5600.00"),
                "OVERDUE",
                LocalDate.now().minusDays(5)
        ));
    }

    /**
     * 初始化虚构还款方案数据。
     */
    private void initializeRepaymentPlans() {
        List<RepaymentPlan> firstCustomerPlans = new ArrayList<>();
        firstCustomerPlans.add(new RepaymentPlan(
                "P10001-6",
                "C10001",
                "6期分期方案",
                new BigDecimal("3000.00"),
                6,
                new BigDecimal("1800.00"),
                LocalDate.now().plusDays(7),
                "首付3000元，剩余款项分6期偿还。"
        ));
        firstCustomerPlans.add(new RepaymentPlan(
                "P10001-12",
                "C10001",
                "12期分期方案",
                new BigDecimal("2000.00"),
                12,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(10),
                "首付2000元，剩余款项分12期偿还。"
        ));
        plansByCustomer.put("C10001", firstCustomerPlans);

        plansByCustomer.put("C10002", List.of(new RepaymentPlan(
                "P10002-3",
                "C10002",
                "3期分期方案",
                new BigDecimal("1000.00"),
                3,
                new BigDecimal("1600.00"),
                LocalDate.now().plusDays(5),
                "首付1000元，剩余款项分3期偿还。"
        )));
    }
}

