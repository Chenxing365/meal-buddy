package com.distributed.base.service;

import com.distributed.base.dto.MerchantApplyRequest;
import com.distributed.base.dto.MerchantDetailResponse;
import com.distributed.base.dto.MerchantResponse;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.Merchant;
import com.distributed.base.entity.User;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.repository.UserRepository;
import com.distributed.base.service.impl.MerchantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M2阶段-MerchantService商家门店与审核流单元测试")
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private MerchantServiceImpl merchantService;

    @BeforeEach
    void setUp() {
        merchantService = new MerchantServiceImpl(
                merchantRepository,
                dishRepository,
                userRepository,
                redisTemplate
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("测试商家入驻申请：初始状态为0-待审核")
    void testApplyMerchant() {
        String userId = "u_merchant_1";
        MerchantApplyRequest req = MerchantApplyRequest.builder()
                .name("绿野轻食轻卡自选店")
                .address("高新区天府大道北段120号")
                .longitude(104.066541)
                .latitude(30.572269)
                .category("轻食沙拉")
                .build();

        when(merchantRepository.existsByUserId(userId)).thenReturn(false);
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(i -> {
            Merchant m = i.getArgument(0);
            m.setId("m_test_1");
            return m;
        });

        MerchantResponse resp = merchantService.apply(userId, req);

        assertNotNull(resp);
        assertEquals("m_test_1", resp.getId());
        assertEquals(0, resp.getStatus()); // 0-待审核
        assertEquals("绿野轻食轻卡自选店", resp.getName());

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("测试商家审核流：通过后状态置为1-正常营业，关联用户角色升为2")
    void testAdminApproveMerchant() {
        String merchantId = "m_test_1";
        String userId = "u_test_1";
        Merchant m = Merchant.builder()
                .id(merchantId)
                .userId(userId)
                .name("川味轻食馆")
                .status(0)
                .build();
        User u = User.builder()
                .id(userId)
                .role(0)
                .build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(m));
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));

        merchantService.adminApproveMerchant(merchantId);

        assertEquals(1, m.getStatus());
        assertEquals(2, u.getRole()); // 升级为商家
        verify(merchantRepository).save(m);
        verify(userRepository).save(u);
        verify(redisTemplate).delete("cache:merchant:" + merchantId);
    }

    @Test
    @DisplayName("测试门店详情查询：包含正常营业菜品并设置Redis缓存300s")
    void testGetMerchantDetail() {
        String merchantId = "m_test_1";
        Merchant m = Merchant.builder()
                .id(merchantId)
                .userId("u_1")
                .name("低卡美味馆")
                .status(1)
                .location(new GeoJsonPoint(104.0, 30.0))
                .build();

        Dish d1 = Dish.builder()
                .id("d_1")
                .merchantId(merchantId)
                .name("经典鸡胸肉沙拉")
                .calorie(320)
                .price(28.0)
                .status(1)
                .isDeleted(false)
                .build();

        when(valueOperations.get("cache:merchant:" + merchantId)).thenReturn(null);
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(m));
        when(dishRepository.findByMerchantIdAndStatusAndIsDeletedFalse(merchantId, 1)).thenReturn(List.of(d1));

        MerchantDetailResponse detail = merchantService.getMerchantDetail(merchantId);

        assertNotNull(detail);
        assertEquals("低卡美味馆", detail.getMerchant().getName());
        assertEquals(1, detail.getDishes().size());
        assertEquals(320, detail.getDishes().get(0).getCalorie());

        verify(valueOperations).set(eq("cache:merchant:" + merchantId), any(MerchantDetailResponse.class), any());
    }
}
