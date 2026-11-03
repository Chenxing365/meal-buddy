package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 到店自提/堂食核销记录文档实体
 *
 * <p>对应 MongoDB 集合：{@code verify_records}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "verify_records")
public class VerifyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 核销记录主键 ID */
    @Id
    private String id;

    /** 关联订单 ID（唯一索引，一单仅能核销一次） */
    @Indexed(unique = true)
    @Field("order_id")
    private String orderId;

    /** 执行核销的商家门店 ID */
    @Indexed
    @Field("merchant_id")
    private String merchantId;

    /** 核销操作人（商家账号/店员标识） */
    @Field("operator")
    private String operator;

    /** 核销方式：0-扫码枪/摄像头扫码，1-手动输码 */
    @Builder.Default
    @Field("verify_type")
    private Integer verifyType = 1;

    /** 核销完成时间戳 */
    @CreatedDate
    @Field("verified_at")
    private LocalDateTime verifiedAt;
}
