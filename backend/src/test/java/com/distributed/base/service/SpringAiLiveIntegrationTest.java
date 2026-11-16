package com.distributed.base.service;

import com.distributed.base.entity.UserPreference;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.service.impl.agent.SpringAiAgentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@DisplayName("Spring AI 真实 DeepSeek 模型连通集成测试（依赖环境变量 AI_API_KEY）")
class SpringAiLiveIntegrationTest {

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Test
    @DisplayName("测试直接调用 DeepSeek API 生成 AI 饭搭子营养建议")
    void testDirectSpringAiDeepSeekCall() {
        String apiKey = System.getenv("AI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("mock"),
                "未设置 AI_API_KEY，跳过真实模型连通测试");

        OpenAiApi openAiApi = new OpenAiApi("https://api.deepseek.com", apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("deepseek-flash")
                .withTemperature(0.7f)
                .build();
        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, options);
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        SpringAiAgentServiceImpl agentService = new SpringAiAgentServiceImpl(
                dishRepository, userPreferenceRepository, merchantRepository, chatClient);

        String reply = agentService.chat("test-user-live", "你好，我今天晚上去跑步了5公里，晚餐应该怎么吃？");

        System.out.println("========== [DeepSeek 真实模型输出] ==========");
        System.out.println(reply);
        System.out.println("===========================================");

        assertNotNull(reply);
        assertTrue(reply.length() > 10);
        assertTrue(!reply.contains("无论想吃清淡减脂还是解馋解压")); // 确保非降级模板，而是大模型输出
    }
}
