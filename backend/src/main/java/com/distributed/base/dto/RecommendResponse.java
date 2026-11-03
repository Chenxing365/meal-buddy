package com.distributed.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 智能推荐接口响应体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 推荐日志 ID */
    private String logId;

    /** 推荐模式：slim / normal / indulge */
    private String mode;

    /** 推荐理由 */
    private String matchReason;

    /** 推荐菜品列表 */
    private List<DishResponse> dishes;

    /** 规则引擎快照 */
    private Map<String, Object> ruleSnapshot;

    /** 反馈状态：0-未反馈，1-赞，-1-踩 */
    private Integer feedback;

    /** 生成时间 */
    private LocalDateTime createdAt;
}
