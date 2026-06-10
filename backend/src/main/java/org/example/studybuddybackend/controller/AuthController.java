package org.example.studybuddybackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.studybuddybackend.dto.request.AuthRegisterRequest;
import org.example.studybuddybackend.dto.response.AuthRegisterResponse;
import org.example.studybuddybackend.entity.User;
import org.example.studybuddybackend.repository.UserRepository;
import org.example.studybuddybackend.service.AuthService;
import org.example.studybuddybackend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${github.client.id}")
    private String githubClientId;

    @Value("${github.client.secret}")
    private String githubClientSecret;

    @Value("${github.redirect.uri}")
    private String githubRedirectUri;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<AuthRegisterResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRegisterResponse> login(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    /**
     * 检查 GitHub OAuth 凭证是否已配置
     */
    private boolean isGithubConfigured() {
        return githubClientId != null
                && !githubClientId.isBlank()
                && !githubClientId.equals("your-github-client-id");
    }

    /**
     * GitHub 登录入口
     * - 有真实凭证：跳转 GitHub 授权页（OAuth 流程）
     * - 无凭证（开发模式）：直接模拟 GitHub 用户登录
     */
    @GetMapping("/github")
    public ResponseEntity<?> githubLogin() {
        if (!isGithubConfigured()) {
            // 开发模式：模拟 GitHub 登录，直接返回 mock 用户 + JWT
            log.info("GitHub OAuth 未配置，使用模拟登录模式");
            return handleMockGithubLogin();
        }

        // 正式模式：跳转 GitHub 授权页
        String url = "https://github.com/login/oauth/authorize?client_id=" + githubClientId
                + "&redirect_uri=" + githubRedirectUri
                + "&scope=user:email";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 模拟 GitHub 登录（开发/演示模式）
     * 创建或查找一个模拟的 GitHub 用户，返回 JWT
     */
    private ResponseEntity<?> handleMockGithubLogin() {
        String mockGithubId = "mock_github_12345";
        String mockUsername = "github_demo";
        String mockNickname = "GitHub演示用户";
        String mockAvatar = "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png";

        User user = userRepository.findByGithubId(mockGithubId).orElse(null);
        if (user == null) {
            user = new User();
            user.setUsername(mockUsername);
            user.setNickname(mockNickname);
            user.setGithubId(mockGithubId);
            user.setAvatar(mockAvatar);
            user.setPassword("");
            user.setPoints(100);
            user = userRepository.save(user);
            log.info("创建模拟GitHub用户: {}", mockUsername);
        }

        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "avatar", user.getAvatar(),
                "points", user.getPoints(),
                "token", token
        ));
    }

    /**
     * GitHub OAuth 回调（正式模式）
     */
    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam String code) {
        try {
            // 1. 用 code 换 access_token
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_JSON);
            tokenHeaders.set("Accept", "application/json");

            String tokenBody = String.format(
                    "{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"code\":\"%s\"}",
                    githubClientId, githubClientSecret, code);

            HttpEntity<String> tokenRequest = new HttpEntity<>(tokenBody, tokenHeaders);
            ResponseEntity<String> tokenResponse = restTemplate.exchange(
                    "https://github.com/login/oauth/access_token",
                    HttpMethod.POST, tokenRequest, String.class);

            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
            String accessToken = tokenJson.get("access_token").asText();

            // 2. 用 access_token 获取用户信息
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<String> userResponse = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET, userRequest, String.class);

            JsonNode userJson = objectMapper.readTree(userResponse.getBody());
            String githubId = userJson.get("id").asText();
            String login = userJson.has("login") ? userJson.get("login").asText() : githubId;
            String avatarUrl = userJson.has("avatar_url") ? userJson.get("avatar_url").asText() : "";

            // 3. 查找或创建用户
            User user = userRepository.findByGithubId(githubId).orElse(null);
            if (user == null) {
                // 检查用户名是否已存在，如存在则加后缀
                String username = login;
                int suffix = 1;
                while (userRepository.existsByUsername(username)) {
                    username = login + "_" + suffix++;
                }
                user = new User();
                user.setUsername(username);
                user.setNickname(login);
                user.setGithubId(githubId);
                user.setAvatar(avatarUrl);
                user.setPassword("");  // GitHub 用户无密码
                user.setPoints(100);
                userRepository.save(user);
            }

            // 4. 生成 JWT 并重定向到前端
            String token = jwtUtil.generateToken(user);
            String redirectUrl = frontendUrl + "/login?token=" + token;

            HttpHeaders redirectHeaders = new HttpHeaders();
            redirectHeaders.setLocation(URI.create(redirectUrl));
            return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);

        } catch (Exception e) {
            String errorUrl = frontendUrl + "/login?error=github_auth_failed";
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setLocation(URI.create(errorUrl));
            return new ResponseEntity<>(errorHeaders, HttpStatus.FOUND);
        }
    }
}
