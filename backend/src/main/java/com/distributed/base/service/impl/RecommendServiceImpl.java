package com.distributed.base.service.impl;

import com.distributed.base.dto.*;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.RecommendLog;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.RecommendLogRepository;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.service.MealBuddyAgentService;
import com.distributed.base.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 推荐业务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final MealBuddyAgentService mealBuddyAgentService;
    private final RecommendLogRepository recommendLogRepository;
    private final DishRepository dishRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final String KEY_RECOMMEND_MODE_PREFIX = "recommend:mode:";
    public static final String KEY_AI_CHAT_HISTORY_PREFIX = "ai:chat:history:";

    @Override
    public void switchMode(String userId, String mode) {
        if (!"slim".equals(mode) && !"normal".equals(mode) && !"indulge".equals(mode)) {
            throw new BusinessException(400, "模式参数非法，仅支持 slim / normal / indulge");
        }

        log.info("[RecommendService] 用户切换推荐模式: userId={}, mode={}", userId, mode);

        // 1. 同步写入 Redis 缓存
        stringRedisTemplate.opsForValue().set(KEY_RECOMMEND_MODE_PREFIX + userId, mode);

        // 2. 异步/同步持久化至用户偏好表
        userPreferenceRepository.findByUserId(userId).ifPresent(p -> {
            p.setRecommendMode(mode);
            userPreferenceRepository.save(p);
        });
    }

    @Override
    public String getCurrentMode(String userId) {
        String cachedMode = stringRedisTemplate.opsForValue().get(KEY_RECOMMEND_MODE_PREFIX + userId);
        if (StringUtils.hasText(cachedMode)) {
            return cachedMode;
        }

        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElse(null);
        String mode = preference != null && StringUtils.hasText(preference.getRecommendMode())
                ? preference.getRecommendMode()
                : "normal";

        stringRedisTemplate.opsForValue().set(KEY_RECOMMEND_MODE_PREFIX + userId, mode);
        return mode;
    }

    @Override
    @Transactional
    public RecommendResponse getRecommendations(String userId) {
        String mode = getCurrentMode(userId);
        log.info("[RecommendService] 执行推荐计算: userId={}, mode={}", userId, mode);

        // 1. 调用底层 Agent 引擎（Spring AI / LangChain4j）
        RecommendResult result = mealBuddyAgentService.generateRecommendation(userId, mode);

        // 2. 决策日志持久化至 recommend_logs
        RecommendLog logEntity = RecommendLog.builder()
                .userId(userId)
                .mode(mode)
                .dishIds(result.getDishIds())
                .matchReason(result.getMatchReason())
                .ruleSnapshot(result.getRuleSnapshot())
                .feedback(0)
                .build();

        RecommendLog saved = recommendLogRepository.save(logEntity);

        return RecommendResponse.builder()
                .logId(saved.getId())
                .mode(mode)
                .matchReason(result.getMatchReason())
                .dishes(result.getDishes())
                .ruleSnapshot(result.getRuleSnapshot())
                .feedback(0)
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    @Override
    public void submitFeedback(String userId, RecommendFeedbackRequest req) {
        RecommendLog logEntity = recommendLogRepository.findById(req.getLogId())
                .orElseThrow(() -> new BusinessException(404, "推荐日志记录不存在"));

        if (!logEntity.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权对此推荐记录提交反馈");
        }

        logEntity.setFeedback(req.getFeedback());
        recommendLogRepository.save(logEntity);
        log.info("[RecommendService] 反馈已更新: logId={}, feedback={}", req.getLogId(), req.getFeedback());
    }

    @Override
    public Page<RecommendResponse> getHistory(String userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RecommendLog> logPage = recommendLogRepository.findByUserId(userId, pageRequest);

        return logPage.map(l -> {
            List<Dish> dishes = (l.getDishIds() != null && !l.getDishIds().isEmpty())
                    ? dishRepository.findByIdInAndIsDeletedFalse(l.getDishIds())
                    : Collections.emptyList();
            List<DishResponse> dishResponses = dishes.stream().map(this::toDishResponse).collect(Collectors.toList());

            return RecommendResponse.builder()
                    .logId(l.getId())
                    .mode(l.getMode())
                    .matchReason(l.getMatchReason())
                    .dishes(dishResponses)
                    .ruleSnapshot(l.getRuleSnapshot())
                    .feedback(l.getFeedback())
                    .createdAt(l.getCreatedAt())
                    .build();
        });
    }

    @Override
    public AiChatResponse chatWithAgent(String userId, String message) {
        log.info("[RecommendService] 用户与 AI 助手会话: userId={}", userId);

        String reply = mealBuddyAgentService.chat(userId, message);
        LocalDateTime now = LocalDateTime.now();

        String chatKey = KEY_AI_CHAT_HISTORY_PREFIX + userId;
        try {
            AiChatMessage userMsg = AiChatMessage.builder()
                    .role("user")
                    .content(message)
                    .timestamp(now)
                    .build();
            AiChatMessage assistantMsg = AiChatMessage.builder()
                    .role("assistant")
                    .content(reply)
                    .timestamp(now)
                    .build();

            redisTemplate.opsForList().rightPush(chatKey, userMsg);
            redisTemplate.opsForList().rightPush(chatKey, assistantMsg);

            // 保留最新 10 轮对话（20 条）
            redisTemplate.opsForList().trim(chatKey, -20, -1);
            redisTemplate.expire(chatKey, Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("[RecommendService] 保存会话历史至 Redis 失败: {}", e.getMessage());
        }

        return AiChatResponse.builder()
                .reply(reply)
                .timestamp(now)
                .build();
    }

    @Override
    public List<AiChatMessage> getChatHistory(String userId) {
        String chatKey = KEY_AI_CHAT_HISTORY_PREFIX + userId;
        try {
            List<Object> rawList = redisTemplate.opsForList().range(chatKey, 0, -1);
            if (rawList != null) {
                return rawList.stream()
                        .filter(o -> o instanceof AiChatMessage)
                        .map(o -> (AiChatMessage) o)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("[RecommendService] 读取会话历史失败: {}", e.getMessage());
        }
        return Collections.emptyList();
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
