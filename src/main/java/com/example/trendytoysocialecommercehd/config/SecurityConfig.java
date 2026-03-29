package com.example.trendytoysocialecommercehd.config;

import com.example.trendytoysocialecommercehd.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        // 暂时允许所有API请求通过（仅用于测试）
                        .anyRequest().permitAll()
                );
        // 生产环境请使用以下配置，并添加JWT过滤器
        // .authorizeHttpRequests(authorize -> authorize
        //     // 允许登录和注册请求
        //     .requestMatchers("/api/user/login", "/api/user/register", "/api/user/refresh-token").permitAll()
        //     // 允许静态资源
        //     .requestMatchers("/static/**").permitAll()
        //     // 允许图片资源
        //     .requestMatchers("/images/**").permitAll()
        //     // 允许 Swagger UI 和相关资源
        //     .requestMatchers(
        //         "/swagger-ui/**",
        //         "/swagger-ui.html",
        //         "/v3/api-docs/**",
        //         "/swagger-resources/**",
        //         "/webjars/**"
        //     ).permitAll()
        //     // 其他请求需要认证
        //     .anyRequest().authenticated()
        // )
        // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}