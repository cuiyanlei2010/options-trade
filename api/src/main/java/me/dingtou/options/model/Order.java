package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单
 *
 * @author qiyan
 */
@Data
public class Order {
    /**
     * 证券
     */
    private Security security;

    /**
     * 买卖方向 1:买  2:卖
     */
    private Integer side;
    /**
     * 订单价格
     */
    private BigDecimal price;

    /**
     * 订单数量
     */
    private Long quantity;
}
