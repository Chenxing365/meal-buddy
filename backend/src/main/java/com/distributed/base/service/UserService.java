package com.distributed.base.service;

import com.distributed.base.dto.*;
import org.springframework.data.domain.Page;

import java.util.Set;

/**
 * 用户核心领域服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    UserResponse register(RegisterRequest req);

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest req);

    /**
     * 获取用户信息（脱敏）
     */
    UserResponse getUserInfo(String userId);

    /**
     * 修改用户信息（昵称/头像）
     */
    UserResponse updateUserInfo(String userId, UpdateUserRequest req);

    /**
     * 获取饮食偏好（优先读 Redis，未命中读 Mongo 并回写）
     */
    UserPreferenceDto getPreferences(String userId);

    /**
     * 更新饮食偏好（落库 Mongo 并同步刷新 Redis 缓存 7 天）
     */
    UserPreferenceDto updatePreferences(String userId, UserPreferenceDto req);

    /**
     * 收藏/取消收藏（商家或菜品），维护 Redis Set 关注集合
     *
     * @param userId   用户 ID
     * @param type     收藏类型：merchant / dish
     * @param targetId 目标商家或菜品 ID
     * @return true-已收藏，false-已取消
     */
    boolean toggleFavorite(String userId, String type, String targetId);

    /**
     * 分页获取当前用户收藏 ID 集合
     */
    Set<String> getFavorites(String userId, String type, int page, int size);

    /**
     * 管理端分页获取用户列表
     */
    Page<UserResponse> listUsersForAdmin(Integer role, Integer status, int page, int size);

    /**
     * 管理端启用/禁用账号
     */
    void updateUserStatus(String userId, Integer status);
}
