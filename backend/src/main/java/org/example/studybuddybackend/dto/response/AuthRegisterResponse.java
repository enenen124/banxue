package org.example.studybuddybackend.dto.response;

import lombok.Data;

@Data
public class AuthRegisterResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer points;
    private String role;
    private String token;
}
