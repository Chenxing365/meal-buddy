package com.distributed.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体推荐内核输出结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 命中的菜品 ID 列表 */
    @Builder.Default
    private List<String> dishIds = new ArrayList<>();

    /** 命中的菜品实体响应列表 */
    @Builder.Default
    private List<DishResponse> dishes = new ArrayList<>();

    /** 推荐理由说明与 AI 润色话术 */
    private String matchReason;

    /** 推荐规则快照（可解释性） */
    @Builder.Default
    private Map<String, Object> ruleSnapshot = new HashMap<>();
}
