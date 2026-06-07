package org.example.studybuddybackend.service;

import java.util.List;
import java.util.Map;

public interface UserService {
    Map<String, Object> getProfile(Long userId);
    Map<String, Object> updateProfile(Long userId, String nickname, String avatar);
    Map<String, String> changePassword(Long userId, String oldPassword, String newPassword);
    Map<String, Object> getStats(Long userId);
    List<Map<String, Object>> getAchievements(Long userId);
    List<Map<String, Object>> getFavorites(Long userId);
    Map<String, Object> addFavorite(Long userId, Long postId);
    Map<String, Object> removeFavorite(Long userId, Long postId);
}
