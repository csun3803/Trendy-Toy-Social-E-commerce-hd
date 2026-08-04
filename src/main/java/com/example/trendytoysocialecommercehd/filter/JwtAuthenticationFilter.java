package com.example.trendytoysocialecommercehd.filter;

import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.service.UserService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Lazy
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String userId = jwtUtil.getUserIdFromToken(token);

                // 只检查普通用户（User表）的状态，商家管理员和平台管理员不受此限制
                try {
                    User user = userService.getUserById(userId);
                    if (user != null && "banned".equals(user.getAccountStatus())) {
                        // 用户已被禁用，返回401
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        Map<String, Object> result = new HashMap<>();
                        result.put("code", 401);
                        result.put("message", "账号已被禁用");
                        result.put("data", null);
                        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
                        return;
                    }
                } catch (Exception e) {
                    // 查询失败（可能不是普通用户），继续执行
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
            }
        } else {
        }

        filterChain.doFilter(request, response);
    }
}