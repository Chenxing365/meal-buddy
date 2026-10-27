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
                        "/api/merchant/apply",
                        "/api/merchant/info",
                        "/api/dish",
                        "/api/dish/*"
                )
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login",
                        "/api/dish/merchant/*"
                );
    }
}
