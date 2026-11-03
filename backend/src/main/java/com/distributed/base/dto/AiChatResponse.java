package com.distributed.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 饭搭子对话响应体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** AI 回复文本 */
    private String reply;

    /** 响应生成时间戳 */
    private LocalDateTime timestamp;
}
