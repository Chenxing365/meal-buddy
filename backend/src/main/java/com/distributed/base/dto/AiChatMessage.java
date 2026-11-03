package com.distributed.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 饭搭子多轮会话消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色：user 或 assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 时间戳 */
    private LocalDateTime timestamp;
}
