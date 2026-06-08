package org.example.studybuddybackend.service.impl;

import org.example.studybuddybackend.entity.*;
import org.example.studybuddybackend.repository.*;
import org.example.studybuddybackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final PostRepository postRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final StudyRoomMemberRepository studyRoomMemberRepository;
    private final PostLikeRepository postLikeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final Map<String, String[]> ACHIEVEMENTS = new LinkedHashMap<>();
    static {
        ACHIEVEMENTS.put("first_focus", new String[]{"初来乍到", "完成第1次专注"});
        ACHIEVEMENTS.put("focus_10h", new String[]{"学习达人", "累计学习10小时"});
        ACHIEVEMENTS.put("points_100", new String[]{"百炼成钢", "累计获得100积分"});
        ACHIEVEMENTS.put("first_post", new String[]{"笔耕不辍", "发布第1篇帖子"});
        ACHIEVEMENTS.put("likes_10", new String[]{"人见人爱", "获得10个赞"});
    }

    @Override
    public Map<String, Object> getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<Long> favoritePostIds = favoriteRepository.findByUserId(userId).stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());

        List<String> unlockedAchievements = userAchievementRepository.findByUserId(userId).stream()
                .map(UserAchievement::getAchievementCode)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("points", user.getPoints());
        result.put("favorites", favoritePostIds);
        result.put("achievements", unlockedAchievements);
        result.put("createdAt", user.getCreatedAt());
        return result;
    }

    @Override
    public Map<String, Object> updateProfile(Long userId, String nickname, String avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        userRepository.save(user);

        return getProfile(userId);
    }

    @Override
    public Map<String, String> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Map.of("message", "密码修改成功");
    }

    @Override
    public Map<String, Object> getStats(Long userId) {
        Integer totalMinutes = studyRecordRepository.sumMinutesByUserId(userId);

        List<Map<String, Object>> dailyRecords = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<StudyRecord> records = studyRecordRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
            int dayMinutes = records.stream().mapToInt(StudyRecord::getMinutes).sum();

            Map<String, Object> dayRecord = new LinkedHashMap<>();
            dayRecord.put("date", date.toString());
            dayRecord.put("minutes", dayMinutes);
            dailyRecords.add(dayRecord);
        }

        long roomCount = studyRoomMemberRepository.countByUserId(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMinutes", totalMinutes != null ? totalMinutes : 0);
        result.put("roomCount", roomCount);
        result.put("dailyRecords", dailyRecords);
        return result;
    }

    @Override
    public List<Map<String, Object>> getAchievements(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : ACHIEVEMENTS.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", entry.getKey());
            item.put("name", entry.getValue()[0]);
            item.put("desc", entry.getValue()[1]);
            item.put("unlocked", userAchievementRepository.existsByUserIdAndAchievementCode(userId, entry.getKey()));
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(fav -> {
                    Post post = postRepository.findById(fav.getPostId()).orElse(null);
                    if (post == null) return null;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", post.getId());
                    map.put("content", post.getContent());
                    map.put("images", post.getImages());
                    map.put("likeCount", post.getLikeCount());
                    map.put("commentCount", post.getCommentCount());
                    map.put("createdAt", post.getCreatedAt());
                    return map;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> addFavorite(Long userId, Long postId) {
        if (favoriteRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new RuntimeException("已经收藏过了");
        }

        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setPostId(postId);
        favoriteRepository.save(fav);

        return Map.of("message", "收藏成功");
    }

    @Override
    @Transactional
    public Map<String, Object> removeFavorite(Long userId, Long postId) {
        Favorite fav = favoriteRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new RuntimeException("未收藏该帖子"));
        favoriteRepository.delete(fav);

        return Map.of("message", "取消收藏成功");
    }

    @Override
    public List<Map<String, Object>> getUserPosts(Long userId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return posts.stream()
                .map(post -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", post.getId());
                    map.put("content", post.getContent());
                    map.put("images", post.getImages());
                    map.put("likeCount", post.getLikeCount());
                    map.put("commentCount", post.getCommentCount());
                    map.put("createdAt", post.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getUserLikes(Long userId) {
        List<PostLike> likes = postLikeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return likes.stream()
                .map(like -> {
                    Post post = postRepository.findById(like.getPostId()).orElse(null);
                    if (post == null) return null;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", post.getId());
                    map.put("content", post.getContent());
                    map.put("images", post.getImages());
                    map.put("likeCount", post.getLikeCount());
                    map.put("commentCount", post.getCommentCount());
                    map.put("createdAt", post.getCreatedAt());
                    return map;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
