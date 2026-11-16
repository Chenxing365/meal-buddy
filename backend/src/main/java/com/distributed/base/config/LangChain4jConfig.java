package com.distributed.base.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "meal-buddy.agent.engine", havingValue = "langchain4j")
public class LangChain4jConfig {

    @Value("${meal-buddy.agent.langchain4j.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${meal-buddy.agent.langchain4j.api-key:${spring.ai.openai.api-key:mock-key-placeholder}}")
    private String apiKey;

    @Value("${meal-buddy.agent.langchain4j.model:${spring.ai.openai.chat.options.model:deepseek-flash}}")
    private String modelName;

    @Bean
    public ChatLanguageModel langChain4jChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl.endsWith("/v1") ? baseUrl : baseUrl + "/v1")
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
