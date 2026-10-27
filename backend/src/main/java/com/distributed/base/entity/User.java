package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户核心实体
 *
 * <p>对应 MongoDB 集合：{@code users}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户主键 ID */
    @Id
    private String id;

    /** 用户名：4~20位，字母开头，字母数字下划线 */
    @Indexed(unique = true)
    @Field("username")
    private String username;

    /** 登录密码（BCrypt 散列加密） */
    @Field("password")
    private String password;

    /** 用户昵称 */
    @Field("nickname")
    private String nickname;

    /** 用户头像 URL */
    @Field("avatar")
    private String avatar;

    /** 手机号（AES-128 加密存储） */
    @Indexed(unique = true)
    @Field("phone")
    private String phone;

    /** 角色：0-普通用户，1-骑手，2-商家，9-平台管理员 */
    @Builder.Default
    @Field("role")
    private Integer role = 0;

    /** 账号状态：0-禁用，1-正常 */
    @Builder.Default
    @Field("status")
    private Integer status = 1;

    /** 注册创建时间 */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
