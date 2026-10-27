package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜品响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜品详情响应对象")
public class DishResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品 ID")
    private String id;

    @Schema(description = "所属商家 ID")
    private String merchantId;

    @Schema(description = "菜品名称")
    private String name;

    @Schema(description = "风味描述")
    private String description;

    @Schema(description = "零售单价")
    private Double price;

    @Schema(description = "会员专享价")
    private Double memberPrice;

    @Schema(description = "菜品头图 URL")
    private String coverImage;

    @Schema(description = "热量标称（大卡）")
    private Integer calorie;

    @Schema(description = "口味标签列表")
    private List<String> flavorTags;

    @Schema(description = "主要食材列表")
    private List<String> ingredientTags;

    @Schema(description = "状态：0-下架售罄，1-正常上架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
