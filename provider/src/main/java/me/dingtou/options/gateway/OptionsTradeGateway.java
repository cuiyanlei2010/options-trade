package me.dingtou.options.gateway;

import me.dingtou.options.model.OwnerOrder;

/**
 * 期权交易
 *
 * @author qiyan
 */
public interface OptionsTradeGateway {

    /**
     * 交易
     *
     * @param order 订单
     * @return 外部订单号
     */
    String trade(OwnerOrder order);

    /**
     * 取消订单
     *
     * @param order 订单
     * @return 订单
     */
    OwnerOrder cancel(OwnerOrder order);
}
