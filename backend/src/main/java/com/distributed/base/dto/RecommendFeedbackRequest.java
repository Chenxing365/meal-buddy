package com.distributed.base.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐结果点赞/点踩反馈请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendFeedbackRequest {

    @NotBlank(message = "推荐记录 ID 不能为空")
    private String logId;

    /** 1-赞，-1-踩 */
    @NotNull(message = "态度反馈不能为空")
    @Min(value = -1, message = "反馈状态非法（-1为踩，1为赞）")
    @Max(value = 1, message = "反馈状态非法（-1为踩，1为赞）")
    private Integer feedback;
}
