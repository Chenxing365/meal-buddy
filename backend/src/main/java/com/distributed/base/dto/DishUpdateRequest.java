package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 菜品修改请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜品信息修改参数")
public class DishUpdateRequest {

    @Size(min = 2, max = 30, message = "菜品名称长度须在2~30字符之间")
    @Schema(description = "菜品名称")
    private String name;

    @Schema(description = "风味成分描述")
    private String description;

    @DecimalMin(value = "0.01", message = "价格不能低于0.01元")
    @Schema(description = "零售单价（元）")
    private Double price;

    @Schema(description = "会员专享价（元）")
    private Double memberPrice;

    @Schema(description = "菜品头图 URL")
    private String coverImage;

    @Schema(description = "热量标称（大卡/份）")
    private Integer calorie;

    @Schema(description = "口味标签列表")
    private List<String> flavorTags;

    @Schema(description = "主要食材列表")
    private List<String> ingredientTags;
}
