package com.distributed.base.service;

import com.distributed.base.dto.*;
import org.springframework.data.domain.Page;

/**
 * 订单交易与核销履约业务服务接口
 */
public interface OrderService {

    /**
     * 买家提交订单
     *
     * @param userId 买家用户 ID
     * @param req 下单请求
     * @return 订单响应详情（包含核销码）
     */
    OrderResponse createOrder(String userId, OrderCreateRequest req);

    /**
     * 模拟订单支付
     *
     * @param userId 买家用户 ID
     * @param orderId 订单 ID
     * @return 支付结果响应
     */
    OrderPayResponse payOrder(String userId, String orderId);

    /**
     * 商家核销到店自提/堂食订单
     *
     * @param merchantUserId 商家用户 ID
     * @param req 核销码及核销方式请求
     * @return 核销结果
     */
    VerifyResponse verifyOrder(String merchantUserId, VerifyRequest req);

    /**
     * 查询订单详情
     *
     * @param userId 操作人 ID
     * @param orderId 订单 ID
     * @return 订单详情
     */
    OrderResponse getOrderDetail(String userId, String orderId);

    /**
     * 买家分页查看历史订单
     *
     * @param userId 买家 ID
     * @param page 页码
     * @param size 每页条数
     * @return 分页订单列表
     */
    Page<OrderResponse> listUserOrders(String userId, int page, int size);

    /**
     * 商家分页查看门店订单
     *
     * @param merchantUserId 商家账号 ID
     * @param page 页码
     * @param size 每页条数
     * @return 分页订单列表
     */
    Page<OrderResponse> listMerchantOrders(String merchantUserId, int page, int size);

    /**
     * 商家查看经营大盘数据（当日订单量、营业额、核销率等）
     *
     * @param merchantUserId 商家账号 ID
     * @return 经营大盘数据
     */
    MerchantDashboardResponse getMerchantDashboard(String merchantUserId);
}
