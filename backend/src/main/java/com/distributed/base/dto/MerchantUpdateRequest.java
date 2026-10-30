package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商家信息修改请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商家门店信息修改参数")
public class MerchantUpdateRequest {

    @Schema(description = "门店名称", example = "老成都轻食坊（升级版）")
    private String name;

    @Schema(description = "门店 Logo 链接")
    private String logo;

    @Schema(description = "实体门店地址")
    private String address;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "经营品类")
    private String category;

    @Schema(description = "营业状态：1-正常营业，2-打烊歇业")
    private Integer status;
}
