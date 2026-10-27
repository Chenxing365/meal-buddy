package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商家入驻申请请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商家入驻资质申请参数")
public class MerchantApplyRequest {

    @NotBlank(message = "门店名称不能为空")
    @Size(min = 5, max = 50, message = "门店名称长度须在5~50字符之间")
    @Schema(description = "门店名称", example = "老成都地道轻食川菜馆")
    private String name;

    @Schema(description = "门店 Logo 链接", example = "https://example.com/logo.jpg")
    private String logo;

    @NotBlank(message = "门店实体地址不能为空")
    @Schema(description = "实体门店地址", example = "成都市锦江区东大街66号")
    private String address;

    @NotNull(message = "经度不能为空")
    @Schema(description = "门店所在经度", example = "104.066541")
    private Double longitude;

    @NotNull(message = "纬度不能为空")
    @Schema(description = "门店所在纬度", example = "30.572269")
    private Double latitude;

    @NotBlank(message = "经营分类不能为空")
    @Schema(description = "分类（快餐小吃、轻食沙拉、川味火锅等）", example = "轻食沙拉")
    private String category;
}
