package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.TreeMap;

/**
 * 账户汇总
 *
 * @author qiyan
 */
@Data
public class OwnerSummary {
    /**
     * 期权总收益
     */
    private BigDecimal allOptionsIncome;

    /**
     * 总手续费
     */
    private BigDecimal totalFee;

    /**
     * 未实现期权收益
     */
    private BigDecimal unrealizedOptionsIncome;

    /**
     * 策略汇总
     */
    private List<StrategySummary> strategySummaries;

    /**
     * 未实现期权
     */
    private List<OwnerOrder> unrealizedOrders;

    /**
     * 月度收益
     */
    private TreeMap<String, BigDecimal> monthlyIncome;

    /**
     * 账户总规模
     */
    private BigDecimal accountSize;

    /**
     * 保证金比例
     */
    private BigDecimal marginRatio;

    /**
     * PUT订单保证金占用
     */
    private BigDecimal putMarginOccupied;

    /**
     * 持有股票总成本
     */
    private BigDecimal totalStockCost;

    /**
     * 可用资金
     */
    private BigDecimal availableFunds;

    /**
     * 当前投资宇瞻用
     */
    private BigDecimal totalInvestment;
}
