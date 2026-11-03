package com.luzhiwen.collectagent.tooling.infrastructure.tools.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 脱敏后的本地测试客户资料。
 */
public class CustomerProfile {

    /**
     * 客户编号。
     */
    private final String customerId;

    /**
     * 脱敏客户姓名。
     */
    private final String maskedName;

    /**
     * 逾期天数。
     */
    private final int overdueDays;

    /**
     * 当前应还总金额。
     */
    private final BigDecimal outstandingAmount;

    /**
     * 客户状态，例如 OVERDUE。
     */
    private final String status;

    /**
     * 最近一次联系日期。
     */
    private final LocalDate lastContactDate;

    /**
     * 创建客户资料。
     *
     * @param customerId 客户编号
     * @param maskedName 脱敏姓名
     * @param overdueDays 逾期天数
     * @param outstandingAmount 应还总金额
     * @param status 客户状态
     * @param lastContactDate 最近联系日期
     */
    public CustomerProfile(
            String customerId,
            String maskedName,
            int overdueDays,
            BigDecimal outstandingAmount,
            String status,
            LocalDate lastContactDate
    ) {
        this.customerId = customerId;
        this.maskedName = maskedName;
        this.overdueDays = overdueDays;
        this.outstandingAmount = outstandingAmount;
        this.status = status;
        this.lastContactDate = lastContactDate;
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
     * 获取脱敏客户姓名。
     *
     * @return 脱敏姓名
     */
    public String getMaskedName() {
        return maskedName;
    }

    /**
     * 获取逾期天数。
     *
     * @return 逾期天数
     */
    public int getOverdueDays() {
        return overdueDays;
    }

    /**
     * 获取应还总金额。
     *
     * @return 应还总金额
     */
    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    /**
     * 获取客户状态。
     *
     * @return 客户状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 获取最近联系日期。
     *
     * @return 最近联系日期
     */
    public LocalDate getLastContactDate() {
        return lastContactDate;
    }
}

