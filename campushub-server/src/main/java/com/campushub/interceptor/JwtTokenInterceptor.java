package com.campushub.interceptor;

import com.campushub.constant.JwtClaimsConstant;
import com.campushub.exception.BusinessException;
import com.campushub.utils.JwtTokenUtil;
import com.campushub.utils.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtTokenInterceptor implements HandlerInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException("请先登录");
        }

        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        Claims claims = jwtTokenUtil.parseToken(token);
        Object userId = claims.get(JwtClaimsConstant.USER_ID);
        if (userId == null) {
            throw new BusinessException("登录凭证无效");
        }

        UserContext.setCurrentUserId(Long.valueOf(String.valueOf(userId)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}
