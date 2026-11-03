package com.distributed.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDashboardResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 门店 ID */
    private String merchantId;

    /** 门店名称 */
    private String merchantName;

    /** 今日订单量 */
    private Long todayOrderCount;

    /** 今日实收营业额（元） */
    private Double todayRevenue;

    /** 今日已核销订单量 */
    private Long todayVerifiedCount;

    /** 今日核销履约率（如 0.85 即 85%） */
    private Double todayVerifyRate;

    /** 历史累计订单量 */
    private Long totalOrderCount;
}
