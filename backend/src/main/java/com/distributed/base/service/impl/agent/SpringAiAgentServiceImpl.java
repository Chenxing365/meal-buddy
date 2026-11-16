package com.distributed.base.service.impl.agent;

import com.distributed.base.dto.DishResponse;
import com.distributed.base.dto.RecommendResult;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.Merchant;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.service.MealBuddyAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring AI 驱动的饭搭子智能体引擎实现
 *
 * <p>阶段一正式接入 Spring AI，融合热量规则引擎与可解释性 AI 推荐话术。
 * 当大模型未配置或调用异常时，自动平滑降级至本地规则引擎话术，保障业务零中断。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "meal-buddy.agent.engine", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiAgentServiceImpl implements MealBuddyAgentService {

    private final DishRepository dishRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final MerchantRepository merchantRepository;
    private final ChatClient chatClient;

    @Autowired
    public SpringAiAgentServiceImpl(
            DishRepository dishRepository,
            UserPreferenceRepository userPreferenceRepository,
            MerchantRepository merchantRepository,
            @Autowired(required = false) ChatClient chatClient) {
        this.dishRepository = dishRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.merchantRepository = merchantRepository;
        this.chatClient = chatClient;
        if (this.chatClient != null) {
            log.info("[SpringAiAgent] Spring AI ChatClient 注入成功，大模型真实驱动已激活");
        } else {
            log.warn("[SpringAiAgent] 未检测到 ChatClient Bean，将运行在优雅降级规则引擎模式");
        }
    }

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

        // 只推荐营业中（status=1）门店的菜品，并与门店详情页口径保持一致
        Map<String, Merchant> merchantIndex = loadMerchantIndex(candidateDishes);
        candidateDishes = candidateDishes.stream()
                .filter(d -> isServingMerchantDish(d, merchantIndex))
                .collect(Collectors.toList());

        List<Dish> matchedDishes;
        String matchReason;
        Map<String, Object> ruleSnapshot = new HashMap<>();
        ruleSnapshot.put("engine", "spring-ai");
        ruleSnapshot.put("mode", mode);
        ruleSnapshot.put("allergyExcluded", allergyTags);
        ruleSnapshot.put("merchantFilter", "status=1");

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

            matchReason = generateAiMatchReason(matchedDishes, mode, preference,
                    String.format("【AI减脂推荐】为您匹配低卡轻食，单餐热量严格控制在 %d 大卡以内，少油健康无负担", maxCalorie));
        } else if ("indulge".equalsIgnoreCase(mode)) {
            matchedDishes = candidateDishes.stream()
                    .sorted(Comparator.comparingInt((Dish d) -> d.getCalorie() != null ? d.getCalorie() : 0).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            ruleSnapshot.put("calorieRestricted", false);
            ruleSnapshot.put("sortStrategy", "high_calorie_and_rich_flavor");

            matchReason = generateAiMatchReason(matchedDishes, mode, preference,
                    "【AI放纵模式】放纵美味模式：不设热量限制，尽情享受浓郁醇厚、满足多巴胺的高品质佳肴！");
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

            matchReason = generateAiMatchReason(matchedDishes, mode, preference,
                    String.format("【AI均衡推荐】根据您的口味偏好精心搭配，均衡热量控制在 %d 大卡以内，美味无负担", maxCalorie));
        }

        List<String> dishIds = matchedDishes.stream().map(Dish::getId).collect(Collectors.toList());
        List<DishResponse> dishResponses = matchedDishes.stream()
                .map(d -> toDishResponse(d, merchantIndex))
                .collect(Collectors.toList());

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

        // 尝试调用 Spring AI 大模型
        if (chatClient != null) {
            try {
                String systemPrompt = String.format("""
                        你叫【AI饭搭子】，是“恰饭啦”美食消费平台的首席贴心AI营养顾问。
                        你的职责是给用户提供精准、专业且温暖接地气的选餐、换菜、热量和营养搭配建议。
                        当前用户饮食偏好上下文：
                        - 当前模式：%s
                        - 偏好热量上限：%s 大卡
                        - 忌口过敏原：%s
                        - 口味偏好：%s
                        要求：
                        1. 语言自然亲切、富有食欲感与人情味。
                        2. 围绕用户的饮食模式给出有见地的搭配，不啰嗦，字数控制在 80~150 字左右。
                        """,
                        modeDesc,
                        preference != null && preference.getCalorieLimit() != null ? preference.getCalorieLimit() : 800,
                        preference != null && preference.getAllergyTags() != null ? preference.getAllergyTags() : "无",
                        preference != null && preference.getFlavorTags() != null ? preference.getFlavorTags() : "通用"
                );

                String aiResponse = chatClient.prompt()
                        .system(systemPrompt)
                        .user(message)
                        .call()
                        .content();

                if (StringUtils.hasText(aiResponse)) {
                    return aiResponse.trim();
                }
            } catch (Exception e) {
                log.error("[SpringAiAgent] 调用 Spring AI 大模型对话失败，自动平滑降级至规则引擎: {}", e.getMessage());
            }
        }

        // 兜底降级：本地规则匹配话术
        return fallbackChat(currentMode, modeDesc, message);
    }

    /**
     * 结合大模型为候选菜品生成生动有说服力的个性化推荐理由（带本地降级）
     */
    private String generateAiMatchReason(List<Dish> dishes, String mode, UserPreference preference, String fallbackReason) {
        if (chatClient == null || dishes.isEmpty()) {
            return fallbackReason;
        }

        try {
            String dishSummary = dishes.stream()
                    .map(d -> String.format("%s(%d大卡)", d.getName(), d.getCalorie() != null ? d.getCalorie() : 0))
                    .collect(Collectors.joining("、"));

            String systemPrompt = """
                    你是【恰饭啦】AI美食推荐官。根据用户当前模式和入选的精选菜品，生成一段 50 字以内、具有强烈食欲感与专业营养依据的推荐语。
                    格式要求：以【AI某某推荐】开头，语气亲切热情。
                    """;

            String userPrompt = String.format("用户模式：%s，候选菜品：%s。请给出推荐语：", mode, dishSummary);

            String reason = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            if (StringUtils.hasText(reason)) {
                return reason.trim();
            }
        } catch (Exception e) {
            log.warn("[SpringAiAgent] 生成推荐理由时 LLM 调用异常，启用模板理由: {}", e.getMessage());
        }

        return fallbackReason;
    }

    private String fallbackChat(String currentMode, String modeDesc, String message) {
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

    /**
     * 批量装载候选菜品所属门店，避免逐条查询造成 N+1
     */
    private Map<String, Merchant> loadMerchantIndex(List<Dish> dishes) {
        List<String> merchantIds = dishes.stream()
                .map(Dish::getMerchantId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return merchantRepository.findAllById(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, m -> m, (a, b) -> a));
    }

    /**
     * 门店若已下线/待审核，其菜品不应出现在首页推荐中；无门店关联的历史数据放行
     */
    private boolean isServingMerchantDish(Dish dish, Map<String, Merchant> merchantIndex) {
        if (dish.getMerchantId() == null || dish.getMerchantId().isBlank()) {
            return true;
        }
        Merchant merchant = merchantIndex.get(dish.getMerchantId());
        return merchant != null && merchant.getStatus() != null && merchant.getStatus() == 1;
    }

    private String resolveMerchantName(Dish dish, Map<String, Merchant> merchantIndex) {
        if (dish.getMerchantId() == null || dish.getMerchantId().isBlank()) {
            return null;
        }
        Merchant merchant = merchantIndex.get(dish.getMerchantId());
        return merchant != null ? merchant.getName() : null;
    }

    private DishResponse toDishResponse(Dish d, Map<String, Merchant> merchantIndex) {
        return DishResponse.builder()
                .id(d.getId())
                .merchantId(d.getMerchantId())
                .merchantName(resolveMerchantName(d, merchantIndex))
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
