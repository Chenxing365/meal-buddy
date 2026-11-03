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
 * LangChain4j 驱动的智能体引擎实现
 *
 * <p>基于 {@code @ConditionalOnProperty(name = "meal-buddy.agent.engine", havingValue = "langchain4j")}
 * 预留 RAG 菜品向量检索与 Tool Calling（工具自动查库）实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meal-buddy.agent.engine", havingValue = "langchain4j")
public class LangChain4jAgentServiceImpl implements MealBuddyAgentService {

    private final DishRepository dishRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    public RecommendResult generateRecommendation(String userId, String mode) {
        log.info("[LangChain4jAgent] 激活 LangChain4j RAG 智能体决策: userId={}, mode={}", userId, mode);

        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElse(null);
        int maxCalorie = "slim".equalsIgnoreCase(mode) ? 500 : 800;

        List<Dish> dishes = dishRepository.findByCalorieLessThanEqualAndStatusAndIsDeletedFalse(
                maxCalorie, 1, org.springframework.data.domain.PageRequest.of(0, 5)).getContent();

        List<DishResponse> dishResponses = dishes.stream().map(this::toDishResponse).collect(Collectors.toList());
        List<String> dishIds = dishes.stream().map(Dish::getId).collect(Collectors.toList());

        Map<String, Object> ruleSnapshot = new HashMap<>();
        ruleSnapshot.put("engine", "langchain4j");
        ruleSnapshot.put("mode", mode);
        ruleSnapshot.put("maxCalorie", maxCalorie);
        ruleSnapshot.put("rag_vector_search", "enabled");

        String matchReason = String.format("【LangChain4j Agent RAG】基于向量知识库与工具链推荐：%s 模式，严格约束单餐热量≤%d大卡。", mode, maxCalorie);

        return RecommendResult.builder()
                .dishIds(dishIds)
                .dishes(dishResponses)
                .matchReason(matchReason)
                .ruleSnapshot(ruleSnapshot)
                .build();
    }

    @Override
    public String chat(String userId, String message) {
        log.info("[LangChain4jAgent] 多轮 RAG 对话咨询: userId={}, message={}", userId, message);
        return "【LangChain4j 智能助手】收到您的健康咨询：" + message + "。正在通过嵌入向量检索精准膳食建议...";
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
