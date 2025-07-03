package me.dingtou.options.service.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.manager.IndicatorManager;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityOrderBook;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.VixIndicator;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.util.TemplateRenderer;

@Service
public class DataQueryMcpServiceImpl implements DataQueryMcpService {

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

    @Tool(description = "查询期权到期日，根据股票代码和市场代码查询股票对应的期权到期日。")
    @Override
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

    @Tool(description = "查询指定日期期权链，根据股票代码、市场代码、到期日查询股票对应的期权数据（价格、成交、Delta、Delta、Gamma、Theta）。")
    @Override
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

    @Tool(description = "查询股票技术指标，根据股票代码、市场代码查询股票技术指标（K线、EMA、BOLL、MACD、RSI）。")
    @Override
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
        VixIndicator vixIndicator = indicatorManager.queryCurrentVix();
        Map<String, Object> data = new HashMap<>();
        data.put("security", security);
        data.put("vixIndicator", vixIndicator);
        data.put("stockIndicator", stockIndicator);
        // 渲染模板
        return TemplateRenderer.render("mcp_stock_indicator.ftl", data);
    }

    @Tool(description = "查询VIX恐慌指数指标，包含当前VIX和标普500的值以及近期走势。")
    @Override
    public String queryVixIndicator() {
        VixIndicator vixIndicator = indicatorManager.queryCurrentVix();
        if(null == vixIndicator){
            return "查询VIX恐慌指数指标失败，请稍后再试。";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("vixIndicator", vixIndicator);
        // 渲染模板
        String result = TemplateRenderer.render("mcp_vix_indicator.ftl", data);
        return result;
    }

    @Tool(description = "查询指定期权代码的报价，根据期权代码、市场代码查询当前期权买卖报价。")
    @Override
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
    @Override
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

}
