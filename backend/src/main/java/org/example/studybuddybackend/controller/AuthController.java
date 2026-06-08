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

    @GetMapping("/github")
    public ResponseEntity<Void> githubLogin() {
        String url = "https://github.com/login/oauth/authorize?client_id=" + githubClientId
                + "&redirect_uri=" + githubRedirectUri
                + "&scope=user:email";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

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
