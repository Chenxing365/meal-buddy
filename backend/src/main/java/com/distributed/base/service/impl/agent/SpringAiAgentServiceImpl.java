package com.distributed.base.service.impl.agent;

import com.distributed.base.dto.DishResponse;
import com.distributed.base.dto.RecommendResult;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.service.MealBuddyAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring AI 驱动的饭搭子智能体引擎实现
 *
 * <p>阶段一默认启用，融合热量规则引擎与可解释性 AI 推荐话术
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meal-buddy.agent.engine", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiAgentServiceImpl implements MealBuddyAgentService {

    private final DishRepository dishRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    public RecommendResult generateRecommendation(String userId, String mode) {
        log.info("[SpringAiAgent] 开始执行智能推荐决策: userId={}, mode={}", userId, mode);

        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElse(UserPreference.builder()
                        .userId(userId)
                        .recommendMode(mode)
                        .calorieLimit(800)
                        .flavorTags(Collections.emptyList())
                        .allergyTags(Collections.emptyList())
                        .build());

        List<Dish> allDishes = dishRepository.findByStatusAndIsDeletedFalse(1);
        List<String> allergyTags = preference.getAllergyTags() != null ? preference.getAllergyTags() : Collections.emptyList();

        List<Dish> candidateDishes = allDishes.stream()
                .filter(d -> !hasAllergyConflict(d, allergyTags))
                .collect(Collectors.toList());

        List<Dish> matchedDishes;
        String matchReason;
        Map<String, Object> ruleSnapshot = new HashMap<>();
        ruleSnapshot.put("engine", "spring-ai");
        ruleSnapshot.put("mode", mode);
        ruleSnapshot.put("allergyExcluded", allergyTags);

        if ("slim".equalsIgnoreCase(mode)) {
            int maxCalorie = preference.getCalorieLimit() != null && preference.getCalorieLimit() > 0
                    ? Math.min(preference.getCalorieLimit(), 500)
                    : 500;
            matchedDishes = candidateDishes.stream()
                    .filter(d -> d.getCalorie() != null && d.getCalorie() <= maxCalorie)
                    .sorted(Comparator.comparingInt(Dish::getCalorie))
                    .limit(5)
                    .collect(Collectors.toList());

            ruleSnapshot.put("maxCalorie", maxCalorie);
            ruleSnapshot.put("calorieFilterRule", "<=" + maxCalorie);

            matchReason = String.format("【AI减脂推荐】为您匹配低卡轻食，单餐热量严格控制在 %d 大卡以内，少油健康无负担", maxCalorie);
            if (!matchedDishes.isEmpty()) {
                int avgCalorie = (int) matchedDishes.stream().mapToInt(Dish::getCalorie).average().orElse(maxCalorie);
                matchReason += String.format("，精选 %d 道高纤低脂菜品，平均热量仅 %d 大卡。", matchedDishes.size(), avgCalorie);
            }
        } else if ("indulge".equalsIgnoreCase(mode)) {
            matchedDishes = candidateDishes.stream()
                    .sorted(Comparator.comparingInt((Dish d) -> d.getCalorie() != null ? d.getCalorie() : 0).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            ruleSnapshot.put("calorieRestricted", false);
            ruleSnapshot.put("sortStrategy", "high_calorie_and_rich_flavor");

            matchReason = "【AI放纵模式】放纵美味模式：不设热量限制，尽情享受浓郁醇厚、满足多巴胺的高品质佳肴！";
            if (!matchedDishes.isEmpty()) {
                matchReason += String.format("已为您优选 %d 款高满足度大满足菜品。", matchedDishes.size());
            }
        } else {
            // normal
            int maxCalorie = preference.getCalorieLimit() != null && preference.getCalorieLimit() > 0
                    ? preference.getCalorieLimit()
                    : 800;
            List<String> flavorTags = preference.getFlavorTags() != null ? preference.getFlavorTags() : Collections.emptyList();

            matchedDishes = candidateDishes.stream()
                    .filter(d -> d.getCalorie() != null && d.getCalorie() <= maxCalorie)
                    .sorted((d1, d2) -> {
                        int match1 = countMatchedTags(d1.getFlavorTags(), flavorTags);
                        int match2 = countMatchedTags(d2.getFlavorTags(), flavorTags);
                        return Integer.compare(match2, match1);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            ruleSnapshot.put("maxCalorie", maxCalorie);
            ruleSnapshot.put("matchedFlavorTags", flavorTags);

            matchReason = String.format("【AI均衡推荐】根据您的口味偏好精心搭配，均衡热量控制在 %d 大卡以内，美味无负担", maxCalorie);
            if (!matchedDishes.isEmpty()) {
                matchReason += String.format("，为您挑选出 %d 道高分精选好味。", matchedDishes.size());
            }
        }

        List<String> dishIds = matchedDishes.stream().map(Dish::getId).collect(Collectors.toList());
        List<DishResponse> dishResponses = matchedDishes.stream().map(this::toDishResponse).collect(Collectors.toList());

        return RecommendResult.builder()
                .dishIds(dishIds)
                .dishes(dishResponses)
                .matchReason(matchReason)
                .ruleSnapshot(ruleSnapshot)
                .build();
    }

    @Override
    public String chat(String userId, String message) {
        log.info("[SpringAiAgent] 用户咨询点餐与营养: userId={}, message={}", userId, message);

        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElse(null);
        String currentMode = preference != null && preference.getRecommendMode() != null
                ? preference.getRecommendMode()
                : "normal";

        String modeDesc = switch (currentMode) {
            case "slim" -> "减脂塑形（热量严格≤500大卡）";
            case "indulge" -> "放纵享受（无热量限制）";
            default -> "日常均衡（热量≤800大卡）";
        };

        StringBuilder reply = new StringBuilder();
        reply.append(String.format("【AI饭搭子·营养助手】您好！已获悉您当前处于 %s 模式。", modeDesc));

        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("热量") || lowerMsg.contains("卡路里") || lowerMsg.contains("低卡") || lowerMsg.contains("减肥")) {
            reply.append(" 建议多选择清蒸水煮、高蛋白鸡胸肉、牛油果及深绿色蔬菜沙拉，避免隐形糖和重油酱汁。");
        } else if (lowerMsg.contains("辣") || lowerMsg.contains("川味") || lowerMsg.contains("重口味")) {
            reply.append(" 为您关注到您对辛辣风味的需求，若在减脂期，推荐干碟辣椒粉替代重油辣油哦！");
        } else if (lowerMsg.contains("推荐") || lowerMsg.contains("吃什么") || lowerMsg.contains("点餐")) {
            reply.append(" 您可以直接在首页或推荐页点击“生成AI推荐”，系统将按您的饮食偏好秒级过滤出最合心意的门店菜品！");
        } else {
            reply.append(" 无论想吃清淡减脂还是解馋解压，我随时为您匹配最契合的热量与风味方案，快去挑选中意的菜品吧！");
        }

        return reply.toString();
    }

    private boolean hasAllergyConflict(Dish dish, List<String> allergyTags) {
        if (allergyTags == null || allergyTags.isEmpty()) {
            return false;
        }
        Set<String> dishIngredients = new HashSet<>();
        if (dish.getIngredientTags() != null) {
            dishIngredients.addAll(dish.getIngredientTags());
        }
        if (dish.getFlavorTags() != null) {
            dishIngredients.addAll(dish.getFlavorTags());
        }
        if (dish.getName() != null) {
            dishIngredients.add(dish.getName());
        }
        for (String allergy : allergyTags) {
            for (String ing : dishIngredients) {
                if (ing.contains(allergy) || allergy.contains(ing)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countMatchedTags(List<String> dishTags, List<String> userTags) {
        if (dishTags == null || userTags == null) {
            return 0;
        }
        int count = 0;
        for (String ut : userTags) {
            if (dishTags.contains(ut)) {
                count++;
            }
        }
        return count;
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
