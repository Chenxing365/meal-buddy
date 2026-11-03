package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.RecommendFeedbackRequest;
import com.distributed.base.dto.RecommendModeRequest;
import com.distributed.base.dto.RecommendResponse;
import com.distributed.base.service.RecommendService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 推荐业务 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
@Tag(name = "AI 推荐接口", description = "AI 饭搭子模式切换、推荐计算、点赞点踩反馈与推荐历史追溯")
public class RecommendController {

    private final RecommendService recommendService;

    @PostMapping("/mode")
    @Operation(summary = "切换 AI 推荐模式", description = "支持 slim(减肥) / normal(正常) / indulge(放纵)")
    public Result<String> switchMode(@Valid @RequestBody RecommendModeRequest req) {
        String userId = UserContext.getUserId();
        recommendService.switchMode(userId, req.getMode());
        return Result.ok("推荐模式已切换为: " + req.getMode(), req.getMode());
    }

    @GetMapping("/mode")
    @Operation(summary = "获取当前推荐模式", description = "优先读取 Redis 缓存，默认 normal")
    public Result<Map<String, String>> getCurrentMode() {
        String userId = UserContext.getUserId();
        String mode = recommendService.getCurrentMode(userId);
        return Result.ok(Map.of("mode", mode));
    }

    @GetMapping("/list")
    @Operation(summary = "执行 AI 推荐计算", description = "按用户选定模式与健康偏好推荐菜品，落库 recommend_logs")
    public Result<RecommendResponse> getRecommendations() {
        String userId = UserContext.getUserId();
        RecommendResponse response = recommendService.getRecommendations(userId);
        return Result.ok(response);
    }

    @PostMapping("/feedback")
    @Operation(summary = "提交推荐点赞/点踩反馈", description = "1-赞，-1-踩，更新 MongoDB 日志 feedback 字段")
    public Result<Void> submitFeedback(@Valid @RequestBody RecommendFeedbackRequest req) {
        String userId = UserContext.getUserId();
        recommendService.submitFeedback(userId, req);
        return Result.ok("反馈提交成功", null);
    }

    @GetMapping("/history")
    @Operation(summary = "追溯历史推荐记录", description = "按时间倒序分页拉取用户推荐决策历史")
    public Result<Page<RecommendResponse>> getHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        String userId = UserContext.getUserId();
        Page<RecommendResponse> historyPage = recommendService.getHistory(userId, page, size);
        return Result.ok(historyPage);
    }
}
