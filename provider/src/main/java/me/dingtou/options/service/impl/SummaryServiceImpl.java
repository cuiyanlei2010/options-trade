package me.dingtou.options.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import me.dingtou.options.dao.OwnerStrategyDAO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.manager.IndicatorManager;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.OwnerSummary;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StrategyExt;
import me.dingtou.options.model.StrategySummary;
import me.dingtou.options.service.SummaryService;
import me.dingtou.options.strategy.OrderTradeStrategy;
import me.dingtou.options.strategy.order.DefaultOrderTradeStrategy;

import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SummaryServiceImpl implements SummaryService {

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private IndicatorManager indicatorManager;

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;

    @Autowired
    private OwnerStrategyDAO ownerStrategyDAO;

    @Override
    public OwnerSummary queryOwnerSummary(String owner) {
        OwnerSummary ownerSummary = new OwnerSummary();

        BigDecimal allOptionsIncome = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal unrealizedOptionsIncome = BigDecimal.ZERO;
        BigDecimal allHoldStockProfit = BigDecimal.ZERO;
        BigDecimal allIncome = BigDecimal.ZERO;

        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategy(owner);
        List<StrategySummary> strategySummaries = new CopyOnWriteArrayList<>();
        // 批量拉取策略数据
        ownerStrategies.parallelStream().forEach(ownerStrategy -> {
            StrategySummary strategySummary = queryStrategySummary(owner, ownerStrategy);
            strategySummaries.add(strategySummary);
        });
        strategySummaries.stream().filter(summary -> CollectionUtils.isEmpty(summary.getStrategyOrders()))
                .forEach(strategySummaries::remove);
        // 统计未平仓订单
        List<OwnerOrder> unrealizedOrders = new ArrayList<>();
        for (StrategySummary strategySummary : strategySummaries) {
            if (null == strategySummary.getAllOptionsIncome()) {
                continue;
            }
            allOptionsIncome = allOptionsIncome.add(strategySummary.getAllOptionsIncome());
            totalFee = totalFee.add(strategySummary.getTotalFee());
            unrealizedOptionsIncome = unrealizedOptionsIncome.add(strategySummary.getUnrealizedOptionsIncome());

            strategySummary.getStrategyOrders().stream()
                    .filter(OwnerOrder::isOpen)
                    .filter(OwnerOrder::isOptionsOrder)
                    .filter(order -> OrderStatus.of(order.getStatus()).isValid())
                    .forEach(unrealizedOrders::add);

            allHoldStockProfit = allHoldStockProfit.add(strategySummary.getHoldStockProfit());
            allIncome = allIncome.add(strategySummary.getAllIncome());
        }

        ownerSummary.setAllOptionsIncome(allOptionsIncome);
        ownerSummary.setTotalFee(totalFee);
        ownerSummary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);
        ownerSummary.setStrategySummaries(strategySummaries);
        ownerSummary.setUnrealizedOrders(unrealizedOrders);
        ownerSummary.setAllHoldStockProfit(allHoldStockProfit);
        ownerSummary.setAllIncome(allIncome);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        Map<String, List<OwnerOrder>> monthOrder = strategySummaries.stream()
                .flatMap(strategy -> strategy.getStrategyOrders().stream())
                .collect(Collectors.groupingBy(order -> simpleDateFormat.format(order.getTradeTime())));

        // 月度收益
        Map<String, BigDecimal> stockLotSizeMap = new HashMap<>();
        for (OwnerStrategy ownerStrategy : ownerStrategies) {
            stockLotSizeMap.put(ownerStrategy.getCode(), BigDecimal.valueOf(ownerStrategy.getLotSize()));
        }

        TreeMap<String, BigDecimal> monthlyIncome = new TreeMap<>();
        for (Map.Entry<String, List<OwnerOrder>> entry : monthOrder.entrySet()) {
            BigDecimal income = entry.getValue().stream()
                    .filter(OwnerOrder::isOptionsOrder)
                    .filter(OwnerOrder::isTraded)
                    .map(order -> {
                        BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
                        BigDecimal quantity = new BigDecimal(order.getQuantity());
                        BigDecimal lotSize = stockLotSizeMap.get(order.getUnderlyingCode());
                        lotSize = null != lotSize ? lotSize : BigDecimal.valueOf(100);
                        return order.getPrice()
                                .multiply(quantity)
                                .multiply(lotSize)
                                .multiply(sign)
                                .subtract(order.getOrderFee());
                    }).reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyIncome.put(entry.getKey(), income);
        }
        ownerSummary.setMonthlyIncome(monthlyIncome);

        // 获取账户信息
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        String accountSizeConf = account.getExtValue(AccountExt.ACCOUNT_SIZE, null);
        String marginRatioConf = account.getExtValue(AccountExt.MARGIN_RATIO, null);
        String positionRatioConf = account.getExtValue(AccountExt.POSITION_RATIO, "0.1");

        if (StringUtils.isNotBlank(accountSizeConf) && StringUtils.isNotBlank(marginRatioConf)) {
            BigDecimal accountSize = new BigDecimal(accountSizeConf);
            BigDecimal marginRatio = new BigDecimal(marginRatioConf);
            BigDecimal positionRatio = new BigDecimal(positionRatioConf);

            ownerSummary.setMarginRatio(marginRatio);
            ownerSummary.setPositionRatio(positionRatio);

            // 根据初始股票数和平均股价计算accountSize
            BigDecimal initStockAccountSize = BigDecimal.ZERO;
            for (StrategySummary strategySummary : strategySummaries) {
                OwnerStrategy strategy = strategySummary.getStrategy();
                int initialStockNum = Integer.parseInt(strategy.getExtValue(StrategyExt.INITIAL_STOCK_NUM, "0"));
                BigDecimal averageStockCost = strategySummary.getAverageStockCost();
                BigDecimal totalStockCost = averageStockCost.multiply(new BigDecimal(initialStockNum));
                initStockAccountSize = initStockAccountSize.add(totalStockCost);
            }
            accountSize = accountSize.add(initStockAccountSize);
            ownerSummary.setAccountSize(accountSize);

            // 计算PUT订单保证金占用和持有股票总成本
            BigDecimal putMarginOccupied = BigDecimal.ZERO;
            BigDecimal totalStockCost = BigDecimal.ZERO;
            for (StrategySummary strategySummary : strategySummaries) {
                putMarginOccupied = putMarginOccupied.add(strategySummary.getPutMarginOccupied());
                totalStockCost = totalStockCost.add(strategySummary.getTotalStockCost());
            }
            ownerSummary.setPutMarginOccupied(putMarginOccupied);
            // 计算持有股票总成本(初始股票扣除)
            ownerSummary.setTotalStockCost(totalStockCost.add(initStockAccountSize));

            // 计算可用资金
            BigDecimal availableFunds = accountSize.subtract(putMarginOccupied)
                    .subtract(totalStockCost)
                    .subtract(initStockAccountSize);
            ownerSummary.setAvailableFunds(availableFunds);
            ownerSummary.setTotalInvestment(putMarginOccupied.add(totalStockCost));

            // 计算未平仓订单的头寸占比
            for (OwnerOrder order : ownerSummary.getUnrealizedOrders()) {
                // 计算订单金额
                BigDecimal orderAmount = OwnerOrder.strikePrice(order)
                        .multiply(new BigDecimal(OwnerOrder.lotSize(order)));
                // 计算该订单的头寸占比
                BigDecimal scaleRatio = orderAmount.divide(accountSize, 4, RoundingMode.HALF_UP);
                order.getExt().put("scaleRatio", scaleRatio.toString());
                // 添加头寸比例阈值
                order.getExt().put("positionRatio", positionRatio.toString());
            }
        }

        return ownerSummary;
    }

    @Override
    public StrategySummary queryStrategySummary(String owner, String strategyId) {
        OwnerStrategy ownerStrategy = ownerStrategyDAO.queryStrategyByStrategyId(strategyId);
        if (ownerStrategy.getStatus() == 0) {
            return null;
        }
        return queryStrategySummary(owner, ownerStrategy);
    }

    /**
     * 获取下一个期权到期日
     * 
     * @param optionsStrikeDates 期权到期日列表
     * @param order              订单
     * @return 下一个期权到期日
     */
    private LocalDate getNextOptionsStrikeDate(List<OptionsStrikeDate> optionsStrikeDates, OwnerOrder order) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strikeDateStr = dateFormat.format(order.getStrikeTime());
        final LocalDate strikeDate = LocalDate.parse(strikeDateStr);
        return optionsStrikeDates.stream()
                .map(dateObj -> {
                    String strikeTime = dateObj.getStrikeTime();
                    LocalDate orderStrikeDate = LocalDate.parse(strikeTime);
                    return orderStrikeDate;
                })
                .filter(date -> date.isAfter(strikeDate))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取可以Roll的期权
     * 
     * @param order          订单
     * @param nextStrikeDate 下一个期权到期日
     * @return 可以Roll的期权
     */
    private List<Security> getRollOptionsSecurity(OwnerOrder order, LocalDate nextStrikeDate) {
        List<Security> optionsSecurityList = new ArrayList<>();
        optionsSecurityList.add(Security.of(order.getCode(), order.getMarket()));

        BigDecimal strikePrice = OwnerOrder.strikePrice(order);
        // 查询下一个期权到期日
        for (int i = 0; i <= 5; i++) {
            BigDecimal nextStrikePrice = strikePrice
                    .subtract(BigDecimal.valueOf(i))
                    .multiply(BigDecimal.valueOf(1000))
                    .setScale(0);
            // BABA250404P133000
            String date = nextStrikeDate.format(DateTimeFormatter.ofPattern("yyMMdd"));
            String code = order.getUnderlyingCode()
                    + date
                    + (OwnerOrder.isPut(order) ? "P" : "C")
                    + nextStrikePrice.toPlainString();
            optionsSecurityList.add(Security.of(code, order.getMarket()));
        }

        // OptionsManager
        return optionsSecurityList;
    }

    /**
     * 查询策略汇总
     * 
     * @param owner         所有者
     * @param ownerStrategy 策略
     * @return 策略汇总
     */
    private StrategySummary queryStrategySummary(String owner, OwnerStrategy ownerStrategy) {
        StrategySummary summary = new StrategySummary();

        summary.setStrategy(ownerStrategy);

        // 订单列表
        List<OwnerOrder> ownerOrders = ownerManager.queryStrategyOrder(ownerStrategy);
        summary.setStrategyOrders(ownerOrders);

        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        // 订单费用
        BigDecimal totalFee = tradeManager.queryTotalOrderFee(account, ownerOrders);
        summary.setTotalFee(totalFee);

        // 股票现价
        Security security = Security.of(ownerStrategy.getCode(), account.getMarket());
        SecurityQuote securityQuote = securityQuoteGateway.quote(account, security);
        BigDecimal lastDone = securityQuote.getLastDone();
        summary.setCurrentStockPrice(lastDone);

        // 股票订单
        int orderHoldStockNum = 0;
        BigDecimal totalStockCost = BigDecimal.ZERO;
        List<OwnerOrder> securityOrders = ownerOrders.stream()
                .filter(OwnerOrder::isStockOrder)
                .toList();
        for (OwnerOrder securityOrder : securityOrders) {
            TradeSide tradeSide = TradeSide.of(securityOrder.getSide());
            BigDecimal totalPrice = securityOrder.getPrice().multiply(new BigDecimal(securityOrder.getQuantity()));
            switch (tradeSide) {
                case BUY:
                case BUY_BACK:
                    orderHoldStockNum += securityOrder.getQuantity();
                    totalStockCost = totalStockCost.add(totalPrice);
                    break;
                case SELL:
                case SELL_SHORT:
                    orderHoldStockNum -= securityOrder.getQuantity();
                    totalStockCost = totalStockCost.subtract(totalPrice);
                    break;
                default:
                    break;
            }

        }

        // 股票成本
        summary.setTotalStockCost(totalStockCost);
        BigDecimal averageStockCost = orderHoldStockNum == 0
                ? BigDecimal.ZERO
                : totalStockCost.divide(new BigDecimal(orderHoldStockNum), 4, RoundingMode.HALF_UP);
        summary.setAverageStockCost(averageStockCost);

        // 初始股票数&成本
        int initialStockNum = Integer.parseInt(ownerStrategy.getExtValue(StrategyExt.INITIAL_STOCK_NUM, "0"));
        BigDecimal initialStockCost = new BigDecimal(ownerStrategy.getExtValue(StrategyExt.INITIAL_STOCK_COST, "0"));

        // 总股票持有数量
        int holdStockNum = initialStockNum + orderHoldStockNum;
        summary.setHoldStockNum(holdStockNum);

        int holdStockNumForProfit = holdStockNum - initialStockNum;
        // 当初始股票被卖后 holdStockNum 会小于 initialStockNum
        // 当holdStockNumForProfit小于0时，不计算持股收益
        if (holdStockNumForProfit < 0) {
            holdStockNumForProfit = 0;
        }

        // 未设置股票成本总盈亏 = （现价-平均成本） * 持股数量
        // 股票现价小于股票成本时总盈亏 = （股票成本-平均成本） * 持股数量
        BigDecimal holdStockPrice = lastDone;
        if (initialStockCost.compareTo(lastDone) > 0) {
            holdStockPrice = initialStockCost;
        }
        BigDecimal holdStockProfit = holdStockPrice.subtract(averageStockCost)
                .multiply(new BigDecimal(holdStockNumForProfit));
        summary.setHoldStockProfit(holdStockProfit);

        // 期权总金额
        List<OwnerOrder> allOptionsOrders = ownerOrders.stream()
                .filter(order -> OrderStatus.of(order.getStatus()).isTraded())
                .filter(OwnerOrder::isOptionsOrder)
                .toList();

        BigDecimal lotSize = new BigDecimal(ownerStrategy.getLotSize());
        // 所有期权利润
        BigDecimal allOptionsIncome = allOptionsOrders.stream()
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        allOptionsIncome = allOptionsIncome.subtract(totalFee);
        // 期权利润
        summary.setAllOptionsIncome(allOptionsIncome);

        // 总收入
        summary.setAllIncome(allOptionsIncome.add(holdStockProfit));

        // 所有未平仓的期权
        List<OwnerOrder> allOpenOptionsOrder = allOptionsOrders.stream()
                .filter(OwnerOrder::isOpen)
                .filter(OwnerOrder::isOptionsOrder)
                .filter(order -> OrderStatus.of(order.getStatus()).isValid())
                .toList();

        // 未平仓的期权利润
        BigDecimal unrealizedOptionsIncome = allOpenOptionsOrder.stream()
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);

        // 获取期权Delta
        List<Security> allOpenOptionsSecurity = allOpenOptionsOrder.stream()
                .map(order -> Security.of(order.getCode(), order.getMarket()))
                .toList();
        List<OptionsRealtimeData> allOpenOptionsRealtimeData = optionsManager
                .queryOptionsRealtimeData(allOpenOptionsSecurity);
        // 计算Delta
        Map<Security, OptionsRealtimeData> securityDeltaMap = new HashMap<>();
        for (OptionsRealtimeData realtimeData : allOpenOptionsRealtimeData) {
            securityDeltaMap.put(realtimeData.getSecurity(), realtimeData);
        }
        // 计算未平仓期权的Delta
        BigDecimal optionsDelta = BigDecimal.ZERO;
        BigDecimal optionsGamma = BigDecimal.ZERO;
        BigDecimal optionsTheta = BigDecimal.ZERO;
        for (OwnerOrder order : allOpenOptionsOrder) {
            OptionsRealtimeData realtimeData = securityDeltaMap.get(Security.of(order.getCode(), order.getMarket()));
            if (null == realtimeData) {
                continue;
            }
            BigDecimal quantity = new BigDecimal(order.getQuantity());
            // 卖出为负 买入为正
            BigDecimal side = new BigDecimal(TradeSide.of(order.getSide()).getSign() * -1);
            BigDecimal delta = realtimeData.getDelta().multiply(side).multiply(quantity);
            BigDecimal gamma = realtimeData.getGamma().multiply(side).multiply(quantity);
            BigDecimal theta = realtimeData.getTheta().multiply(side).multiply(quantity);

            optionsDelta = optionsDelta.add(delta);
            optionsGamma = optionsGamma.add(gamma);
            optionsTheta = optionsTheta.add(theta);
        }
        // 股票Delta
        BigDecimal stockDelta = BigDecimal.valueOf(holdStockNum).divide(lotSize, 4, RoundingMode.HALF_UP);
        // 策略Delta
        summary.setStrategyDelta(stockDelta.add(optionsDelta));
        // 策略Gamma(未平仓期权Gamma)
        summary.setStrategyGamma(optionsGamma);
        // 策略Theta(未平仓期权Theta)
        summary.setStrategyTheta(optionsTheta);

        // 计算PUT订单保证金占用
        String marginRatioConfig = account.getExtValue(AccountExt.MARGIN_RATIO, null);

        if (null != marginRatioConfig) {
            BigDecimal marginRatio = new BigDecimal(marginRatioConfig);
            BigDecimal putMarginOccupied = allOptionsOrders.stream()
                    .filter(OwnerOrder::isOpen)
                    .filter(OwnerOrder::isOptionsOrder)
                    .filter(OwnerOrder::isSell)
                    .map(order -> {
                        BigDecimal result = new BigDecimal(0);
                        if (OwnerOrder.isPut(order)) {
                            BigDecimal strikePrice = OwnerOrder.strikePrice(order);
                            result = strikePrice.multiply(lotSize)
                                    .multiply(BigDecimal.valueOf(order.getQuantity()))
                                    .multiply(marginRatio);
                        }
                        return result;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setPutMarginOccupied(putMarginOccupied);
        }

        // 计算未平仓订单的AI提示
        List<OwnerOrder> openOrders = ownerOrders.stream()
                .filter(OwnerOrder::isOpen)
                .filter(OwnerOrder::isOptionsOrder)
                .toList();
        // 未平仓订单处理策略
        OrderTradeStrategy defaultOrderTradeStrategy = new DefaultOrderTradeStrategy();
        for (OwnerOrder order : openOrders) {

            // 查询未平仓订单可以Roll的期权实时数据 [当前行权价格, 当前行权价格-5]
            // 查询股票期权到期日
            List<OptionsStrikeDate> optionsStrikeDates = optionsManager.queryOptionsExpDate(order.getUnderlyingCode(),
                    order.getMarket());
            LocalDate nextStrikeDate = getNextOptionsStrikeDate(optionsStrikeDates, order);
            if (null != nextStrikeDate) {
                List<Security> optionsSecurityList = getRollOptionsSecurity(order, nextStrikeDate);
                List<OptionsRealtimeData> optionsRealtimeDataList = optionsManager
                        .queryOptionsRealtimeData(optionsSecurityList);
                order.setExtValue(OrderExt.ROLL_OPTIONS, optionsRealtimeDataList);
            }

            StockIndicator stockIndicator = indicatorManager.calculateStockIndicator(account, security);
            defaultOrderTradeStrategy.calculate(account, order, stockIndicator);
        }

        return summary;
    }

}
