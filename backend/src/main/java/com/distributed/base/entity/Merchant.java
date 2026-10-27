package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家门店实体
 *
 * <p>对应 MongoDB 集合：{@code merchants}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "merchants")
public class Merchant implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商家主键 ID */
    @Id
    private String id;

    /** 绑定的管理员/商家账号 ID（一对一，唯一索引） */
    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    /** 门店名称（5~50字符） */
    @Field("name")
    private String name;

    /** 门店 Logo 链接 */
    @Field("logo")
    private String logo;

    /** 实体门店地址 */
    @Field("address")
    private String address;

    /** 经纬度地理坐标（GeoJSON Point: [lng, lat]） */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    @Field("location")
    private GeoJsonPoint location;

    /** 经营分类（快餐小吃、火锅、轻食沙拉等） */
    @Indexed
    @Field("category")
    private String category;

    /** 平台抽成比例（默认 5%） */
    @Builder.Default
    @Field("commission_rate")
    private Double commissionRate = 0.05;

    /** 状态：0-待审核，1-正常营业，2-歇业打烊，3-审核驳回 */
    @Builder.Default
    @Indexed
    @Field("status")
    private Integer status = 0;

    /** 审核驳回理由 */
    @Field("reject_reason")
    private String rejectReason;

    /** 申请入驻时间 */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** 最后变更时间 */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
