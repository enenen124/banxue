package org.example.studybuddybackend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.studybuddybackend.entity.*;
import org.example.studybuddybackend.repository.*;
import org.example.studybuddybackend.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getPostList(int page, int limit) {
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page - 1, limit));

        List<Map<String, Object>> posts = postPage.getContent().stream()
                .map(this::buildPostSummary)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("posts", posts);
        result.put("total", postPage.getTotalElements());
        result.put("page", page);
        result.put("pages", postPage.getTotalPages());
        return result;
    }

    @Override
    public Map<String, Object> getPostDetail(Long id, Long currentUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        Map<String, Object> result = buildPostDetail(post);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createPost(Long userId, String content, String images) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setImages(images != null ? images : "[]");
        post.setLikeCount(0);
        post.setCommentCount(0);

        Post saved = postRepository.save(post);
        return buildPostDetail(saved);
    }

    @Override
    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此帖子");
        }
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public Map<String, Object> addComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        List<Map<String, Object>> commentList = comments.stream()
                .map(this::buildCommentMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("comments", commentList);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
        boolean isLiked;

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            isLiked = false;
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            isLiked = true;
        }

        postRepository.save(post);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("likes", post.getLikeCount());
        result.put("isLiked", isLiked);
        return result;
    }

    private Map<String, Object> buildPostSummary(Post post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("content", post.getContent());
        map.put("images", parseImages(post.getImages()));
        map.put("author", buildAuthorInfo(post.getUserId()));
        map.put("commentCount", post.getCommentCount());
        map.put("likeCount", post.getLikeCount());
        map.put("createdAt", post.getCreatedAt());
        return map;
    }

    private Map<String, Object> buildPostDetail(Post post) {
        Map<String, Object> map = buildPostSummary(post);

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());
        List<Map<String, Object>> commentList = comments.stream()
                .map(this::buildCommentMap)
                .collect(Collectors.toList());
        map.put("comments", commentList);

        List<PostLike> likes = post.getLikes();
        List<Map<String, Object>> likeList = new ArrayList<>();
        if (likes != null) {
            likeList = likes.stream().map(like -> {
                User u = userRepository.findById(like.getUserId()).orElse(null);
                Map<String, Object> m = new LinkedHashMap<>();
                if (u != null) {
                    m.put("id", u.getId());
                    m.put("nickname", u.getNickname());
                    m.put("avatar", u.getAvatar());
                }
                return m;
            }).collect(Collectors.toList());
        }
        map.put("likes", likeList);

        return map;
    }

    private Map<String, Object> buildCommentMap(Comment comment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", comment.getId());
        map.put("content", comment.getContent());
        map.put("author", buildAuthorInfo(comment.getUserId()));
        map.put("createdAt", comment.getCreatedAt());
        return map;
    }

    private Map<String, Object> buildAuthorInfo(Long userId) {
        Map<String, Object> author = new LinkedHashMap<>();
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            author.put("id", user.getId());
            author.put("nickname", user.getNickname());
            author.put("avatar", user.getAvatar());
        }
        return author;
    }

    private List<String> parseImages(String imagesJson) {
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
