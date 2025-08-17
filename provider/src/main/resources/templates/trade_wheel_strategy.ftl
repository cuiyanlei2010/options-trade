当前日期${currentDate}。<#if summary??>期权策略ID:${summary.strategy.strategyId}，当前期权策略是：${summary.strategy.getOptionsStrategy().getName() }。</#if>
当前${securityQuote.security.toString()}股票价格是${securityPrice}。<#if vixIndicator?? && vixIndicator.currentVix??>当前VIX指数是${vixIndicator.currentVix.value}。</#if>
我准备使用车轮策略(Wheel Strategy)卖出${securityQuote.security.toString()}距离到期日${optionsChain.dte()}天的${isSellPutStage?string('看跌期权（Cash-Secured Put）','看涨期权（Covered Call）')}。倾向的期权Delta绝对值范围:0.25-0.35。
备选的期权距离到期日${optionsChain.dte()}天，请查询策略详情，基于策略详情和市场环境分析`交易期权标的`里的标的，给我这些期权标的交易建议。