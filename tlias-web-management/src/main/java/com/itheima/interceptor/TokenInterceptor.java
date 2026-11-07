package com.itheima.interceptor;

import com.itheima.utils.JwtUtils;
import com.itheima.utils.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();

        if(requestURI.contains("/login")){
            log.info("Login request to {}", requestURI);
            return true;
        }

        String token = request.getHeader("token");

        if(token == null || token.isEmpty()){
            log.info("Token is null");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            Claims claims = JwtUtils.parseToken(token);
            Integer empId = claims.get("id", Integer.class);
            String name = claims.get("name", String.class);
            if (name == null) {
                name = claims.get("username", String.class);
            }
            UserContext.set(empId, name);
            log.info("Token is valid");
        } catch (Exception e) {
            log.info("Token is invalid");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 线程池会复用线程，请求结束后必须清理，避免把上一个请求的用户带到下一个请求
        UserContext.clear();
    }

}
