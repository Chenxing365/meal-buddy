package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 菜品创建请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜品录入参数")
public class DishCreateRequest {

    @NotBlank(message = "所属商家 ID 不能为空")
    @Schema(description = "所属商家 ID")
    private String merchantId;

    @NotBlank(message = "菜品名称不能为空")
    @Size(min = 2, max = 30, message = "菜品名称长度须在2~30字符之间")
    @Schema(description = "菜品名称", example = "低卡柠檬手撕鸡沙拉")
    private String name;

    @Schema(description = "风味成分描述", example = "精选优质鸡胸肉配柠檬醋汁，高蛋白少油")
    private String description;

    @NotNull(message = "标准零售价不能为空")
    @DecimalMin(value = "0.01", message = "价格不能低于0.01元")
    @Schema(description = "零售单价（元）", example = "28.5")
    private Double price;

    @Schema(description = "会员专享价（元）", example = "25.0")
    private Double memberPrice;

    @Schema(description = "菜品头图 URL")
    private String coverImage;

    @NotNull(message = "标称热量不能为空（AI 饮食模式核心筛选参数）")
    @Schema(description = "热量标称（大卡/份）", example = "380")
    private Integer calorie;

    @Schema(description = "口味标签列表", example = "[\"酸辣\", \"少油\", \"高蛋白\"]")
    private List<String> flavorTags;

    @Schema(description = "主要食材列表", example = "[\"鸡胸肉\", \"生菜\", \"小番茄\"]")
    private List<String> ingredientTags;
}
