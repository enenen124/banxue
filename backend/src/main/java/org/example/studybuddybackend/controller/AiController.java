package org.example.studybuddybackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.studybuddybackend.repository.StudyRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final StudyRecordRepository studyRecordRepository;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userMessage = body.get("message");

        // 简单的模拟 AI 回复
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", generateAiReply(userMessage));
        result.put("role", "assistant");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analysis")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 基于用户学习数据生成分析
        Integer totalMinutes = studyRecordRepository.sumMinutesByUserId(userId);
        if (totalMinutes == null) totalMinutes = 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMinutes", totalMinutes);
        result.put("totalHours", String.format("%.1f", totalMinutes / 60.0));

        // 生成学习建议
        String suggestion;
        if (totalMinutes < 60) {
            suggestion = "你才刚刚开始学习之旅，建议每天至少专注30分钟，养成良好习惯。试试使用番茄工作法：25分钟专注 + 5分钟休息。";
        } else if (totalMinutes < 300) {
            suggestion = "学习势头不错！你已经积累了" + totalMinutes + "分钟的学习时间。建议制定每周学习计划，保持稳定的学习节奏。";
        } else if (totalMinutes < 600) {
            suggestion = "太棒了！你已经是学习达人了（" + totalMinutes + "分钟）。建议尝试主题学习法，围绕一个主题深入学习，效果会更好。";
        } else {
            suggestion = "你是超级学霸！累计" + totalMinutes + "分钟的学习时间非常优秀。建议适当休息，保持身心健康，学习效率会更高。";
        }

        result.put("suggestion", suggestion);
        result.put("level", getLevel(totalMinutes));

        return ResponseEntity.ok(result);
    }

    private String generateAiReply(String message) {
        if (message == null || message.isEmpty()) {
            return "你好！我是伴学AI助手 🌟 可以回答你关于学习方法和时间管理的问题。试试问我：\n• 如何提高专注力？\n• 怎么制定学习计划？\n• 有什么记忆技巧？\n• 考试怎么复习？";
        }

        String lower = message.toLowerCase();

        // 身份和打招呼
        if (lower.contains("你是谁") || lower.contains("你叫什么") || lower.contains("介绍一下自己") || lower.contains("你好") || lower.contains("hello") || lower.contains("hi")) {
            return "你好！我是「伴学AI」🤖 你的智能学习助手！我可以帮你解答关于学习方法、时间管理、考试技巧等方面的问题，还能根据你的学习数据给出个性化建议。有什么想问的尽管说~";
        }

        if (lower.contains("专注") || lower.contains("集中") || lower.contains("分心") || lower.contains("走神")) {
            return "提高专注力的方法：\n1. 🍅 番茄工作法：25分钟专注 + 5分钟休息\n2. 📱 手机静音并放到视线之外\n3. 🎯 每次只设定一个小目标\n4. 🎵 尝试白噪音或轻音乐\n5. 💧 保持充足水分，适当休息";
        } else if (lower.contains("计划") || lower.contains("安排") || lower.contains("时间表")) {
            return "制定学习计划的建议：\n1. 📋 明确学习目标（SMART原则）\n2. 📅 按优先级排列任务\n3. ⏰ 为每个任务设定时间限制\n4. ✅ 每天结束时回顾完成情况\n5. 🔄 每周调整计划";
        } else if (lower.contains("记忆") || lower.contains("背") || lower.contains("忘")) {
            return "高效记忆技巧：\n1. 🔁 间隔重复法：在遗忘前复习\n2. 🧠 费曼学习法：用自己的话讲给别人听\n3. 🗺️ 思维导图：建立知识间的联系\n4. ✍️ 主动回忆：合上书本自己默写\n5. 🔗 联想记忆：将新知识和已知关联";
        } else if (lower.contains("考试") || lower.contains("复习") || lower.contains("备考")) {
            return "考试复习建议：\n1. 📖 先复习重点和难点\n2. 📝 做历年真题找感觉\n3. 🗓️ 制定倒计时复习计划\n4. 👥 和同学组队互相考问\n5. 😴 考前保证充足睡眠";
        } else if (lower.contains("效率") || lower.contains("高效") || lower.contains("学习方法")) {
            return "提高学习效率的方法：\n1. 🎯 明确学习目标，知道为什么而学\n2. 📚 主动学习 > 被动学习（做题 > 看书）\n3. 🧩 分块学习：把大任务拆成小块\n4. 🔄 定期复习：当天复习 + 每周回顾\n5. 💪 保持运动：运动能提升大脑功能";
        } else if (lower.contains("压力") || lower.contains("焦虑") || lower.contains("心态")) {
            return "缓解学习压力的建议：\n1. 🧘 深呼吸和冥想，每天5分钟\n2. 🏃 适当运动，释放内咖肽\n3. 📝 写下焦虑的事情，理清思路\n4. 🎵 听音乐放松\n5. 💬 和朋友或家人聊聊";
        } else if (lower.contains("谢谢") || lower.contains("感谢") || lower.contains("thank")) {
            return "不客气！😊 有任何学习问题随时可以问我，祝你学习顺利！";
        } else {
            return "感谢你的提问！💡 作为伴学AI助手，我擅长解答以下方面的问题：\n• 📖 学习方法和效率提升\n• ⏰ 时间管理和计划制定\n• 🧠 记忆技巧和复习策略\n• 📝 考试备考和心态调节\n\n你可以试着换个关键词问我，或者使用「学习分析」功能查看你的学习数据和建议！";
        }
    }

    private String getLevel(int totalMinutes) {
        if (totalMinutes < 60) return "新手上路";
        if (totalMinutes < 300) return "学习入门";
        if (totalMinutes < 600) return "学习达人";
        if (totalMinutes < 1200) return "学霸进阶";
        return "学神之巅";
    }
}
