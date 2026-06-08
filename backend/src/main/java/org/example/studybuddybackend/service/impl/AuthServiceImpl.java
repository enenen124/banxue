package org.example.studybuddybackend.service.impl;

import org.example.studybuddybackend.dto.request.AuthRegisterRequest;
import org.example.studybuddybackend.dto.response.AuthRegisterResponse;
import org.example.studybuddybackend.entity.User;
import org.example.studybuddybackend.repository.UserRepository;
import org.example.studybuddybackend.service.AuthService;
import org.example.studybuddybackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthRegisterResponse register(AuthRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setAvatar("");
        user.setPoints(100);

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser);
        return buildAuthResponse(savedUser, token);
    }

    @Override
    public AuthRegisterResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse(user, token);
    }

    private AuthRegisterResponse buildAuthResponse(User user, String token) {
        AuthRegisterResponse response = new AuthRegisterResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setPoints(user.getPoints());
        response.setToken(token);
        return response;
    }
}
