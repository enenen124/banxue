package org.example.studybuddybackend.config;

import org.example.studybuddybackend.entity.Achievement;
import org.example.studybuddybackend.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AchievementRepository achievementRepository;

    @Override
    public void run(String... args) {
        initAchievement("first_focus", "初来乍到", "完成第1次专注");
        initAchievement("focus_10h", "学习达人", "累计学习10小时");
        initAchievement("points_100", "百炼成钢", "累计获得100积分");
        initAchievement("first_post", "笔耕不辍", "发布第1篇帖子");
        initAchievement("likes_10", "人见人爱", "获得10个赞");
    }

    private void initAchievement(String code, String name, String description) {
        if (!achievementRepository.findByCode(code).isPresent()) {
            Achievement achievement = new Achievement();
            achievement.setCode(code);
            achievement.setName(name);
            achievement.setDescription(description);
            achievementRepository.save(achievement);
        }
    }
}
