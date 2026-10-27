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
 * 菜品实体
 *
 * <p>对应 MongoDB 集合：{@code dishes}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dishes")
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜品主键 ID */
    @Id
    private String id;

    /** 所属商家 ID */
    @Indexed
    @Field("merchant_id")
    private String merchantId;

    /** 菜品名称（2~30字符） */
    @Field("name")
    private String name;

    /** 菜品风味成分介绍 */
    @Field("description")
    private String description;

    /** 标准零售价（≥0.01元） */
    @Field("price")
    private Double price;

    /** 会员专享特价 */
    @Field("member_price")
    private Double memberPrice;

    /** 菜品头图 URL */
    @Field("cover_image")
    private String coverImage;

    /** 热量标称（大卡），AI 模式核心筛选字段 */
    @Indexed
    @Field("calorie")
    private Integer calorie;

    /** 口味标签：如 ["酸辣", "少油"] */
    @Indexed
    @Builder.Default
    @Field("flavor_tags")
    private List<String> flavorTags = new ArrayList<>();

    /** 主要食材成分标签 */
    @Builder.Default
    @Field("ingredient_tags")
    private List<String> ingredientTags = new ArrayList<>();

    /** 状态：0-下架售罄，1-正常上架 */
    @Builder.Default
    @Indexed
    @Field("status")
    private Integer status = 1;

    /** 逻辑删除标记 */
    @Builder.Default
    @Field("is_deleted")
    private Boolean isDeleted = false;

    /** 创建时间 */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** 最后变更时间 */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
