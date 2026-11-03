package com.distributed.base.service.impl;

import com.distributed.base.dto.*;
import com.distributed.base.entity.*;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.*;
import com.distributed.base.service.OrderService;
import com.distributed.base.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单交易履约与核销服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final VerifyRecordRepository verifyRecordRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final MerchantRepository merchantRepository;
    private final DishRepository dishRepository;
    private final SystemService systemService;

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, OrderCreateRequest req) {
        log.info("[OrderService] 创建订单: userId={}, merchantId={}, fulfillType={}", userId, req.getMerchantId(), req.getFulfillType());

        Merchant merchant = merchantRepository.findById(req.getMerchantId())
                .orElseThrow(() -> new BusinessException(404, "目标商家门店不存在"));

        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(400, "该门店未营业或暂停接单");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        for (OrderItemRequest itemReq : req.getItems()) {
            Dish dish = dishRepository.findByIdAndIsDeletedFalse(itemReq.getDishId())
                    .orElseThrow(() -> new BusinessException(404, "菜品不存在或已下架: ID=" + itemReq.getDishId()));

            if (!dish.getMerchantId().equals(req.getMerchantId())) {
                throw new BusinessException(400, "菜品不属于当前下单门店: " + dish.getName());
            }

            if (dish.getStatus() == null || dish.getStatus() != 1) {
                throw new BusinessException(400, "菜品已售罄或下架: " + dish.getName());
            }

            double itemPrice = (dish.getMemberPrice() != null && dish.getMemberPrice() > 0)
                    ? dish.getMemberPrice()
                    : dish.getPrice();

            double lineTotal = BigDecimal.valueOf(itemPrice)
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            totalAmount += lineTotal;

            orderItems.add(OrderItem.builder()
                    .dishId(dish.getId())
                    .name(dish.getName())
                    .price(itemPrice)
                    .quantity(itemReq.getQuantity())
                    .build());
        }

        // 生成唯一订单号 ORD + yyyyMMddHHmmss + 4位随机数
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        String orderNo = "ORD" + timestamp + randomSuffix;

        // 生成 6 位随机核销码（仅限自提 1 或堂食 2）
        String pickupCode = null;
        if (req.getFulfillType() == 1 || req.getFulfillType() == 2) {
            pickupCode = String.format("%06d", new Random().nextInt(1000000));
        }

        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .merchantId(req.getMerchantId())
                .fulfillType(req.getFulfillType())
                .items(orderItems)
                .totalAmount(BigDecimal.valueOf(totalAmount).setScale(2, RoundingMode.HALF_UP).doubleValue())
                .status(0) // 0-待支付
                .address(req.getAddress())
                .pickupCode(pickupCode)
                .build();

        Order saved = orderRepository.save(order);

        // 如果是外卖订单，初始化配送单
        if (req.getFulfillType() == 0) {
            DeliveryRecord delivery = DeliveryRecord.builder()
                    .orderId(saved.getId())
                    .deliveryStatus(0) // 0-待接单
                    .address(req.getAddress())
                    .build();
            deliveryRecordRepository.save(delivery);
        }

        systemService.saveLog("ORDER", "CREATE", userId, "创建订单: orderNo=" + orderNo + ", total=" + totalAmount, "CLIENT");

        return toOrderResponse(saved, merchant.getName());
    }

    @Override
    @Transactional
    public OrderPayResponse payOrder(String userId, String orderId) {
        log.info("[OrderService] 模拟订单支付: userId={}, orderId={}", userId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权支付他人名下的订单");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException(400, "订单状态不支持支付（当前状态：" + getStatusDesc(order.getStatus()) + "）");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(1); // 1-已支付
        order.setPaymentTime(now);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        // 外卖订单自动流转为待骑手接单/配送中
        if (order.getFulfillType() == 0) {
            deliveryRecordRepository.findByOrderId(orderId).ifPresent(d -> {
                d.setDeliveryStatus(1); // 1-配送中
                d.setUpdatedAt(now);
                deliveryRecordRepository.save(d);
            });
        }

        // 记录系统运维日志与原子计数
        systemService.saveLog("ORDER", "PAY", userId, "订单模拟支付成功: orderNo=" + order.getOrderNo() + ", amount=" + order.getTotalAmount(), "CLIENT");
        systemService.incrementCounter();

        return OrderPayResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paymentTime(now)
                .pickupCode(order.getPickupCode())
                .build();
    }

    @Override
    @Transactional
    public VerifyResponse verifyOrder(String merchantUserId, VerifyRequest req) {
        log.info("[OrderService] 商家执行订单核销: merchantUserId={}, pickupCode={}", merchantUserId, req.getPickupCode());

        Merchant merchant = merchantRepository.findByUserId(merchantUserId)
                .orElseThrow(() -> new BusinessException(403, "当前登录账号未绑定任何商家门店"));

        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(403, "当前门店资质未通过审核或已暂停营业");
        }

        // 查找属于当前门店的核销码订单
        Order order = orderRepository.findByPickupCodeAndMerchantId(req.getPickupCode(), merchant.getId())
                .orElse(null);

        if (order == null) {
            systemService.saveLog("ORDER", "VERIFY", merchantUserId, "核销失败：核销码无效或不属于当前门店 [code=" + req.getPickupCode() + "]", "MERCHANT");
            throw new BusinessException(400, "核销码无效或不属于当前门店");
        }

        // 幂等防重检查：检查订单状态与唯一核销记录
        if (order.getStatus() == 3 || verifyRecordRepository.existsByOrderId(order.getId())) {
            systemService.saveLog("ORDER", "VERIFY", merchantUserId, "核销失败：订单已核销，拦截重复操作 [orderNo=" + order.getOrderNo() + "]", "MERCHANT");
            throw new BusinessException(400, "该订单已核销，请勿重复核销");
        }

        if (order.getStatus() != 1) {
            throw new BusinessException(400, "订单未支付或已取消，无法核销（当前状态：" + getStatusDesc(order.getStatus()) + "）");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 写入核销记录集合 verify_records
        VerifyRecord verifyRecord = VerifyRecord.builder()
                .orderId(order.getId())
                .merchantId(merchant.getId())
                .operator(merchant.getName())
                .verifyType(req.getVerifyType() != null ? req.getVerifyType() : 1)
                .verifiedAt(now)
                .build();
        verifyRecordRepository.save(verifyRecord);

        // 2. 更新订单状态为 3-已核销
        order.setStatus(3);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        // 3. 记录全链路日志
        systemService.saveLog("ORDER", "VERIFY", merchantUserId, "订单核销成功: orderNo=" + order.getOrderNo() + ", pickupCode=" + req.getPickupCode(), "MERCHANT");

        return VerifyResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .merchantId(merchant.getId())
                .operator(merchant.getName())
                .verifyType(verifyRecord.getVerifyType())
                .verifiedAt(now)
                .message("订单核销成功！")
                .build();
    }

    @Override
    public OrderResponse getOrderDetail(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        Merchant merchant = merchantRepository.findById(order.getMerchantId()).orElse(null);
        String merchantName = merchant != null ? merchant.getName() : "未知门店";

        return toOrderResponse(order, merchantName);
    }

    @Override
    public Page<OrderResponse> listUserOrders(String userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        return orderPage.map(o -> {
            Merchant m = merchantRepository.findById(o.getMerchantId()).orElse(null);
            return toOrderResponse(o, m != null ? m.getName() : "");
        });
    }

    @Override
    public Page<OrderResponse> listMerchantOrders(String merchantUserId, int page, int size) {
        Merchant merchant = merchantRepository.findByUserId(merchantUserId)
                .orElseThrow(() -> new BusinessException(403, "当前账号未绑定有效门店"));

        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchant.getId(), pageRequest);

        return orderPage.map(o -> toOrderResponse(o, merchant.getName()));
    }

    @Override
    public MerchantDashboardResponse getMerchantDashboard(String merchantUserId) {
        Merchant merchant = merchantRepository.findByUserId(merchantUserId)
                .orElseThrow(() -> new BusinessException(403, "当前账号未绑定有效门店"));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Order> todayOrders = orderRepository.findByMerchantIdAndCreatedAtBetween(merchant.getId(), startOfDay, endOfDay);
        List<VerifyRecord> todayVerifies = verifyRecordRepository.findByMerchantIdAndVerifiedAtBetween(merchant.getId(), startOfDay, endOfDay);

        long todayOrderCount = todayOrders.size();
        double todayRevenue = todayOrders.stream()
                .filter(o -> o.getStatus() >= 1 && o.getStatus() <= 4)
                .mapToDouble(Order::getTotalAmount)
                .sum();
        long todayVerifiedCount = todayVerifies.size();

        double verifyRate = todayOrderCount > 0
                ? BigDecimal.valueOf((double) todayVerifiedCount / todayOrderCount)
                .setScale(4, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        long totalOrderCount = orderRepository.countByMerchantId(merchant.getId());

        return MerchantDashboardResponse.builder()
                .merchantId(merchant.getId())
                .merchantName(merchant.getName())
                .todayOrderCount(todayOrderCount)
                .todayRevenue(BigDecimal.valueOf(todayRevenue).setScale(2, RoundingMode.HALF_UP).doubleValue())
                .todayVerifiedCount(todayVerifiedCount)
                .todayVerifyRate(verifyRate)
                .totalOrderCount(totalOrderCount)
                .build();
    }

    private OrderResponse toOrderResponse(Order o, String merchantName) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderNo(o.getOrderNo())
                .userId(o.getUserId())
                .merchantId(o.getMerchantId())
                .merchantName(merchantName)
                .fulfillType(o.getFulfillType())
                .items(o.getItems())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .statusDesc(getStatusDesc(o.getStatus()))
                .address(o.getAddress())
                .pickupCode(o.getPickupCode())
                .paymentTime(o.getPaymentTime())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "未知状态";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "配送中";
            case 3 -> "已核销";
            case 4 -> "已完成";
            case 5 -> "已取消";
            default -> "未知状态(" + status + ")";
        };
    }
}
