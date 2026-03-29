package com.example.trendytoysocialecommercehd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 设置允许的源（开发环境）
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",           // 本地开发
                "http://127.0.0.1:*",          // 本地IP
                "http://localhost:8080",       // 前端端口
                "http://localhost:3000",       // 前端端口
                "http://localhost:8081"        // 后端端口
        ));

        // 允许的请求方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept",
                "Authorization", "X-Requested-With",
                "Access-Control-Allow-Origin", "Access-Control-Allow-Headers",
                "Cache-Control"
        ));

        // 允许暴露的响应头
        config.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type",
                "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
        ));

        // 允许携带凭证（cookies、认证头等）
        config.setAllowCredentials(true);

        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}