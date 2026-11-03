package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单交易主文档实体
 *
 * <p>对应 MongoDB 集合：{@code orders}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单主键 ID */
    @Id
    private String id;

    /** 全局唯一业务订单号（例如 ORD20261103...） */
    @Indexed(unique = true)
    @Field("order_no")
    private String orderNo;

    /** 下单买家用户 ID */
    @Indexed
    @Field("user_id")
    private String userId;

    /** 接单商家门店 ID */
    @Indexed
    @Field("merchant_id")
    private String merchantId;

    /** 履约方式：0-外卖配送，1-到店自提，2-堂食就餐 */
    @Field("fulfill_type")
    private Integer fulfillType;

    /** 菜品明细快照 */
    @Builder.Default
    @Field("items")
    private List<OrderItem> items = new ArrayList<>();

    /** 订单总支付金额（元） */
    @Field("total_amount")
    private Double totalAmount;

    /** 状态机：0-待支付，1-已支付，2-配送中，3-已核销，4-已完成，5-已取消 */
    @Builder.Default
    @Indexed
    @Field("status")
    private Integer status = 0;

    /** 外卖收货地址 / 堂食桌号说明 */
    @Field("address")
    private String address;

    /** 6 位自提/堂食核销码（核销后置空或标记失效） */
    @Indexed
    @Field("pickup_code")
    private String pickupCode;

    /** 支付完成时间 */
    @Field("payment_time")
    private LocalDateTime paymentTime;

    /** 下单创建时间 */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
