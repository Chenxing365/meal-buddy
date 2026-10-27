package com.distributed.base.service.impl;

import com.distributed.base.dto.MerchantApplyRequest;
import com.distributed.base.dto.MerchantDetailResponse;
import com.distributed.base.dto.MerchantResponse;
import com.distributed.base.dto.MerchantUpdateRequest;
import com.distributed.base.dto.DishResponse;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.Merchant;
import com.distributed.base.entity.User;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.repository.UserRepository;
import com.distributed.base.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商家门店业务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final DishRepository dishRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final String CACHE_MERCHANT_PREFIX = "cache:merchant:";

    @Override
    @Transactional
    public MerchantResponse apply(String userId, MerchantApplyRequest req) {
        log.info("[MerchantService] 提交入驻申请: userId={}, name={}", userId, req.getName());

        if (merchantRepository.existsByUserId(userId)) {
            throw new BusinessException(400, "您已提交过门店入驻申请，请勿重复提交");
        }

        GeoJsonPoint point = new GeoJsonPoint(req.getLongitude(), req.getLatitude());

        Merchant merchant = Merchant.builder()
                .userId(userId)
                .name(req.getName())
                .logo(req.getLogo())
                .address(req.getAddress())
                .location(point)
                .category(req.getCategory())
                .commissionRate(0.05)
                .status(0) // 0-待审核
                .build();

        Merchant saved = merchantRepository.save(merchant);
        log.info("[MerchantService] 门店入驻申请已录入，等待审核: merchantId={}", saved.getId());
        return toMerchantResponse(saved);
    }

    @Override
    public MerchantDetailResponse getMerchantDetail(String merchantId) {
        String cacheKey = CACHE_MERCHANT_PREFIX + merchantId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof MerchantDetailResponse detail) {
                return detail;
            }
        } catch (Exception e) {
            log.warn("[MerchantService] 读取门店详情缓存失败: {}", e.getMessage());
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(404, "目标商家门店不存在"));

        List<Dish> dishList = dishRepository.findByMerchantIdAndStatusAndIsDeletedFalse(merchantId, 1);
        List<DishResponse> dishResponses = dishList.stream().map(this::toDishResponse).collect(Collectors.toList());

        MerchantDetailResponse detail = MerchantDetailResponse.builder()
                .merchant(toMerchantResponse(merchant))
                .dishes(dishResponses)
                .build();

        try {
            redisTemplate.opsForValue().set(cacheKey, detail, Duration.ofSeconds(300));
        } catch (Exception e) {
            log.warn("[MerchantService] 写入门店详情缓存失败: {}", e.getMessage());
        }

        return detail;
    }

    @Override
    public Page<MerchantResponse> listMerchants(String category, Double lng, Double lat, Double distanceKm, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Merchant> merchantPage;

        if (lng != null && lat != null && distanceKm != null) {
            Point point = new Point(lng, lat);
            Distance distance = new Distance(distanceKm, Metrics.KILOMETERS);
            merchantPage = merchantRepository.findByLocationNearAndStatus(point, distance, 1, pageRequest);
        } else if (StringUtils.hasText(category)) {
            merchantPage = merchantRepository.findByCategoryAndStatus(category, 1, pageRequest);
        } else {
            merchantPage = merchantRepository.findByStatus(1, pageRequest);
        }

        return merchantPage.map(this::toMerchantResponse);
    }

    @Override
    public MerchantResponse updateMerchantInfo(String merchantId, String userId, MerchantUpdateRequest req) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(404, "目标商家不存在"));

        if (!merchant.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改其他商家的门店信息");
        }

        if (StringUtils.hasText(req.getName())) {
            merchant.setName(req.getName());
        }
        if (StringUtils.hasText(req.getLogo())) {
            merchant.setLogo(req.getLogo());
        }
        if (StringUtils.hasText(req.getAddress())) {
            merchant.setAddress(req.getAddress());
        }
        if (req.getLongitude() != null && req.getLatitude() != null) {
            merchant.setLocation(new GeoJsonPoint(req.getLongitude(), req.getLatitude()));
        }
        if (StringUtils.hasText(req.getCategory())) {
            merchant.setCategory(req.getCategory());
        }
        if (req.getStatus() != null && (req.getStatus() == 1 || req.getStatus() == 2)) {
            merchant.setStatus(req.getStatus());
        }

        Merchant updated = merchantRepository.save(merchant);
        evictMerchantCache(merchantId);
        return toMerchantResponse(updated);
    }

    @Override
    public Page<MerchantResponse> adminListMerchants(Integer status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Merchant> pageResult = status != null
                ? merchantRepository.findByStatus(status, pageRequest)
                : merchantRepository.findAll(pageRequest);
        return pageResult.map(this::toMerchantResponse);
    }

    @Override
    @Transactional
    public void adminApproveMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(404, "目标商家不存在"));

        merchant.setStatus(1); // 1-正常营业
        merchant.setRejectReason(null);
        merchantRepository.save(merchant);

        // 升级关联用户角色为 2-商家
        User user = userRepository.findById(merchant.getUserId()).orElse(null);
        if (user != null) {
            user.setRole(2);
            userRepository.save(user);
        }

        evictMerchantCache(merchantId);
        log.info("[MerchantService] 商家资质审核通过: merchantId={}", merchantId);
    }

    @Override
    public void adminRejectMerchant(String merchantId, String reason) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(404, "目标商家不存在"));

        merchant.setStatus(3); // 3-审核驳回
        merchant.setRejectReason(StringUtils.hasText(reason) ? reason : "资质审核未达标");
        merchantRepository.save(merchant);

        evictMerchantCache(merchantId);
        log.info("[MerchantService] 商家资质审核已驳回: merchantId={}, reason={}", merchantId, reason);
    }

    public void evictMerchantCache(String merchantId) {
        try {
            redisTemplate.delete(CACHE_MERCHANT_PREFIX + merchantId);
        } catch (Exception e) {
            log.warn("[MerchantService] 驱逐门店缓存失败: {}", e.getMessage());
        }
    }

    private MerchantResponse toMerchantResponse(Merchant m) {
        Double lng = m.getLocation() != null ? m.getLocation().getX() : null;
        Double lat = m.getLocation() != null ? m.getLocation().getY() : null;
        return MerchantResponse.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .name(m.getName())
                .logo(m.getLogo())
                .address(m.getAddress())
                .longitude(lng)
                .latitude(lat)
                .category(m.getCategory())
                .commissionRate(m.getCommissionRate())
                .status(m.getStatus())
                .rejectReason(m.getRejectReason())
                .createdAt(m.getCreatedAt())
                .build();
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
