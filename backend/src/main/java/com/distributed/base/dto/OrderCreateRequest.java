package com.distributed.base.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotBlank(message = "商家门店 ID 不能为空")
    private String merchantId;

    /** 0-外卖配送，1-到店自提，2-堂食就餐 */
    @NotNull(message = "履约方式不能为空")
    @Min(value = 0, message = "履约方式非法（0-外卖配送，1-到店自提，2-堂食就餐）")
    @Max(value = 2, message = "履约方式非法（0-外卖配送，1-到店自提，2-堂食就餐）")
    private Integer fulfillType;

    @NotEmpty(message = "下单菜品清单不能为空")
    @Valid
    private List<OrderItemRequest> items;

    /** 外卖配送地址或就餐桌号备注 */
    private String address;
}
