package com.distributed.base.service;

import com.distributed.base.dto.DishCreateRequest;
import com.distributed.base.dto.DishResponse;
import com.distributed.base.dto.DishUpdateRequest;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.Merchant;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.service.impl.DishServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M2阶段-DishService菜品供给与热量维护单元测试")
class DishServiceTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private DishServiceImpl dishService;

    @BeforeEach
    void setUp() {
        dishService = new DishServiceImpl(
                dishRepository,
                merchantRepository,
                redisTemplate
        );
    }

    @Test
    @DisplayName("测试录入菜品：未通过审核门店（status=0）禁止添加菜品")
    void testCreateDishUnapprovedMerchant() {
        String userId = "u_merchant_1";
        String merchantId = "m_1";

        Merchant pendingMerchant = Merchant.builder()
                .id(merchantId)
                .userId(userId)
                .status(0) // 0-待审核
                .build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(pendingMerchant));

        DishCreateRequest req = DishCreateRequest.builder()
                .merchantId(merchantId)
                .name("减脂牛油果沙拉")
                .price(32.0)
                .calorie(420)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> dishService.createDish(userId, req));
        assertTrue(ex.getMessage().contains("未通过平台审核"));
    }

    @Test
    @DisplayName("测试录入菜品：正常营业门店成功录入并联动驱逐门店缓存")
    void testCreateDishSuccess() {
        String userId = "u_merchant_1";
        String merchantId = "m_1";

        Merchant approvedMerchant = Merchant.builder()
                .id(merchantId)
                .userId(userId)
                .status(1) // 1-正常营业
                .build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(approvedMerchant));
        when(dishRepository.save(any(Dish.class))).thenAnswer(i -> {
            Dish d = i.getArgument(0);
            d.setId("d_test_1");
            return d;
        });

        DishCreateRequest req = DishCreateRequest.builder()
                .merchantId(merchantId)
                .name("低卡黑椒牛肉粒")
                .price(38.0)
                .calorie(450)
                .flavorTags(List.of("黑椒", "少油"))
                .build();

        DishResponse resp = dishService.createDish(userId, req);

        assertNotNull(resp);
        assertEquals("d_test_1", resp.getId());
        assertEquals(450, resp.getCalorie());
        assertEquals(List.of("黑椒", "少油"), resp.getFlavorTags());

        // 联动驱逐门店缓存
        verify(redisTemplate).delete("cache:merchant:" + merchantId);
    }

    @Test
    @DisplayName("测试菜品修改：联动驱逐菜品缓存与门店详情缓存")
    void testUpdateDishAndEvictCache() {
        String userId = "u_merchant_1";
        String merchantId = "m_1";
        String dishId = "d_1";

        Dish existingDish = Dish.builder()
                .id(dishId)
                .merchantId(merchantId)
                .name("原味沙拉")
                .price(20.0)
                .calorie(300)
                .isDeleted(false)
                .build();

        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .userId(userId)
                .build();

        when(dishRepository.findByIdAndIsDeletedFalse(dishId)).thenReturn(Optional.of(existingDish));
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(dishRepository.save(any(Dish.class))).thenAnswer(i -> i.getArgument(0));

        DishUpdateRequest updateReq = DishUpdateRequest.builder()
                .name("升级版黑醋沙拉")
                .price(25.0)
                .calorie(280)
                .build();

        DishResponse resp = dishService.updateDish(dishId, userId, updateReq);

        assertEquals("升级版黑醋沙拉", resp.getName());
        assertEquals(280, resp.getCalorie());
        assertEquals(25.0, resp.getPrice());

        verify(redisTemplate).delete("cache:dish:" + dishId);
        verify(redisTemplate).delete("cache:merchant:" + merchantId);
    }
}
