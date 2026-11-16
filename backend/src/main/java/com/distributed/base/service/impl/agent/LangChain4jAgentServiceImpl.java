package com.distributed.base.service.impl.agent;

import com.distributed.base.dto.DishResponse;
import com.distributed.base.dto.RecommendResult;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.Merchant;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.retrieval.MealBuddyRetriever;
import com.distributed.base.retrieval.RetrievalQuery;
import com.distributed.base.retrieval.RetrievalResult;
import com.distributed.base.service.MealBuddyAgentService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meal-buddy.agent.engine", havingValue = "langchain4j")
public class LangChain4jAgentServiceImpl implements MealBuddyAgentService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final DishRepository dishRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final MerchantRepository merchantRepository;
    private final ChatLanguageModel chatLanguageModel;
    private final MealBuddyRetriever mealBuddyRetriever;

    private final Map<String, List<ChatMessage>> historyStore = new ConcurrentHashMap<>();

    @Override
    public RecommendResult generateRecommendation(String userId, String mode) {
        log.info("[LangChain4jAgent] 智能推荐: userId={}, mode={}", userId, mode);

        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElse(null);
        List<String> allergyTags = preference != null && preference.getAllergyTags() != null
                ? preference.getAllergyTags() : Collections.emptyList();

        Map<String, Merchant> merchantIndex = loadMerchantIndex();

        List<Dish> candidates = dishRepository.findByStatusAndIsDeletedFalse(1).stream()
                .filter(d -> isServingMerchantDish(d, merchantIndex))
                .filter(d -> !hasAllergyConflict(d, allergyTags))
                .collect(Collectors.toList());

        int maxCalorie = resolveMaxCalorie(mode, preference);
        List<Dish> matched = candidates.stream()
                .filter(d -> d.getCalorie() != null && d.getCalorie() <= maxCalorie)
                .sorted(Comparator.comparingInt(Dish::getCalorie))
                .limit(5)
                .collect(Collectors.toList());

        List<DishResponse> dishResponses = matched.stream()
                .map(d -> toDishResponse(d, merchantIndex))
                .collect(Collectors.toList());

        Map<String, Object> ruleSnapshot = new HashMap<>();
        ruleSnapshot.put("engine", "langchain4j");
        ruleSnapshot.put("mode", mode);
        ruleSnapshot.put("maxCalorie", maxCalorie);
        ruleSnapshot.put("allergyExcluded", allergyTags);

        String matchReason = generateMatchReason(mode, matched, maxCalorie);

        RetrievalResult retrieval = mealBuddyRetriever.retrieve(RetrievalQuery.builder()
                .userId(userId)
                .text(buildRetrievalText(mode, preference))
                .mode(mode)
                .maxCalorie(maxCalorie)
                .limit(8)
                .build());
        ruleSnapshot.put("retrieval", summarizeRetrieval(retrieval));

        return RecommendResult.builder()
                .dishIds(matched.stream().map(Dish::getId).collect(Collectors.toList()))
                .dishes(dishResponses)
                .matchReason(matchReason)
                .ruleSnapshot(ruleSnapshot)
                .build();
    }

    @Override
    public String chat(String userId, String message) {
        log.info("[LangChain4jAgent] 对话: userId={}, message={}", userId, message);

        List<ChatMessage> history = historyStore.computeIfAbsent(userId, k -> new ArrayList<>());

        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElse(null);
        RetrievalResult retrieval = mealBuddyRetriever.retrieve(RetrievalQuery.builder()
                .userId(userId)
                .text(message)
                .mode(preference != null ? preference.getRecommendMode() : "normal")
                .maxCalorie(preference != null ? preference.getCalorieLimit() : null)
                .excludeIngredients(preference != null ? preference.getAllergyTags() : null)
                .limit(8)
                .build());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(buildSystemPrompt(preference, retrieval)));
        messages.addAll(history);
        messages.add(UserMessage.from(message));

        String reply;
        try {
            reply = chatLanguageModel.generate(messages).content().text();
        } catch (Exception e) {
            log.error("[LangChain4jAgent] 模型调用失败: {}", e.getMessage());
            return "【LangChain4j 助手】抱歉，AI 服务暂时不可用，请稍后再试。";
        }

        if (!StringUtils.hasText(reply)) {
            return "【LangChain4j 助手】抱歉，我没有理解您的问题，可以再说一次吗？";
        }

        history.add(UserMessage.from(message));
        history.add(AiMessage.from(reply));
        while (history.size() > MAX_HISTORY_MESSAGES) {
            history.remove(0);
        }
        return reply.trim();
    }

    private String buildSystemPrompt(UserPreference preference, RetrievalResult retrieval) {
        String mode = preference != null && preference.getRecommendMode() != null
                ? preference.getRecommendMode() : "normal";

        return String.format("""
                你叫【AI饭搭子】，是“恰饭啦”美食消费平台的首席AI营养顾问。
                当前用户模式：%s；热量上限：%s 大卡；忌口：%s；口味偏好：%s。

                当前在售菜品（唯一事实来源，只能从这里推荐）：
                %s

                检索层上下文（会话层级摘要与候选菜品，仅作参考，不得替代在售菜品清单）：
                %s

                要求：回答亲切自然，80~200字；禁止编造清单外的菜名或价格；不支持截图与定位。
                """,
                mode,
                preference != null && preference.getCalorieLimit() != null ? preference.getCalorieLimit() : 800,
                preference != null && preference.getAllergyTags() != null ? preference.getAllergyTags() : "无",
                preference != null && preference.getFlavorTags() != null ? preference.getFlavorTags() : "通用",
                buildDishContext(),
                buildRetrievalContext(retrieval));
    }

    private String buildRetrievalContext(RetrievalResult retrieval) {
        if (retrieval == null || retrieval.getHits().isEmpty()) {
            return "（无检索结果）";
        }
        return retrieval.getHits().stream()
                .limit(6)
                .map(h -> String.format("- [%s] %s", h.getDocType(), h.getTitle()))
                .collect(Collectors.joining("\n"));
    }

    private Map<String, Object> summarizeRetrieval(RetrievalResult retrieval) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hitCount", retrieval.getHits().size());
        summary.put("degraded", retrieval.isDegraded());
        summary.put("elapsedMs", retrieval.getElapsedMs());
        summary.put("trace", retrieval.getTrace().stream()
                .map(t -> t.getStrategy() + ":" + t.getStatus())
                .collect(Collectors.toList()));
        return summary;
    }

    private String buildRetrievalText(String mode, UserPreference preference) {
        StringBuilder sb = new StringBuilder(mode == null ? "" : mode);
        if (preference != null) {
            if (preference.getFlavorTags() != null) {
                sb.append(' ').append(String.join(" ", preference.getFlavorTags()));
            }
            if (preference.getAllergyTags() != null) {
                sb.append(" 忌口:").append(String.join(" ", preference.getAllergyTags()));
            }
        }
        return sb.toString().trim();
    }

    private String buildDishContext() {
        Map<String, Merchant> merchantIndex = loadMerchantIndex();
        List<Dish> dishes = dishRepository.findByStatusAndIsDeletedFalse(1).stream()
                .filter(d -> isServingMerchantDish(d, merchantIndex))
                .limit(12)
                .collect(Collectors.toList());
        if (dishes.isEmpty()) {
            return "（当前无在售菜品）";
        }
        return dishes.stream()
                .map(d -> String.format("- %s | 门店:%s | %s元 | %s大卡 | 口味:%s",
                        d.getName(),
                        resolveMerchantName(d, merchantIndex) != null ? resolveMerchantName(d, merchantIndex) : "平台自营",
                        d.getPrice() != null ? d.getPrice() : "-",
                        d.getCalorie() != null ? d.getCalorie() : "-",
                        d.getFlavorTags() != null && !d.getFlavorTags().isEmpty() ? String.join("/", d.getFlavorTags()) : "-"))
                .collect(Collectors.joining("\n"));
    }

    private String generateMatchReason(String mode, List<Dish> dishes, int maxCalorie) {
        if (dishes.isEmpty()) {
            return "【LangChain4j 推荐】当前没有符合条件的菜品，请稍后再试。";
        }
        String dishSummary = dishes.stream()
                .map(d -> String.format("%s(%d大卡)", d.getName(), d.getCalorie() != null ? d.getCalorie() : 0))
                .collect(Collectors.joining("、"));
        String prompt = String.format(
                "用户模式：%s，热量上限：%d 大卡，候选菜品：%s。请用50字以内生成一段推荐语，以【AI推荐】开头。",
                mode, maxCalorie, dishSummary);
        try {
            String reason = chatLanguageModel.generate(prompt);
            if (StringUtils.hasText(reason)) {
                return reason.trim();
            }
        } catch (Exception e) {
            log.warn("[LangChain4jAgent] 推荐语生成失败，使用模板: {}", e.getMessage());
        }
        return String.format("【AI推荐】已按 %s 模式为您精选 5 道菜品，单餐热量控制在 %d 大卡以内。", mode, maxCalorie);
    }

    private int resolveMaxCalorie(String mode, UserPreference preference) {
        if ("slim".equalsIgnoreCase(mode)) {
            return 500;
        }
        if ("indulge".equalsIgnoreCase(mode)) {
            return Integer.MAX_VALUE;
        }
        return preference != null && preference.getCalorieLimit() != null && preference.getCalorieLimit() > 0
                ? preference.getCalorieLimit() : 800;
    }

    private boolean hasAllergyConflict(Dish dish, List<String> allergyTags) {
        if (allergyTags.isEmpty()) {
            return false;
        }
        Set<String> ingredients = new HashSet<>();
        if (dish.getIngredientTags() != null) {
            ingredients.addAll(dish.getIngredientTags());
        }
        if (dish.getFlavorTags() != null) {
            ingredients.addAll(dish.getFlavorTags());
        }
        if (dish.getName() != null) {
            ingredients.add(dish.getName());
        }
        for (String allergy : allergyTags) {
            for (String ing : ingredients) {
                if (ing.contains(allergy) || allergy.contains(ing)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, Merchant> loadMerchantIndex() {
        List<Dish> dishes = dishRepository.findByStatusAndIsDeletedFalse(1);
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
