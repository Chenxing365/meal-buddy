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

/**
 * 外卖配送单文档实体
 *
 * <p>对应 MongoDB 集合：{@code delivery_records}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "delivery_records")
public class DeliveryRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配送主键 ID */
    @Id
    private String id;

    /** 关联订单 ID（唯一） */
    @Indexed(unique = true)
    @Field("order_id")
    private String orderId;

    /** 接单骑手 ID */
    @Indexed
    @Field("rider_id")
    private String riderId;

    /** 配送状态：0-待接单，1-配送中，2-已送达 */
    @Builder.Default
    @Indexed
    @Field("delivery_status")
    private Integer deliveryStatus = 0;

    /** 配送目标地址 */
    @Field("address")
    private String address;

    /** 顾客联系电话 */
    @Field("contact_phone")
    private String contactPhone;

    /** 创建时间 */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
