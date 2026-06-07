package org.example.studybuddybackend.config;

import org.example.studybuddybackend.config.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // 【关键】让 Spring 自动注入 JwtInterceptor
    private final JwtInterceptor jwtInterceptor;

    // 跨域配置
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",   // 前端本地开发
                        "http://localhost:8080",   // 后端本地（前端同源调试用）
                        "https://banxue.vercel.app" // 前端生产环境
                )
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // 拦截器配置
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor) // 使用注入的拦截器
                .addPathPatterns("/api/**") // 拦截所有 /api/ 开头的请求
                .excludePathPatterns(
                        "/api/auth/register",  // 排除注册接口
                        "/api/auth/login",     // 排除登录接口
                        "/api/auth/github/callback"  // 排除 GitHub 回调接口
                );
    }
}