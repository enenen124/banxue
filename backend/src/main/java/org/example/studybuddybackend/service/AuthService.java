package org.example.studybuddybackend.service;

import org.example.studybuddybackend.dto.request.AuthRegisterRequest;
import org.example.studybuddybackend.dto.response.AuthRegisterResponse;

public interface AuthService {
    AuthRegisterResponse register(AuthRegisterRequest request);
    AuthRegisterResponse login(String username, String password);
}

