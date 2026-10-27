package com.distributed.base.service.impl;

import com.distributed.base.dto.DishCreateRequest;
import com.distributed.base.dto.DishResponse;
import com.distributed.base.dto.DishUpdateRequest;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.Merchant;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;

/**
 * 菜品供给管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishRepository dishRepository;
    private final MerchantRepository merchantRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final String CACHE_DISH_PREFIX = "cache:dish:";
    public static final String CACHE_MERCHANT_PREFIX = "cache:merchant:";

    @Override
    @Transactional
    public DishResponse createDish(String userId, DishCreateRequest req) {
        log.info("[DishService] 创建菜品: merchantId={}, name={}, calorie={}", req.getMerchantId(), req.getName(), req.getCalorie());

        Merchant merchant = merchantRepository.findById(req.getMerchantId())
                .orElseThrow(() -> new BusinessException(404, "所属商家不存在"));

        if (!merchant.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权向非本人名下的商家添加菜品");
        }

        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException(400, "门店未通过平台审核或处于歇业状态，暂不能添加菜品");
        }

        if (req.getCalorie() == null || req.getCalorie() <= 0) {
            throw new BusinessException(400, "菜品标称热量必须为大于0的有效数值");
        }

        Dish dish = Dish.builder()
                .merchantId(req.getMerchantId())
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .memberPrice(req.getMemberPrice())
                .coverImage(req.getCoverImage())
                .calorie(req.getCalorie())
                .flavorTags(req.getFlavorTags() != null ? req.getFlavorTags() : new ArrayList<>())
                .ingredientTags(req.getIngredientTags() != null ? req.getIngredientTags() : new ArrayList<>())
                .status(1) // 1-正常上架
                .isDeleted(false)
                .build();

        Dish saved = dishRepository.save(dish);

        // 联动驱逐商家门店详情缓存
        evictMerchantCache(saved.getMerchantId());

        return toDishResponse(saved);
    }

    @Override
    public DishResponse updateDish(String dishId, String userId, DishUpdateRequest req) {
        Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId)
                .orElseThrow(() -> new BusinessException(404, "菜品不存在或已被删除"));

        Merchant merchant = merchantRepository.findById(dish.getMerchantId())
                .orElseThrow(() -> new BusinessException(404, "所属商家不存在"));

        if (!merchant.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改其他商家的菜品信息");
        }

        if (StringUtils.hasText(req.getName())) {
            dish.setName(req.getName());
        }
        if (StringUtils.hasText(req.getDescription())) {
            dish.setDescription(req.getDescription());
        }
        if (req.getPrice() != null) {
            dish.setPrice(req.getPrice());
        }
        if (req.getMemberPrice() != null) {
            dish.setMemberPrice(req.getMemberPrice());
        }
        if (StringUtils.hasText(req.getCoverImage())) {
            dish.setCoverImage(req.getCoverImage());
        }
        if (req.getCalorie() != null) {
            dish.setCalorie(req.getCalorie());
        }
        if (req.getFlavorTags() != null) {
            dish.setFlavorTags(req.getFlavorTags());
        }
        if (req.getIngredientTags() != null) {
            dish.setIngredientTags(req.getIngredientTags());
        }

        Dish updated = dishRepository.save(dish);

        // 联动驱逐 Redis 菜品详情与门店缓存
        evictDishCache(dishId);
        evictMerchantCache(dish.getMerchantId());

        return toDishResponse(updated);
    }

    @Override
    public void toggleDishStatus(String dishId, String userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值非法（0-下架，1-上架）");
        }

        Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId)
                .orElseThrow(() -> new BusinessException(404, "菜品不存在"));

        Merchant merchant = merchantRepository.findById(dish.getMerchantId())
                .orElseThrow(() -> new BusinessException(404, "所属商家不存在"));

        if (!merchant.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作其他商家的菜品");
        }

        dish.setStatus(status);
        dishRepository.save(dish);

        evictDishCache(dishId);
        evictMerchantCache(dish.getMerchantId());
    }

    @Override
    public void deleteDish(String dishId, String userId) {
        Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId)
                .orElseThrow(() -> new BusinessException(404, "菜品不存在"));

        Merchant merchant = merchantRepository.findById(dish.getMerchantId())
                .orElseThrow(() -> new BusinessException(404, "所属商家不存在"));

        if (!merchant.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除其他商家的菜品");
        }

        dish.setIsDeleted(true);
        dishRepository.save(dish);

        evictDishCache(dishId);
        evictMerchantCache(dish.getMerchantId());
    }

    @Override
    public DishResponse getDishDetail(String dishId) {
        String cacheKey = CACHE_DISH_PREFIX + dishId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DishResponse resp) {
                return resp;
            }
        } catch (Exception e) {
            log.warn("[DishService] 读取菜品缓存失败: {}", e.getMessage());
        }

        Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId)
                .orElseThrow(() -> new BusinessException(404, "菜品不存在或已下架"));

        DishResponse response = toDishResponse(dish);

        try {
            redisTemplate.opsForValue().set(cacheKey, response, Duration.ofSeconds(300));
        } catch (Exception e) {
            log.warn("[DishService] 写入菜品缓存失败: {}", e.getMessage());
        }

        return response;
    }

    @Override
    public Page<DishResponse> listDishesByMerchant(String merchantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return dishRepository.findByMerchantIdAndIsDeletedFalse(merchantId, pageRequest).map(this::toDishResponse);
    }

    private void evictDishCache(String dishId) {
        try {
            redisTemplate.delete(CACHE_DISH_PREFIX + dishId);
        } catch (Exception e) {
            log.warn("[DishService] 驱逐菜品缓存失败: {}", e.getMessage());
        }
    }

    private void evictMerchantCache(String merchantId) {
        try {
            redisTemplate.delete(CACHE_MERCHANT_PREFIX + merchantId);
        } catch (Exception e) {
            log.warn("[DishService] 驱逐门店缓存失败: {}", e.getMessage());
        }
    }

    private DishResponse toDishResponse(Dish d) {
        return DishResponse.builder()
                .id(d.getId())
                .merchantId(d.getMerchantId())
                .name(d.getName())
                .description(d.getDescription())
                .price(d.getPrice())
                .memberPrice(d.getMemberPrice())
                .coverImage(d.getCoverImage())
                .calorie(d.getCalorie())
                .flavorTags(d.getFlavorTags())
                .ingredientTags(d.getIngredientTags())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
