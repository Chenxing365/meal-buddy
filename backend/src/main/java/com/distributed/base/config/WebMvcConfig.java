package com.distributed.base.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 拦截器配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns(
                        "/api/user/**",
                        "/api/admin/**",
                        "/api/merchant/**",
                        "/api/dish",
                        "/api/dish/**",
                        "/api/recommend/**",
                        "/api/ai/**",
                        "/api/order",
                        "/api/order/**",
                        "/api/retrieval/**",
                        // 联调接口：读需登录，写操作在 ApiController 内额外校验管理员角色
                        "/api/mongo/**",
                        "/api/redis/**"
                )
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login"
                );
    }
}
