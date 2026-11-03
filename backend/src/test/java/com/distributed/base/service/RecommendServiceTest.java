package com.distributed.base.service;

import com.distributed.base.dto.*;
import com.distributed.base.entity.Dish;
import com.distributed.base.entity.RecommendLog;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.RecommendLogRepository;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.service.impl.RecommendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M3阶段-RecommendService推荐与AI会话业务单元测试")
class RecommendServiceTest {

    @Mock
    private MealBuddyAgentService mealBuddyAgentService;

    @Mock
    private RecommendLogRepository recommendLogRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, String> stringValueOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    private RecommendServiceImpl recommendService;

    @BeforeEach
    void setUp() {
        recommendService = new RecommendServiceImpl(
                mealBuddyAgentService,
                recommendLogRepository,
                dishRepository,
                userPreferenceRepository,
                stringRedisTemplate,
                redisTemplate
        );
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    @DisplayName("测试模式切换与获取：正确写入并读取 Redis 键 recommend:mode:{userId}")
    void testSwitchModeAndGetMode() {
        String userId = "u_test_100";

        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(
                UserPreference.builder().userId(userId).recommendMode("normal").build()
        ));

        recommendService.switchMode(userId, "slim");

        verify(stringValueOperations).set("recommend:mode:" + userId, "slim");

        when(stringValueOperations.get("recommend:mode:" + userId)).thenReturn("slim");
        String currentMode = recommendService.getCurrentMode(userId);
        assertEquals("slim", currentMode);
    }

    @Test
    @DisplayName("阶段验收标准1：slim模式下推荐返回菜品热量全部<=500大卡且理由包含卡路里说明，落库recommend_logs")
    void testSlimModeRecommendationAndLogPersistence() {
        String userId = "u_test_100";
        String mode = "slim";

        when(stringValueOperations.get("recommend:mode:" + userId)).thenReturn(mode);

        DishResponse lowCalDish = DishResponse.builder()
                .id("d_low_1")
                .name("香煎鸡胸藜麦沙拉")
                .calorie(380)
                .price(29.0)
                .build();

        RecommendResult agentResult = RecommendResult.builder()
                .dishIds(List.of("d_low_1"))
                .dishes(List.of(lowCalDish))
                .matchReason("【AI减脂推荐】为您匹配低卡轻食，单餐热量严格控制在 500 大卡以内，少油健康无负担")
                .ruleSnapshot(Map.of("mode", "slim", "maxCalorie", 500))
                .build();

        when(mealBuddyAgentService.generateRecommendation(userId, mode)).thenReturn(agentResult);

        when(recommendLogRepository.save(any(RecommendLog.class))).thenAnswer(invocation -> {
            RecommendLog l = invocation.getArgument(0);
            l.setId("log_999");
            l.setCreatedAt(LocalDateTime.now());
            return l;
        });

        RecommendResponse resp = recommendService.getRecommendations(userId);

        assertNotNull(resp);
        assertEquals("log_999", resp.getLogId());
        assertEquals("slim", resp.getMode());
        assertTrue(resp.getMatchReason().contains("500 大卡以内"));
        assertEquals(1, resp.getDishes().size());
        assertTrue(resp.getDishes().get(0).getCalorie() <= 500);

        ArgumentCaptor<RecommendLog> captor = ArgumentCaptor.forClass(RecommendLog.class);
        verify(recommendLogRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals("slim", captor.getValue().getMode());
        assertEquals(List.of("d_low_1"), captor.getValue().getDishIds());
        assertEquals(0, captor.getValue().getFeedback());
    }

    @Test
    @DisplayName("阶段验收标准2：用户点赞后，recommend_logs 对应记录 feedback 字段变为 1")
    void testSubmitFeedbackThumbsUp() {
        String userId = "u_test_100";
        String logId = "log_999";

        RecommendLog existingLog = RecommendLog.builder()
                .id(logId)
                .userId(userId)
                .mode("slim")
                .feedback(0)
                .build();

        when(recommendLogRepository.findById(logId)).thenReturn(Optional.of(existingLog));

        RecommendFeedbackRequest req = RecommendFeedbackRequest.builder()
                .logId(logId)
                .feedback(1) // 1-点赞
                .build();

        recommendService.submitFeedback(userId, req);

        assertEquals(1, existingLog.getFeedback());
        verify(recommendLogRepository).save(existingLog);
    }

    @Test
    @DisplayName("测试 AI 饭搭子多轮会话：生成回复并存入 Redis List 保留最新会话")
    void testChatWithAgent() {
        String userId = "u_test_100";
        String message = "减脂期中午推荐吃什么？";
        String aiReply = "【AI饭搭子·营养助手】建议选择高蛋白低脂轻食！";

        when(mealBuddyAgentService.chat(userId, message)).thenReturn(aiReply);

        AiChatResponse response = recommendService.chatWithAgent(userId, message);

        assertNotNull(response);
        assertEquals(aiReply, response.getReply());

        String chatKey = "ai:chat:history:" + userId;
        verify(listOperations, times(2)).rightPush(eq(chatKey), any());
        verify(listOperations).trim(eq(chatKey), eq(-20L), eq(-1L));
    }
}
