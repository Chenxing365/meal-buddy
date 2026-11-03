package com.distributed.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String orderNo;
    private Integer status;
    private Double totalAmount;
    private LocalDateTime paymentTime;
    private String pickupCode;
}
