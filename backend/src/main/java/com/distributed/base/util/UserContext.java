package com.distributed.base.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前请求上下文中的登录用户信息
 */
public class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentUser {
        private String userId;
        private String username;
        private Integer role;
    }

    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static CurrentUser get() {
        return CURRENT_USER.get();
    }

    public static String getUserId() {
        CurrentUser user = CURRENT_USER.get();
        return user != null ? user.getUserId() : null;
    }

    public static Integer getRole() {
        CurrentUser user = CURRENT_USER.get();
        return user != null ? user.getRole() : null;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
