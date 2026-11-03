package com.distributed.base.service;

import com.distributed.base.dto.*;
import com.distributed.base.entity.*;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.*;
import com.distributed.base.service.impl.OrderServiceImpl;
import com.distributed.base.webservice.impl.SystemIntegrationWsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M4阶段-OrderService交易履约、核销防重与运维集成单元测试")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VerifyRecordRepository verifyRecordRepository;

    @Mock
    private DeliveryRecordRepository deliveryRecordRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private SystemService systemService;

    private OrderServiceImpl orderService;
    private SystemIntegrationWsImpl systemIntegrationWs;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                verifyRecordRepository,
                deliveryRecordRepository,
                merchantRepository,
                dishRepository,
                systemService
        );
        systemIntegrationWs = new SystemIntegrationWsImpl(systemService);
    }

    @Test
    @DisplayName("测试创建自提订单：成功生成6位自提核销码并初始为待支付(status=0)")
    void testCreatePickupOrderAndGeneratePickupCode() {
        String userId = "u_buyer_1";
        String merchantId = "m_shop_1";
        String dishId = "d_salad_1";

        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .name("轻食动力自选轻卡")
                .status(1) // 正常营业
                .build();

        Dish dish = Dish.builder()
                .id(dishId)
                .merchantId(merchantId)
                .name("经典水煮鸡胸沙拉")
                .price(28.0)
                .status(1)
                .isDeleted(false)
                .build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(dishRepository.findByIdAndIsDeletedFalse(dishId)).thenReturn(Optional.of(dish));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId("ord_test_id_1");
            return o;
        });

        OrderCreateRequest req = OrderCreateRequest.builder()
                .merchantId(merchantId)
                .fulfillType(1) // 1-到店自提
                .items(List.of(OrderItemRequest.builder().dishId(dishId).quantity(2).build()))
                .build();

        OrderResponse resp = orderService.createOrder(userId, req);

        assertNotNull(resp);
        assertEquals("ord_test_id_1", resp.getId());
        assertEquals(0, resp.getStatus()); // 待支付
        assertEquals(56.0, resp.getTotalAmount());
        assertNotNull(resp.getPickupCode());
        assertEquals(6, resp.getPickupCode().length()); // 6位核销码
        assertTrue(resp.getOrderNo().startsWith("ORD"));

        verify(systemService).saveLog(eq("ORDER"), eq("CREATE"), eq(userId), any(), eq("CLIENT"));
    }

    @Test
    @DisplayName("测试模拟订单支付：状态流转为已支付(status=1)，记录系统运维日志与自增计数器")
    void testPayOrderSuccess() {
        String userId = "u_buyer_1";
        String orderId = "ord_test_id_1";

        Order order = Order.builder()
                .id(orderId)
                .orderNo("ORD202611030001")
                .userId(userId)
                .status(0) // 待支付
                .totalAmount(56.0)
                .pickupCode("123456")
                .fulfillType(1)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderPayResponse payResp = orderService.payOrder(userId, orderId);

        assertNotNull(payResp);
        assertEquals(1, payResp.getStatus()); // 1-已支付
        assertNotNull(payResp.getPaymentTime());
        assertEquals("123456", payResp.getPickupCode());

        verify(systemService).saveLog(eq("ORDER"), eq("PAY"), eq(userId), any(), eq("CLIENT"));
        verify(systemService).incrementCounter();
    }

    @Test
    @DisplayName("阶段验收标准1：商家端输入错误核销码报 400 校验异常")
    void testVerifyOrderWithInvalidPickupCode() {
        String merchantUserId = "u_merchant_owner_1";
        String merchantId = "m_shop_1";

        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .userId(merchantUserId)
                .status(1)
                .build();

        when(merchantRepository.findByUserId(merchantUserId)).thenReturn(Optional.of(merchant));
        when(orderRepository.findByPickupCodeAndMerchantId("999999", merchantId)).thenReturn(Optional.empty());

        VerifyRequest req = VerifyRequest.builder()
                .pickupCode("999999")
                .verifyType(1)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.verifyOrder(merchantUserId, req));
        assertTrue(ex.getMessage().contains("核销码无效"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("阶段验收标准1与3：输入正确核销码成功核销(status=3)，写入verify_records且SOAP通道可查到审计日志")
    void testVerifyOrderSuccess() {
        String merchantUserId = "u_merchant_owner_1";
        String merchantId = "m_shop_1";
        String orderId = "ord_1001";
        String pickupCode = "654321";

        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .userId(merchantUserId)
                .name("极客能量轻食屋")
                .status(1)
                .build();

        Order order = Order.builder()
                .id(orderId)
                .orderNo("ORD202611038888")
                .merchantId(merchantId)
                .status(1) // 1-已支付
                .pickupCode(pickupCode)
                .build();

        when(merchantRepository.findByUserId(merchantUserId)).thenReturn(Optional.of(merchant));
        when(orderRepository.findByPickupCodeAndMerchantId(pickupCode, merchantId)).thenReturn(Optional.of(order));
        when(verifyRecordRepository.existsByOrderId(orderId)).thenReturn(false);

        VerifyRequest req = VerifyRequest.builder()
                .pickupCode(pickupCode)
                .verifyType(0) // 扫码核销
                .build();

        VerifyResponse resp = orderService.verifyOrder(merchantUserId, req);

        assertNotNull(resp);
        assertEquals(orderId, resp.getOrderId());
        assertEquals("ORD202611038888", resp.getOrderNo());
        assertEquals("极客能量轻食屋", resp.getOperator());
        assertEquals("订单核销成功！", resp.getMessage());

        // 验证写入核销记录集合
        ArgumentCaptor<VerifyRecord> recordCaptor = ArgumentCaptor.forClass(VerifyRecord.class);
        verify(verifyRecordRepository).save(recordCaptor.capture());
        assertEquals(orderId, recordCaptor.getValue().getOrderId());
        assertEquals(merchantId, recordCaptor.getValue().getMerchantId());

        // 验证订单状态流转为 3-已核销
        assertEquals(3, order.getStatus());
        verify(orderRepository).save(order);

        // 验证全链路系统运维日志记录
        verify(systemService).saveLog(eq("ORDER"), eq("VERIFY"), eq(merchantUserId), contains("订单核销成功"), eq("MERCHANT"));

        // 验收标准3：通过 SOAP WebService 运维通道查询最近日志
        SystemLog mockLog = SystemLog.builder()
                .module("ORDER")
                .action("VERIFY")
                .operator(merchantUserId)
                .content("订单核销成功: orderNo=ORD202611038888, pickupCode=654321")
                .build();
        when(systemService.findByModule("ORDER")).thenReturn(List.of(mockLog));

        String logsJson = systemIntegrationWs.queryRecentLogs("ORDER");
        assertNotNull(logsJson);
        assertTrue(logsJson.contains("ORD202611038888"));
        assertTrue(logsJson.contains("VERIFY"));
    }

    @Test
    @DisplayName("阶段验收标准2：重复输入核销码报“订单已核销”异常（幂等防重拦截）")
    void testVerifyOrderIdempotencyDuplicateRejection() {
        String merchantUserId = "u_merchant_owner_1";
        String merchantId = "m_shop_1";
        String orderId = "ord_1001";
        String pickupCode = "654321";

        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .userId(merchantUserId)
                .status(1)
                .build();

        Order verifiedOrder = Order.builder()
                .id(orderId)
                .orderNo("ORD202611038888")
                .merchantId(merchantId)
                .status(3) // 3-已核销
                .pickupCode(pickupCode)
                .build();

        when(merchantRepository.findByUserId(merchantUserId)).thenReturn(Optional.of(merchant));
        when(orderRepository.findByPickupCodeAndMerchantId(pickupCode, merchantId)).thenReturn(Optional.of(verifiedOrder));

        VerifyRequest req = VerifyRequest.builder()
                .pickupCode(pickupCode)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.verifyOrder(merchantUserId, req));
        assertTrue(ex.getMessage().contains("已核销，请勿重复核销"));
        assertEquals(400, ex.getCode());

        // 验证拦截未重复保存核销记录
        verify(verifyRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("测试商家经营大盘：正确聚合当日订单量、营业额、已核销数与核销率")
    void testMerchantDashboard() {
        String merchantUserId = "u_merchant_owner_1";
        String merchantId = "m_shop_1";

        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .userId(merchantUserId)
                .name("轻食测试店")
                .build();

        Order o1 = Order.builder().id("o1").merchantId(merchantId).totalAmount(30.0).status(1).build(); // 已支付
        Order o2 = Order.builder().id("o2").merchantId(merchantId).totalAmount(50.0).status(3).build(); // 已核销
        Order o3 = Order.builder().id("o3").merchantId(merchantId).totalAmount(20.0).status(0).build(); // 待支付(不计入营收)

        VerifyRecord v1 = VerifyRecord.builder().id("v1").orderId("o2").build();

        when(merchantRepository.findByUserId(merchantUserId)).thenReturn(Optional.of(merchant));
        when(orderRepository.findByMerchantIdAndCreatedAtBetween(eq(merchantId), any(), any()))
                .thenReturn(List.of(o1, o2, o3));
        when(verifyRecordRepository.findByMerchantIdAndVerifiedAtBetween(eq(merchantId), any(), any()))
                .thenReturn(List.of(v1));
        when(orderRepository.countByMerchantId(merchantId)).thenReturn(100L);

        MerchantDashboardResponse dashboard = orderService.getMerchantDashboard(merchantUserId);

        assertNotNull(dashboard);
        assertEquals(merchantId, dashboard.getMerchantId());
        assertEquals(3L, dashboard.getTodayOrderCount());
        assertEquals(80.0, dashboard.getTodayRevenue()); // 30 + 50
        assertEquals(1L, dashboard.getTodayVerifiedCount());
        assertEquals(0.3333, dashboard.getTodayVerifyRate()); // 1 / 3
        assertEquals(100L, dashboard.getTotalOrderCount());
    }
}
