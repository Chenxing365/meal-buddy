package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单快照菜品明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜品 ID */
    private String dishId;

    /** 菜品名称快照 */
    private String name;

    /** 购买单价快照 */
    private Double price;

    /** 购买数量 */
    private Integer quantity;
}
