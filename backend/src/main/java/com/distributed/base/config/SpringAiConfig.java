package com.distributed.base.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 基础配置与 ChatClient 适配
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:mock-key-placeholder}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:deepseek-flash}")
    private String modelName;

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel chatModel() {
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(modelName)
                .withTemperature(0.7f)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

