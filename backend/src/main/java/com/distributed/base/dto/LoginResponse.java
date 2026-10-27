package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录成功响应 DTO（双 Token 结构）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应对象")
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌（有效期 2 小时）")
    private String accessToken;

    @Schema(description = "刷新令牌（有效期 7 天）")
    private String refreshToken;

    @Schema(description = "当前用户信息")
    private UserResponse user;
}
