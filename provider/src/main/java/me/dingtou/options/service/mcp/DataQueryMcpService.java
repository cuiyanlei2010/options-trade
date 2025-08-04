package me.dingtou.options.service.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.manager.IndicatorManager;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.IndicatorDataFrame;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityCandlestick;
import me.dingtou.options.model.SecurityOrderBook;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.VixIndicator;
import me.dingtou.options.model.EarningsCalendar;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.service.EarningsCalendarService;
import me.dingtou.options.util.IndicatorDataFrameUtil;
import me.dingtou.options.util.TemplateRenderer;

@Service
public class DataQueryMcpService {

    @Autowired
    private AuthService authService;

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private IndicatorManager indicatorManager;

    @Autowired
    private EarningsCalendarService earningsCalendarService;

    @Tool(description = "查询期权到期日，根据股票代码和市场代码查询股票对应的期权到期日。返回结果包括股票代码、市场代码和到期日列表。")
    public String queryOptionsExpDate(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        String encodeOwner = authService.decodeOwner(ownerCode);
        if (null == encodeOwner) {
            return "用户编码信息不正确或已经过期";
        }
        List<OptionsStrikeDate> expDates = optionsManager.queryOptionsExpDate(code, market);
        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("security", Security.of(code, market));
        data.put("expDates", expDates);
        // 渲染模板
        return TemplateRenderer.render("mcp_options_exp_date.ftl", data);
    }

    @Tool(description = "查询指定日期期权链，根据股票代码、市场代码、到期日查询股票对应的期权数据。返回结果包括期权代码、类型(Call/Put)、行权价、当前价格、隐含波动率、Delta、Theta、Gamma、未平仓合约数、当天交易量等信息。")
    public String queryOptionsChain(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market,
            @ToolParam(required = true, description = "期权到期日 2025-06-27") String strikeDate) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        OptionsChain optionsChain = optionsManager.queryOptionsChain(account, Security.of(code, market), strikeDate,
                false);

        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("security", optionsChain.getSecurity());
        data.put("strikeTime", optionsChain.getStrikeTime());
        data.put("optionsList", optionsChain.getOptionsList());

        // 渲染模板
        return TemplateRenderer.render("mcp_options_chain.ftl", data);
    }

    @Tool(description = "查询股票技术指标，根据股票代码、市场代码查询股票技术指标。返回结果包括标的行情（最新价、周波动幅度、月波动幅度）、近70个交易日的K线数据（日期、开盘价、收盘价、最高价、最低价、成交量、成交额）以及近20个交易日的技术指标（EMA、BOLL、MACD、RSI）。")
    public String queryStockIndicator(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        Security security = Security.of(code, market);
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        StockIndicator stockIndicator = indicatorManager.calculateStockIndicator(account, security);
        IndicatorDataFrame stockIndicatorDataFrame = IndicatorDataFrameUtil.createDataFrame(stockIndicator, 20);
        Map<String, Object> data = new HashMap<>();
        data.put("security", security);
        data.put("stockIndicator", stockIndicator);
        data.put("stockIndicatorDataFrame", stockIndicatorDataFrame);
        // 渲染模板
        return TemplateRenderer.render("mcp_stock_indicator.ftl", data);
    }

    @Tool(description = "查询VIX恐慌指数指标，包含当前VIX和标普500的值以及近期走势。返回结果包括当前VIX值、日期、日变动，以及标普500的值和日期。")
    public String queryVixIndicator() {
        VixIndicator vixIndicator = indicatorManager.queryCurrentVix();
        if (null == vixIndicator) {
            return "查询VIX恐慌指数指标失败，请稍后再试。";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("vixIndicator", vixIndicator);
        // 渲染模板
        String result = TemplateRenderer.render("mcp_vix_indicator.ftl", data);
        return result;
    }

    @Tool(description = "查询指定期权代码的报价，根据期权代码、市场代码查询当前期权买卖报价。返回结果包括期权代码和买卖盘价格列表。")
    public String queryOrderBook(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "期权代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {

        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        SecurityOrderBook orderBook = tradeManager.querySecurityOrderBook(code, market);
        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("security", Security.of(code, market));
        data.put("orderBook", orderBook);
        // 渲染模板
        return TemplateRenderer.render("mcp_order_book.ftl", data);
    }

    @Tool(description = "查询指股票代码的当前价格，根据股票代码、市场代码查询当前股票价格。")
    public String queryStockRealPrice(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        return indicatorManager.queryStockPrice(account, Security.of(code, market)).toPlainString();
    }

    @Tool(description = "查询股票K线数据（日K、周K、月K），根据股票代码、市场代码、K线类型、K线数量查询股票对应K线数据。返回结果包括日期、开盘价、收盘价、最高价、最低价、成交量、成交额。")
    public String queryStockCandlesticks(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market,
            @ToolParam(required = true, description = "K线类型 1000:日K 2000:周K 3000:月K") Integer periodCode,
            @ToolParam(required = true, description = "K线数量") Integer count) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        Security security = Security.of(code, market);
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        CandlestickPeriod period = CandlestickPeriod.of(periodCode);
        SecurityCandlestick candlesticks = indicatorManager.getCandlesticks(account, security, period, count);
        Map<String, Object> data = new HashMap<>();
        data.put("security", security);
        data.put("periodName", period.getName());
        data.put("count", count);
        data.put("candlesticks", candlesticks.getCandlesticks());
        // 渲染模板
        return TemplateRenderer.render("mcp_stock_candlesticks.ftl", data);
    }

    @Tool(description = "查询股票财报日历，根据股票代码查询股票的财报发布信息。返回结果包括股票代码、公司名称、财报日期、预期每股收益等信息。")
    public String queryEarningsCalendar(@ToolParam(required = true, description = "股票代码，例如：BABA") String code) {
        List<EarningsCalendar> earningsCalendars = earningsCalendarService.getEarningsCalendarBySymbol(code);
        if (StringUtils.isBlank(code)) {
            return "股票代码不能为空";
        }
        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("symbol", code);
        data.put("earningsCalendars", earningsCalendars);
        // 渲染模板
        return TemplateRenderer.render("mcp_earnings_calendar.ftl", data);
    }

}
