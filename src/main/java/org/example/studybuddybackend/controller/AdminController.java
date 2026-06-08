package org.example.studybuddybackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.studybuddybackend.entity.User;
import org.example.studybuddybackend.repository.UserRepository;
import org.example.studybuddybackend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final UserRepository userRepository;

    private void checkAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("未授权");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"admin".equals(user.getRole())) {
            throw new RuntimeException("无权限");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(HttpServletRequest request) {
        checkAdmin(request);
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        checkAdmin(request);
        return ResponseEntity.ok(adminService.updateUser(id, body.get("role"), body.get("status")));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            HttpServletRequest request) {
        checkAdmin(request);
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getAllRooms(HttpServletRequest request) {
        checkAdmin(request);
        return ResponseEntity.ok(adminService.getAllRooms());
    }

    @PostMapping("/rooms/{id}/settle")
    public ResponseEntity<Map<String, Object>> settleRoom(
            @PathVariable Long id,
            HttpServletRequest request) {
        checkAdmin(request);
        return ResponseEntity.ok(adminService.settleRoom(id));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Map<String, String>> deleteRoom(
            @PathVariable Long id,
            HttpServletRequest request) {
        checkAdmin(request);
        adminService.deleteRoom(id);
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }

    @GetMapping("/posts")
    public ResponseEntity<List<Map<String, Object>>> getAllPosts(HttpServletRequest request) {
        checkAdmin(request);
        return ResponseEntity.ok(adminService.getAllPosts());
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable Long id,
            HttpServletRequest request) {
        checkAdmin(request);
        adminService.deletePost(id);
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }
}
