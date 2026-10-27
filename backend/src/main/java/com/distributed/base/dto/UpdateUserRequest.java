package com.distributed.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息修改请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息更新参数")
public class UpdateUserRequest {

    @Schema(description = "用户昵称", example = "大吃一惊的Jens")
    private String nickname;

    @Schema(description = "用户头像 URL", example = "https://example.com/avatar.png")
    private String avatar;
}
