package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.AiChatMessage;
import com.distributed.base.dto.AiChatRequest;
import com.distributed.base.dto.AiChatResponse;
import com.distributed.base.service.RecommendService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 饭搭子多轮会话 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 饭搭子会话接口", description = "多轮点餐咨询、营养助手问答与最近 10 轮会话记忆追溯")
public class AiChatController {

    private final RecommendService recommendService;

    @PostMapping("/chat")
    @Operation(summary = "与 AI 饭搭子互动对话", description = "支持多轮点餐推荐与营养咨询，会话自动存入 Redis List 保留最近 10 轮")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest req) {
        String userId = UserContext.getUserId();
        AiChatResponse response = recommendService.chatWithAgent(userId, req.getMessage());
        return Result.ok(response);
    }

    @GetMapping("/chat/history")
    @Operation(summary = "拉取最近会话记录", description = "获取当前用户与 AI 饭搭子的最近 10 轮会话历史")
    public Result<List<AiChatMessage>> getChatHistory() {
        String userId = UserContext.getUserId();
        List<AiChatMessage> history = recommendService.getChatHistory(userId);
        return Result.ok(history);
    }
}
