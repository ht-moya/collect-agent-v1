package com.luzhiwen.collectagent.tooling.infrastructure.tools.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 本地测试还款方案。
 */
public class RepaymentPlan {

    /**
     * 方案编号。
     */
    private final String planId;

    /**
     * 方案所属客户编号。
     */
    private final String customerId;

    /**
     * 方案名称。
     */
    private final String planName;

    /**
     * 首付金额。
     */
    private final BigDecimal downPayment;

    /**
     * 分期期数。
     */
    private final int installmentCount;

    /**
     * 每期还款金额。
     */
    private final BigDecimal installmentAmount;

    /**
     * 方案截止日期。
     */
    private final LocalDate deadline;

    /**
     * 方案说明。
     */
    private final String description;

    /**
     * 创建还款方案。
     *
     * @param planId 方案编号
     * @param customerId 客户编号
     * @param planName 方案名称
     * @param downPayment 首付金额
     * @param installmentCount 分期期数
     * @param installmentAmount 每期还款金额
     * @param deadline 截止日期
     * @param description 方案说明
     */
    public RepaymentPlan(
            String planId,
            String customerId,
            String planName,
            BigDecimal downPayment,
            int installmentCount,
            BigDecimal installmentAmount,
            LocalDate deadline,
            String description
    ) {
        this.planId = planId;
        this.customerId = customerId;
        this.planName = planName;
        this.downPayment = downPayment;
        this.installmentCount = installmentCount;
        this.installmentAmount = installmentAmount;
        this.deadline = deadline;
        this.description = description;
    }

    /**
     * 获取方案编号。
     *
     * @return 方案编号
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * 获取客户编号。
     *
     * @return 客户编号
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * 获取方案名称。
     *
     * @return 方案名称
     */
    public String getPlanName() {
        return planName;
    }

    /**
     * 获取首付金额。
     *
     * @return 首付金额
     */
    public BigDecimal getDownPayment() {
        return downPayment;
    }

    /**
     * 获取分期期数。
     *
     * @return 分期期数
     */
    public int getInstallmentCount() {
        return installmentCount;
    }

    /**
     * 获取每期还款金额。
     *
     * @return 每期还款金额
     */
    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    /**
     * 获取截止日期。
     *
     * @return 截止日期
     */
    public LocalDate getDeadline() {
        return deadline;
    }

    /**
     * 获取方案说明。
     *
     * @return 方案说明
     */
    public String getDescription() {
        return description;
    }
}

