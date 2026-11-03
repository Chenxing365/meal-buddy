package com.distributed.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 切换 AI 推荐模式请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendModeRequest {

    @NotBlank(message = "推荐模式不能为空")
    @Pattern(regexp = "slim|normal|indulge", message = "推荐模式仅支持 slim(减肥) / normal(正常) / indulge(放纵)")
    private String mode;
}
