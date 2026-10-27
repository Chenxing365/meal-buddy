package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息响应 DTO（脱敏手机号）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应对象")
public class UserResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户唯一 ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "脱敏手机号（138****1234）")
    private String phone;

    @Schema(description = "角色：0-普通用户，1-骑手，2-商家，9-管理员")
    private Integer role;

    @Schema(description = "状态：0-禁用，1-正常")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;
}
