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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 推荐与决策日志实体
 *
 * <p>对应 MongoDB 集合：{@code recommend_logs}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recommend_logs")
public class RecommendLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志主键 ID */
    @Id
    private String id;

    /** 接收推荐的用户 ID */
    @Indexed
    @Field("user_id")
    private String userId;

    /** 触发模式：slim / normal / indulge */
    @Field("mode")
    private String mode;

    /** 命中并推荐的菜品 ID 数组 */
    @Builder.Default
    @Field("dish_ids")
    private List<String> dishIds = new ArrayList<>();

    /** 推荐理由（规则说明 + AI 包装话术） */
    @Field("match_reason")
    private String matchReason;

    /** 规则引擎过滤权重快照（保证推荐可解释性） */
    @Builder.Default
    @Field("rule_snapshot")
    private Map<String, Object> ruleSnapshot = new HashMap<>();

    /** 用户态度反馈：0-未反馈，1-赞，-1-踩 */
    @Builder.Default
    @Field("feedback")
    private Integer feedback = 0;

    /** 推荐生成时间 */
    @CreatedDate
    @Indexed
    @Field("created_at")
    private LocalDateTime createdAt;
}
