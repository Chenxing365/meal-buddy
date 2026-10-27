package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家基本信息响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商家门店响应对象")
public class MerchantResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家 ID")
    private String id;

    @Schema(description = "绑定的管理员/商家账号 ID")
    private String userId;

    @Schema(description = "门店名称")
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

    @Schema(description = "平台抽成比例")
    private Double commissionRate;

    @Schema(description = "状态：0-待审核，1-正常营业，2-歇业打烊，3-审核驳回")
    private Integer status;

    @Schema(description = "驳回理由")
    private String rejectReason;

    @Schema(description = "申请入驻时间")
    private LocalDateTime createdAt;
}
