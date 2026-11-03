package com.distributed.base.dto;

import com.distributed.base.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String orderNo;
    private String userId;
    private String merchantId;
    private String merchantName;
    private Integer fulfillType;
    private List<OrderItem> items;
    private Double totalAmount;
    private Integer status;
    private String statusDesc;
    private String address;
    private String pickupCode;
    private LocalDateTime paymentTime;
    private LocalDateTime createdAt;
}
