package com.example.server.interceptor;

import com.example.common.properties.JwtProperties;
import com.example.common.utils.JwtUtil;
import com.example.common.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class UserInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String token = request.getHeader(jwtProperties.getTokenName());

            ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
            if (ops.get(token) == null) {
                throw new RuntimeException();
            }
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            ThreadLocalUtil.set(claims);

            // 刷新token有效期
            stringRedisTemplate.expire(token, jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);

            return true;
        }catch (Exception e){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
