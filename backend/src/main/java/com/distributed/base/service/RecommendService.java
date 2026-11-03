package com.distributed.base.service;

import com.distributed.base.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * AI 推荐业务服务接口
 */
public interface RecommendService {

    /**
     * 切换当前用户的推荐模式（slim / normal / indulge）
     *
     * @param userId 用户 ID
     * @param mode 模式
     */
    void switchMode(String userId, String mode);

    /**
     * 获取当前用户的推荐模式
     *
     * @param userId 用户 ID
     * @return 当前模式
     */
    String getCurrentMode(String userId);

    /**
     * 执行推荐计算，落库推荐日志并返回推荐菜品列表及推荐理由
     *
     * @param userId 用户 ID
     * @return 推荐结果
     */
    RecommendResponse getRecommendations(String userId);

    /**
     * 针对历史推荐日志提交点赞/点踩反馈
     *
     * @param userId 用户 ID
     * @param req 反馈请求
     */
    void submitFeedback(String userId, RecommendFeedbackRequest req);

    /**
     * 分页查询用户历史推荐记录
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param size 每页数量
     * @return 历史记录分页
     */
    Page<RecommendResponse> getHistory(String userId, int page, int size);

    /**
     * 与 AI 饭搭子进行多轮交互
     *
     * @param userId 用户 ID
     * @param message 用户消息
     * @return AI 对话回复
     */
    AiChatResponse chatWithAgent(String userId, String message);

    /**
     * 获取最近的多轮会话记录（最新 10 轮）
     *
     * @param userId 用户 ID
     * @return 会话消息列表
     */
    List<AiChatMessage> getChatHistory(String userId);
}
