package com.distributed.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRequest {

    /** 6 位自提/堂食核销码 */
    @NotBlank(message = "核销码不能为空")
    private String pickupCode;

    /** 0-扫码核销，1-手动输码 */
    @Builder.Default
    private Integer verifyType = 1;
}
