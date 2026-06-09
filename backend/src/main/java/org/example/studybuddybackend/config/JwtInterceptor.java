package org.example.studybuddybackend.config;

import org.example.studybuddybackend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestURI = request.getRequestURI();

        // 放行公开接口
        if (requestURI.contains("/auth/register") || 
            requestURI.contains("/auth/login") ||
            requestURI.contains("/auth/github")) {
            return true;
        }

        // 放行帖子列表和详情的GET请求（用 startsWith 而非 matches，避免查询参数干扰）
        if ("GET".equalsIgnoreCase(request.getMethod()) && requestURI.startsWith("/api/posts")) {
            tryParseToken(request);
            return true;
        }

        // 放行自习室列表的GET请求
        if ("GET".equalsIgnoreCase(request.getMethod()) && requestURI.startsWith("/api/rooms")) {
            tryParseToken(request);
            return true;
        }

        // 放行用户公开信息的GET请求（/api/users/profile, /api/users/{id}）
        if ("GET".equalsIgnoreCase(request.getMethod()) && 
            (requestURI.startsWith("/api/users/profile") || requestURI.matches("/api/users/\\d+"))) {
            tryParseToken(request);
            return true;
        }

        // 放行成就/NFT查询的GET请求
        if ("GET".equalsIgnoreCase(request.getMethod()) && requestURI.startsWith("/api/achievements/")) {
            tryParseToken(request);
            return true;
        }

        // 放行静态资源（上传文件）
        if (requestURI.startsWith("/uploads/")) {
            return true;
        }

        // 验证 JWT Token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                request.setAttribute("userId", userId);
                return true;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"未授权\"}");
        return false;
    }

    /**
     * 尝试解析token（如果有的话），但不强制要求
     */
    private void tryParseToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                request.setAttribute("userId", userId);
            }
        }
    }
}
