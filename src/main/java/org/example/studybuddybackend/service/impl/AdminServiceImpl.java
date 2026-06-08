package org.example.studybuddybackend.service.impl;

import org.example.studybuddybackend.entity.*;
import org.example.studybuddybackend.repository.*;
import org.example.studybuddybackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final StudyRoomRepository roomRepository;
    private final StudyRoomMemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final FavoriteRepository favoriteRepository;

    @Override
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("nickname", user.getNickname());
                    map.put("avatar", user.getAvatar());
                    map.put("points", user.getPoints());
                    map.put("role", user.getRole());
                    map.put("createdAt", user.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> updateUser(Long userId, String role, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (role != null) user.setRole(role);
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "更新成功");
        result.put("id", user.getId());
        result.put("role", user.getRole());
        return result;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 删除用户相关数据
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Post post : posts) {
            commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).forEach(commentRepository::delete);
            postLikeRepository.findByPostIdAndUserId(post.getId(), userId).ifPresent(postLikeRepository::delete);
            favoriteRepository.findByUserIdAndPostId(userId, post.getId()).ifPresent(favoriteRepository::delete);
            postRepository.delete(post);
        }

        userRepository.delete(user);
    }

    @Override
    public List<Map<String, Object>> getAllRooms() {
        return roomRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(room -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", room.getId());
                    map.put("name", room.getName());
                    map.put("targetMinutes", room.getTargetMinutes());
                    map.put("maxMembers", room.getMaxMembers());
                    map.put("minStake", room.getMinStake());
                    map.put("status", room.getStatus());
                    map.put("createdAt", room.getCreatedAt());
                    map.put("endAt", room.getEndAt());

                    User creator = userRepository.findById(room.getCreatorId()).orElse(null);
                    Map<String, Object> creatorMap = new LinkedHashMap<>();
                    if (creator != null) {
                        creatorMap.put("id", creator.getId());
                        creatorMap.put("nickname", creator.getNickname());
                    }
                    map.put("creator", creatorMap);

                    List<StudyRoomMember> members = memberRepository.findByRoomId(room.getId());
                    map.put("memberCount", members.size());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> settleRoom(Long roomId) {
        StudyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("自习室不存在"));

        if ("已关闭".equals(room.getStatus())) {
            throw new RuntimeException("自习室已经结算过了");
        }

        List<StudyRoomMember> members = memberRepository.findByRoomId(room.getId());
        int qualifiedCount = 0;
        int unqualifiedCount = 0;
        int poolStake = 0;

        for (StudyRoomMember m : members) {
            if (m.getStudyMinutes() >= room.getTargetMinutes()) {
                m.setQualified(true);
                qualifiedCount++;
            } else {
                m.setQualified(false);
                unqualifiedCount++;
                poolStake += m.getStakeAmount();
            }
        }

        int bonusPerPerson = qualifiedCount > 0 ? poolStake / qualifiedCount : 0;
        for (StudyRoomMember m : members) {
            if (Boolean.TRUE.equals(m.getQualified())) {
                m.setBonus(bonusPerPerson);
                User u = userRepository.findById(m.getUserId()).orElse(null);
                if (u != null) {
                    u.setPoints(u.getPoints() + m.getStakeAmount() + bonusPerPerson);
                    userRepository.save(u);
                }
            }
            memberRepository.save(m);
        }

        room.setStatus("已关闭");
        roomRepository.save(room);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "结算完成");
        result.put("qualifiedCount", qualifiedCount);
        result.put("unqualifiedCount", unqualifiedCount);
        result.put("poolStake", poolStake);
        return result;
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {
        StudyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("自习室不存在"));

        // 退还所有成员押金
        List<StudyRoomMember> members = memberRepository.findByRoomId(roomId);
        for (StudyRoomMember m : members) {
            User u = userRepository.findById(m.getUserId()).orElse(null);
            if (u != null) {
                u.setPoints(u.getPoints() + m.getStakeAmount());
                userRepository.save(u);
            }
            memberRepository.delete(m);
        }

        roomRepository.delete(room);
    }

    @Override
    public List<Map<String, Object>> getAllPosts() {
        return postRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(post -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", post.getId());
                    map.put("content", post.getContent());
                    map.put("images", post.getImages());
                    map.put("likeCount", post.getLikeCount());
                    map.put("commentCount", post.getCommentCount());
                    map.put("createdAt", post.getCreatedAt());

                    User user = userRepository.findById(post.getUserId()).orElse(null);
                    Map<String, Object> authorMap = new LinkedHashMap<>();
                    if (user != null) {
                        authorMap.put("id", user.getId());
                        authorMap.put("nickname", user.getNickname());
                    }
                    map.put("author", authorMap);
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        // 删除相关评论和点赞
        commentRepository.findByPostIdOrderByCreatedAtAsc(postId).forEach(commentRepository::delete);
        postRepository.delete(post);
    }
}
