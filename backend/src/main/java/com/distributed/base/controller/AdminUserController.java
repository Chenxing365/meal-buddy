package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.UserResponse;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.service.UserService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户平台管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "平台用户管理接口", description = "管理员分页检索用户、启用/禁用账号")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "管理员分页查询用户", description = "支持按角色和账号状态过滤")
    public Result<Page<UserResponse>> listUsers(
            @RequestParam(value = "role", required = false) Integer role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        checkAdmin();
        Page<UserResponse> userPage = userService.listUsersForAdmin(role, status, page, size);
        return Result.ok(userPage);
    }

    @PutMapping("/user/{id}/status")
    @Operation(summary = "启用/禁用用户账号", description = "更新 status 字段：0-禁用，1-正常")
    public Result<Void> updateUserStatus(
            @PathVariable("id") String id,
            @RequestBody Map<String, Integer> body) {
        checkAdmin();
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值非法（仅支持 0-禁用 或 1-正常）");
        }
        userService.updateUserStatus(id, status);
        return Result.ok(status == 1 ? "已启用该账号" : "已禁用该账号", null);
    }

    private void checkAdmin() {
        Integer role = UserContext.getRole();
        if (role == null || role != 9) {
            throw new BusinessException(403, "无权访问：需要平台管理员权限");
        }
    }
}
