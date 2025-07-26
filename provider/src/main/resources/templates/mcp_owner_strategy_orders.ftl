# ${strategySummary.strategy.strategyName!''} 策略详情

## 策略信息
- 策略ID: ${strategySummary.strategy.strategyId!''}
- 策略名称: ${strategySummary.strategy.strategyName!''}
- 策略Delta: ${strategySummary.strategyDelta!''} (看多比例:${strategySummary.avgDelta!''})
- 策略盈利: $${strategySummary.allIncome!''}
- 期权盈利: $${strategySummary.allOptionsIncome!''}
- 持有股票: ${strategySummary.holdStockNum!''}
- 当前股价: $${strategySummary.currentStockPrice!''}
- 股票支出: $${strategySummary.totalStockCost!''} (成本: $${strategySummary.averageStockCost!''})
- 持股盈亏: $${strategySummary.holdStockProfit!''}
- 希腊字母: Delta:${strategySummary.optionsDelta!''}｜Gamma:${strategySummary.optionsGamma!''}｜Theta:${strategySummary.optionsTheta!''}
- 期权已到账收入: $${strategySummary.allOptionsIncome!''} (已扣除手续费$${strategySummary.totalFee!''})
- 期权未到期收入: $${strategySummary.unrealizedOptionsIncome!''}
- PUT订单保证金占用: $${strategySummary.putMarginOccupied!''}

<#if orders?? && orders?size gt 0>
## 策略订单明细
| 标的代码 | 证券代码 | 类型 | 价格 | 数量 | 订单收益 | 订单费用 | 行权时间 | 交易时间 | 状态 | 订单号 | 是否平仓 |
|---------|---------|------|------|------|---------|---------|---------|---------|------|--------|---------|
<#list orders as order>
| ${order.underlyingCode!''} | ${order.code!''} | ${order.side!''} | ${order.price!''} | ${order.quantity!0} | ${order.ext.totalIncome!''} | ${order.orderFee!''} | ${(order.strikeTime?string('yyyy-MM-dd'))!''} | ${(order.tradeTime?string('yyyy-MM-dd HH:mm:ss'))!''} | ${order.status!''} | ${order.platformOrderId!''} | ${order.ext.isClose!'false'} |
</#list>
<#else>
暂无订单
</#if>
