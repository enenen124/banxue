package org.example.studybuddybackend.service;

import java.util.Map;

public interface PostService {
    Map<String, Object> getPostList(int page, int limit);
    Map<String, Object> getPostDetail(Long id, Long currentUserId);
    Map<String, Object> createPost(Long userId, String content, String images);
    void deletePost(Long id, Long userId);
    Map<String, Object> addComment(Long postId, Long userId, String content);
    Map<String, Object> toggleLike(Long postId, Long userId);
}
