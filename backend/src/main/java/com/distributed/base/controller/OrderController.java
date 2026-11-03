package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.OrderCreateRequest;
import com.distributed.base.dto.OrderPayResponse;
import com.distributed.base.dto.OrderResponse;
import com.distributed.base.service.OrderService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 订单交易履约 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单交易履约接口", description = "买家下单、模拟支付、订单详情与用户历史订单查询")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "提交创建订单", description = "买家创建外卖/自提/堂食订单，自提堂食自动生成6位核销码")
    public Result<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest req) {
        String userId = UserContext.getUserId();
        OrderResponse response = orderService.createOrder(userId, req);
        return Result.ok("订单创建成功", response);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "模拟订单支付", description = "流转订单状态为已支付，记录系统运维审计日志")
    public Result<OrderPayResponse> payOrder(@PathVariable("id") String id) {
        String userId = UserContext.getUserId();
        OrderPayResponse response = orderService.payOrder(userId, id);
        return Result.ok("支付成功，正在为您安排履约制作", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询订单详情", description = "买家/商家查询订单履约与核销码状态")
    public Result<OrderResponse> getOrderDetail(@PathVariable("id") String id) {
        String userId = UserContext.getUserId();
        OrderResponse response = orderService.getOrderDetail(userId, id);
        return Result.ok(response);
    }

    @GetMapping("/list")
    @Operation(summary = "买家查看历史订单列表", description = "按时间倒序分页检索登录用户的全部订单")
    public Result<Page<OrderResponse>> listOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        String userId = UserContext.getUserId();
        Page<OrderResponse> orderPage = orderService.listUserOrders(userId, page, size);
        return Result.ok(orderPage);
    }
}
