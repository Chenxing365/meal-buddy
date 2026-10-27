package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
 * 用户饮食偏好实体
 *
 * <p>对应 MongoDB 集合：{@code user_preferences}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_preferences")
public class UserPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @Id
    private String id;

    /** 关联的用户 ID（一对一，唯一索引） */
    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    /** 推荐模式：slim（减肥）/ normal（正常）/ indulge（放纵） */
    @Builder.Default
    @Field("recommend_mode")
    private String recommendMode = "normal";

    /** 口味标签数组，如 ["川味", "清淡", "微辣"] */
    @Builder.Default
    @Field("flavor_tags")
    private List<String> flavorTags = new ArrayList<>();

    /** 热量阈值（大卡/餐）：减肥≤500，正常≤800，放纵无限制 */
    @Builder.Default
    @Field("calorie_limit")
    private Integer calorieLimit = 800;

    /** 忌口/过敏原标签数组，如 ["花生", "海鲜", "香菜"] */
    @Builder.Default
    @Field("allergy_tags")
    private List<String> allergyTags = new ArrayList<>();

    /** 最后更新时间 */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
