package com.distributed.base.config;

import com.distributed.base.exception.BusinessException;
import com.distributed.base.util.JwtUtil;
import com.distributed.base.util.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截与鉴权拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 放行公共 GET 查询（门店列表/详情、菜品列表/详情）
        boolean isPublicGet = "GET".equalsIgnoreCase(request.getMethod()) &&
                (request.getRequestURI().startsWith("/api/merchant/") || request.getRequestURI().startsWith("/api/dish"));

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            if (isPublicGet) {
                return true;
            }
            throw new BusinessException(401, "请先登录（缺少 Bearer Token）");
        }

        String token = authHeader.substring(7).trim();
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            throw new BusinessException(401, "登录凭证已过期或无效");
        }

        String jti = claims.getId();
        if (StringUtils.hasText(jti)) {
            Boolean isBlacklisted = stringRedisTemplate.hasKey("token:blacklist:" + jti);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                throw new BusinessException(401, "登录凭证已失效，请重新登录");
            }
        }

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        Integer role = claims.get("role", Integer.class);

        UserContext.set(UserContext.CurrentUser.builder()
                .userId(userId)
                .username(username)
                .role(role)
                .build());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
