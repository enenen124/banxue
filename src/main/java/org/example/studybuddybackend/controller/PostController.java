package org.example.studybuddybackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.studybuddybackend.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPostList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(postService.getPostList(page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPostDetail(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(postService.getPostDetail(id, userId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String content = (String) body.get("content");
        String images = body.containsKey("images") ? body.get("images").toString() : "[]";
        // Convert images list to JSON string
        if (body.containsKey("images")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                images = om.writeValueAsString(body.get("images"));
            } catch (Exception e) {
                images = "[]";
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(userId, content, images));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        postService.deletePost(id, userId);
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.addComment(id, userId, body.get("content")));
    }

    @PutMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(postService.toggleLike(id, userId));
    }

    // 前端使用 POST 方法点赞，兼容处理
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLikePost(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(postService.toggleLike(id, userId));
    }
}
