package org.example.studybuddybackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.studybuddybackend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.updateProfile(userId, body.get("nickname"), body.get("avatar")));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword")));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.getStats(userId));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<Map<String, Object>>> getAchievements(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.getAchievements(userId));
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<Map<String, Object>>> getFavorites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.getFavorites(userId));
    }

    @PostMapping("/favorites/{postId}")
    public ResponseEntity<Map<String, Object>> addFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.addFavorite(userId, postId));
    }

    @DeleteMapping("/favorites/{postId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.removeFavorite(userId, postId));
    }

    @GetMapping("/posts")
    public ResponseEntity<List<Map<String, Object>>> getUserPosts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.getUserPosts(userId));
    }

    @GetMapping("/likes")
    public ResponseEntity<List<Map<String, Object>>> getUserLikes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(userService.getUserLikes(userId));
    }
}
