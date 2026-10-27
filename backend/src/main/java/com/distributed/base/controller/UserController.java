package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.*;
import com.distributed.base.service.UserService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * 用户前台业务 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户核心域接口", description = "用户注册、登录、信息维护、饮食偏好设定及收藏")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "校验用户名唯一，手机号 AES 加密存储，密码 BCrypt 散列")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        UserResponse response = userService.register(req);
        return Result.ok("注册成功", response);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "返回 Access Token (2小时) 与 Refresh Token (7天)")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse response = userService.login(req);
        return Result.ok("登录成功", response);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息", description = "根据 Bearer Token 识别身份，返回脱敏手机号")
    public Result<UserResponse> getUserInfo() {
        String userId = UserContext.getUserId();
        UserResponse response = userService.getUserInfo(userId);
        return Result.ok(response);
    }

    @PutMapping("/info")
    @Operation(summary = "修改当前登录用户信息", description = "更新昵称与头像")
    public Result<UserResponse> updateUserInfo(@RequestBody UpdateUserRequest req) {
        String userId = UserContext.getUserId();
        UserResponse response = userService.updateUserInfo(userId, req);
        return Result.ok("修改成功", response);
    }

    @GetMapping("/preferences")
    @Operation(summary = "获取当前用户饮食偏好", description = "优先命中 Redis 偏好缓存")
    public Result<UserPreferenceDto> getPreferences() {
        String userId = UserContext.getUserId();
        UserPreferenceDto dto = userService.getPreferences(userId);
        return Result.ok(dto);
    }

    @PutMapping("/preferences")
    @Operation(summary = "更新饮食偏好", description = "支持修改 recommend_mode, flavor_tags, calorie_limit, allergy_tags")
    public Result<UserPreferenceDto> updatePreferences(@Valid @RequestBody UserPreferenceDto req) {
        String userId = UserContext.getUserId();
        UserPreferenceDto dto = userService.updatePreferences(userId, req);
        return Result.ok("偏好已更新", dto);
    }

    @PostMapping("/favorite/{type}")
    @Operation(summary = "收藏/取消收藏", description = "维护 Redis 关注集合，type 可选 merchant 或 dish")
    public Result<Map<String, Object>> toggleFavorite(
            @PathVariable("type") String type,
            @RequestBody Map<String, String> body) {
        String userId = UserContext.getUserId();
        String targetId = body.get("targetId");
        boolean isFavorited = userService.toggleFavorite(userId, type, targetId);
        return Result.ok(isFavorited ? "收藏成功" : "已取消收藏", Map.of("favorited", isFavorited));
    }

    @GetMapping("/favorites")
    @Operation(summary = "获取收藏列表", description = "按类型分页获取收藏目标 ID 集合")
    public Result<Set<String>> getFavorites(
            @RequestParam(value = "type", defaultValue = "dish") String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        String userId = UserContext.getUserId();
        Set<String> favorites = userService.getFavorites(userId, type, page, size);
        return Result.ok(favorites);
    }
}
