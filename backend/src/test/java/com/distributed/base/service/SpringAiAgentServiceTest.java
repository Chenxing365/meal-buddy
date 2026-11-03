package com.distributed.base.service;

import com.distributed.base.dto.RecommendResult;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.service.impl.agent.SpringAiAgentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M3阶段-SpringAiAgentService规则引擎与推荐话术单元测试")
class SpringAiAgentServiceTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    private SpringAiAgentServiceImpl agentService;

    @BeforeEach
    void setUp() {
        agentService = new SpringAiAgentServiceImpl(dishRepository, userPreferenceRepository);
    }

    @Test
    @DisplayName("测试 slim 模式：严格过滤热量<=500大卡并排除忌口标签")
    void testSlimModeFiltering() {
        String userId = "u_user_1";

        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .recommendMode("slim")
                .calorieLimit(500)
                .allergyTags(List.of("花生"))
                .build();

        Dish d1 = Dish.builder().id("d1").name("低卡彩椒鸡胸").calorie(350).ingredientTags(List.of("鸡胸肉", "彩椒")).status(1).build();
        Dish d2 = Dish.builder().id("d2").name("香炸花生鸡排").calorie(420).ingredientTags(List.of("鸡排", "花生碎")).status(1).build();
        Dish d3 = Dish.builder().id("d3").name("重油红烧蹄髈").calorie(950).ingredientTags(List.of("猪肉")).status(1).build();
        Dish d4 = Dish.builder().id("d4").name("清爽水煮虾仁").calorie(280).ingredientTags(List.of("鲜虾", "西兰花")).status(1).build();

        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(dishRepository.findByStatusAndIsDeletedFalse(1)).thenReturn(List.of(d1, d2, d3, d4));

        RecommendResult result = agentService.generateRecommendation(userId, "slim");

        assertNotNull(result);
        assertEquals(2, result.getDishes().size());
        assertTrue(result.getDishes().stream().anyMatch(d -> d.getId().equals("d1")));
        assertTrue(result.getDishes().stream().anyMatch(d -> d.getId().equals("d4")));
        assertFalse(result.getDishes().stream().anyMatch(d -> d.getId().equals("d2"))); // 包含花生忌口
        assertFalse(result.getDishes().stream().anyMatch(d -> d.getId().equals("d3"))); // 热量950超标
        assertTrue(result.getDishes().stream().allMatch(d -> d.getCalorie() <= 500));
        assertTrue(result.getMatchReason().contains("500 大卡以内"));
    }

    @Test
    @DisplayName("测试 normal 模式：热量<=800并优先匹配口味偏好标签")
    void testNormalModeSorting() {
        String userId = "u_user_2";

        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .recommendMode("normal")
                .calorieLimit(800)
                .flavorTags(List.of("川味", "麻辣"))
                .build();

        Dish d1 = Dish.builder().id("d1").name("清汤素面").calorie(400).flavorTags(List.of("清淡")).status(1).build();
        Dish d2 = Dish.builder().id("d2").name("川味麻辣抄手").calorie(650).flavorTags(List.of("川味", "麻辣")).status(1).build();
        Dish d3 = Dish.builder().id("d3").name("超大份全家桶").calorie(1200).flavorTags(List.of("香脆")).status(1).build();

        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(dishRepository.findByStatusAndIsDeletedFalse(1)).thenReturn(List.of(d1, d2, d3));

        RecommendResult result = agentService.generateRecommendation(userId, "normal");

        assertNotNull(result);
        assertEquals(2, result.getDishes().size());
        assertEquals("d2", result.getDishes().get(0).getId()); // 口味匹配度高排在首位
        assertTrue(result.getDishes().stream().allMatch(d -> d.getCalorie() <= 800));
    }

    @Test
    @DisplayName("测试 indulge 放纵模式：不限制热量并优先展现高满足度菜品")
    void testIndulgeMode() {
        String userId = "u_user_3";

        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .recommendMode("indulge")
                .build();

        Dish d1 = Dish.builder().id("d1").name("轻食黄瓜条").calorie(50).status(1).build();
        Dish d2 = Dish.builder().id("d2").name("芝士拉丝双层和牛堡").calorie(1100).status(1).build();

        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(dishRepository.findByStatusAndIsDeletedFalse(1)).thenReturn(List.of(d1, d2));

        RecommendResult result = agentService.generateRecommendation(userId, "indulge");

        assertNotNull(result);
        assertEquals(2, result.getDishes().size());
        assertEquals("d2", result.getDishes().get(0).getId()); // 最高热量排在首位
        assertTrue(result.getMatchReason().contains("放纵美味模式"));
    }
}
