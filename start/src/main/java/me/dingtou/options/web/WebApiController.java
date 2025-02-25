package me.dingtou.options.web;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.*;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.service.OptionsQueryService;
import me.dingtou.options.service.OptionsTradeService;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * API 控制器
 *
 * @author qiyan
 */
@Slf4j
@RestController
public class WebApiController {

    @Autowired
    private AuthService authService;


    @Autowired
    private OptionsQueryService optionsQueryService;

    @Autowired
    private OptionsTradeService optionsTradeService;


    /**
     * 查询当前用户的证券和策略
     *
     * @return 用户的证券和策略
     */
    @RequestMapping(value = "/owner/get", method = RequestMethod.GET)
    public WebResult<Owner> queryOwner() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return WebResult.success(optionsQueryService.queryOwner(owner));
    }

    /**
     * 查询当前用户的证券和策略汇总
     *
     * @return 汇总信息
     */
    @RequestMapping(value = "/owner/summary", method = RequestMethod.GET)
    public WebResult<OwnerSummary> queryOwnerSummary() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return WebResult.success(optionsQueryService.queryOwnerSummary(owner));
    }

    /**
     * 查询证券的期权链到期日
     *
     * @param security 证券
     * @return 期权链到期日
     */
    @RequestMapping(value = "/options/strike/list", method = RequestMethod.GET)
    public WebResult<List<OptionsStrikeDate>> listOptionsExpDate(Security security) {
        log.info("list strike. security:{}", security);
        if (null == security || StringUtils.isEmpty(security.getCode())) {
            return WebResult.success(Collections.emptyList());
        }
        List<OptionsStrikeDate> optionsStrikeDates = optionsQueryService.queryOptionsExpDate(security);
        if (null == optionsStrikeDates || optionsStrikeDates.isEmpty()) {
            return WebResult.success(Collections.emptyList());
        }
        return WebResult.success(optionsStrikeDates);
    }

    /**
     * 查询期权链
     *
     * @param market                   市场
     * @param code                     证券代码
     * @param strikeTime               期权链到期日
     * @param strikeTimestamp          到期时间戳
     * @param optionExpiryDateDistance 到期天数
     * @return 期权链
     */
    @RequestMapping(value = "/options/chain/get", method = RequestMethod.GET)
    public WebResult<OptionsChain> listOptionsChain(@RequestParam(value = "market", required = true) Integer market, @RequestParam(value = "code", required = true) String code, @RequestParam(value = "strikeTime", required = true) String strikeTime, @RequestParam(value = "strikeTimestamp", required = true) Long strikeTimestamp, @RequestParam(value = "optionExpiryDateDistance", required = true) Integer optionExpiryDateDistance, @RequestParam(value = "strategyId", required = false) String strategyId) throws Exception {
        log.info("get options chain. market:{}, code:{}, strikeTime:{}, strikeTimestamp:{}, optionExpiryDateDistance:{}, strategyId:{}", market, code, strikeTime, strikeTimestamp, optionExpiryDateDistance, strategyId);
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);

        OptionsStrikeDate optionsStrikeDate = new OptionsStrikeDate();
        optionsStrikeDate.setStrikeTime(strikeTime);
        optionsStrikeDate.setStrikeTimestamp(strikeTimestamp);
        optionsStrikeDate.setOptionExpiryDateDistance(optionExpiryDateDistance);

        // 当传入策略ID，则查询对应的策略进行期权链处理。
        Owner owner = optionsQueryService.queryOwner(SessionUtils.getCurrentOwner());
        OwnerStrategy strategy = null;
        if (null != owner && null != strategyId) {
            Optional<? extends OwnerStrategy> ownerStrategy = owner.getStrategyList().stream().filter(item -> item.getStrategyId().equals(strategyId)).findFirst();
            if (ownerStrategy.isPresent()) {
                strategy = ownerStrategy.get();
            }
        }

        try {
            OptionsChain optionsChain = optionsQueryService.queryOptionsChain(security, optionsStrikeDate, strategy);
            return WebResult.success(optionsChain);
        } catch (Exception e) {
            log.error("get options chain error. market:{}, code:{}, strikeTime:{}, message:{}", market, code, strikeTime, e.getMessage(), e);
            return WebResult.failure(e.getMessage());
        }
    }


    @RequestMapping(value = "/options/strategy/get", method = RequestMethod.GET)
    public WebResult<StrategySummary> queryStrategySummary(@RequestParam(value = "strategyId", required = true) String strategyId) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return WebResult.success(optionsQueryService.queryStrategySummary(owner, strategyId));
    }


    @RequestMapping(value = "/options/orderbook/get", method = RequestMethod.GET)
    public WebResult<SecurityOrderBook> listOrderBook(@RequestParam(value = "market", required = true) Integer market, @RequestParam(value = "code", required = true) String code) throws Exception {
        log.info("get orderbook. market:{}, code:{}", market, code);
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);
        return WebResult.success(optionsQueryService.queryOrderBook(security));
    }


    @RequestMapping(value = "/trade/submit", method = RequestMethod.POST)
    public WebResult<OwnerOrder> submit(@RequestParam(value = "side", required = true) Integer side, @RequestParam(value = "strategyId", required = true) String strategyId, @RequestParam(value = "quantity", required = true) Integer quantity, @RequestParam(value = "price", required = true) String price, @RequestParam(value = "options", required = true) String options, @RequestParam(value = "password", required = true) String password) throws Exception {

        String owner = SessionUtils.getCurrentOwner();
        log.info("trade submit. owner:{}, side:{}, quantity:{}, price:{}, options:{}", owner, side, quantity, price, options);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        Options optionsObj = JSON.parseObject(options, Options.class);
        BigDecimal sellPrice = new BigDecimal(price);
        return WebResult.success(optionsTradeService.submit(strategyId, TradeSide.of(side), quantity, sellPrice, optionsObj));
    }


    @RequestMapping(value = "/trade/order/draft", method = RequestMethod.GET)
    public WebResult<List<OwnerOrder>> queryDraftOrder(@RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("query order draft. owner:{}, password:{}", owner, password);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        return WebResult.success(optionsQueryService.queryDraftOrder(owner));
    }


    @RequestMapping(value = "/trade/close", method = RequestMethod.POST)
    public WebResult<OwnerOrder> close(@RequestParam(value = "price", required = true) String price, @RequestParam(value = "order", required = true) String order, @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade close. owner:{}, price:{}, order:{}", owner, price, order);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        OwnerOrder orderObj = JSON.parseObject(order, OwnerOrder.class);
        String loginOwner = SessionUtils.getCurrentOwner();
        if (!loginOwner.equals(orderObj.getOwner())) {
            return WebResult.failure("账号信息错误");
        }
        return WebResult.success(optionsTradeService.close(orderObj, new BigDecimal(price)));
    }


    @RequestMapping(value = "/trade/modify", method = RequestMethod.POST)
    public WebResult<OwnerOrder> modify(@RequestParam(value = "action", required = true) String action, @RequestParam(value = "order", required = true) String order, @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade modify. owner:{}, action:{}, order:{}", owner, action, order);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        OrderAction orderAction = OrderAction.of(action);
        OwnerOrder orderObj = JSON.parseObject(order, OwnerOrder.class);
        if (!owner.equals(orderObj.getOwner())) {
            return WebResult.failure("订单Owner不匹配");
        }
        return WebResult.success(optionsTradeService.modify(orderObj, orderAction));
    }

    @RequestMapping(value = "/trade/sync", method = RequestMethod.GET)
    public WebResult<Boolean> sync(@RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade sync. owner:{}", owner);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        return WebResult.success(optionsTradeService.sync(owner));
    }

    @RequestMapping(value = "/trade/update", method = RequestMethod.POST)
    public WebResult<Integer> updateOrderStrategy(@RequestParam(value = "strategyId", required = true) String strategyId, @RequestParam(value = "orderIds", required = true) List<Long> orderIds, @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade update. owner:{}, strategyId:{}, orderIds:{}", owner, strategyId, orderIds);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        if (orderIds.isEmpty()) {
            return WebResult.success(0);
        }
        return WebResult.success(optionsTradeService.updateOrderStrategy(owner, orderIds, strategyId));
    }

}
