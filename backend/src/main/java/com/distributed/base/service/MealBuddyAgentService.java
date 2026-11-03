package com.distributed.base.service;

import com.distributed.base.dto.RecommendResult;

/**
 * AI 饭搭子智能体统一引擎服务抽象
 *
 * <p>面向业务层统一屏蔽底层 AI 框架细节（支持 Spring AI 与 LangChain4j 适配）
 */
public interface MealBuddyAgentService {

    /**
     * 根据用户偏好与当前模式生成智能推荐（含菜品列表与推荐理由）
     *
     * @param userId 用户 ID
     * @param mode 推荐模式（slim/normal/indulge）
     * @return 推荐结果
     */
    RecommendResult generateRecommendation(String userId, String mode);

    /**
     * 多轮点餐咨询与营养助手对话
     *
     * @param userId 用户 ID
     * @param message 用户消息
     * @return AI 助手回复
     */
    String chat(String userId, String message);
}
