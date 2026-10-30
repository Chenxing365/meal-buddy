package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 商家详情与菜品清单聚合响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商家门店详情及在售菜品列表")
public class MerchantDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家门店基本资料")
    private MerchantResponse merchant;

    @Schema(description = "门店在售菜品清单")
    private List<DishResponse> dishes;
}
