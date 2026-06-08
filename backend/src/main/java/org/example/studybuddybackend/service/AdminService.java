package org.example.studybuddybackend.service;

import java.util.List;
import java.util.Map;

public interface AdminService {
    List<Map<String, Object>> getAllUsers();
    Map<String, Object> updateUser(Long userId, String role, String status);
    void deleteUser(Long userId);
    List<Map<String, Object>> getAllRooms();
    Map<String, Object> settleRoom(Long roomId);
    void deleteRoom(Long roomId);
    List<Map<String, Object>> getAllPosts();
    void deletePost(Long postId);
}
