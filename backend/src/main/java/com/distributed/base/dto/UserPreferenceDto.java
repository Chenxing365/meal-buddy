package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户偏好设置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户饮食推荐偏好")
public class UserPreferenceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = "^(slim|normal|indulge)$", message = "推荐模式仅支持: slim(减肥), normal(正常), indulge(放纵)")
    @Schema(description = "推荐模式: slim(减肥)/normal(正常)/indulge(放纵)", example = "slim")
    private String recommendMode;

    @Schema(description = "口味标签列表", example = "[\"川味\", \"清淡\", \"少油\"]")
    private List<String> flavorTags;

    @Schema(description = "单餐热量上限（大卡）", example = "500")
    private Integer calorieLimit;

    @Schema(description = "忌口/过敏原标签列表", example = "[\"花生\", \"海鲜\"]")
    private List<String> allergyTags;
}
