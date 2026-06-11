package org.example.studybuddybackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:https://api.siliconflow.cn/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:deepseek-ai/DeepSeek-V4-Pro}")
    private String aiModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final String SYSTEM_PROMPT =
            "你是「伴学AI」，一个专注于学习辅导的智能助手。你只能回答与学习相关的问题，包括但不限于：\n" +
            "- 学习方法和技巧\n" +
            "- 时间管理和计划制定\n" +
            "- 考试复习和备考策略\n" +
            "- 记忆方法和知识管理\n" +
            "- 学习效率提升\n" +
            "- 学习心理和压力调节\n" +
            "- 各学科学习方法（数学、编程、语言等）\n\n" +
            "如果用户问了与学习无关的问题（如娱乐、政治、暴力、色情等），请礼貌地拒绝并引导回学习话题。\n" +
            "回复要简洁、有条理、积极向上，使用适当的emoji让回答更生动。";

    public AiController() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== AI 聊天（SSE 流式） ====================

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, String> body, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2分钟超时

        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            userMessage = "你好";
        }

        final String message = userMessage;

        executor.execute(() -> {
            try {
                // 检查 API Key 是否配置
                if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-siliconflow-api-key")) {
                    sendFallbackReply(emitter, message);
                    return;
                }

                callSiliconFlowStreaming(emitter, message);

            } catch (Exception e) {
                log.error("AI chat error: ", e);
                try {
                    String errorJson = objectMapper.writeValueAsString(
                            Map.of("error", "AI服务暂时不可用，请稍后重试"));
                    emitter.send(SseEmitter.event().data(errorJson));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                } catch (Exception ex) {
                    log.error("SSE send error: ", ex);
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    // ==================== AI 学习分析 ====================

    @PostMapping("/analysis")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        if (body == null || body.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "缺少学习数据"));
        }

        // 构建分析提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下学生的学习数据，给出简洁、积极、有建设性的学习分析和建议（100字以内）：\n\n");

        if (body.containsKey("totalMinutes")) {
            prompt.append("- 累计学习总时长: ").append(body.get("totalMinutes")).append("分钟\n");
        }
        if (body.containsKey("totalHours")) {
            prompt.append("- 累计学习小时数: ").append(body.get("totalHours")).append("小时\n");
        }
        if (body.containsKey("roomCount")) {
            prompt.append("- 参与自习室次数: ").append(body.get("roomCount")).append("\n");
        }
        if (body.containsKey("qualifiedCount")) {
            prompt.append("- 自习室达标次数: ").append(body.get("qualifiedCount")).append("\n");
        }
        if (body.containsKey("unqualifiedCount")) {
            prompt.append("- 自习室未达标次数: ").append(body.get("unqualifiedCount")).append("\n");
        }
        if (body.containsKey("postCount")) {
            prompt.append("- 发布帖子数: ").append(body.get("postCount")).append("\n");
        }
        if (body.containsKey("daily")) {
            prompt.append("- 近7天每日学习数据: ").append(body.get("daily")).append("\n");
        }

        prompt.append("\n请给出鼓励性的分析和具体建议。");

        // 调用 AI API（非流式）
        try {
            if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-siliconflow-api-key")) {
                // API Key 未配置，使用模板生成
                return ResponseEntity.ok(Map.of("analysis", generateFallbackAnalysis(body)));
            }

            String analysis = callSiliconFlow(prompt.toString(), false);
            return ResponseEntity.ok(Map.of("analysis", analysis));

        } catch (Exception e) {
            log.error("AI analysis error: ", e);
            return ResponseEntity.ok(Map.of("analysis", generateFallbackAnalysis(body)));
        }
    }

    // ==================== 硅基流动 API 调用（流式） ====================

    private void callSiliconFlowStreaming(SseEmitter emitter, String userMessage) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiModel);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        requestBody.put("max_tokens", 2048);
        requestBody.put("temperature", 0.7);

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody), headers);

        // 发送请求获取流式响应（用 byte[] 避免 RestTemplate 默认 ISO-8859-1 乱码）
        ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, byte[].class);

        String responseBody = new String(response.getBody(), StandardCharsets.UTF_8);
        if (responseBody == null) {
            throw new RuntimeException("Empty response from SiliconFlow");
        }

        // 解析 SSE 流并逐行转发
        String[] lines = responseBody.split("\n");
        for (String line : lines) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                if (data.equals("[DONE]")) {
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    break;
                }
                try {
                    JsonNode node = objectMapper.readTree(data);
                    JsonNode choices = node.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        JsonNode delta = choices.get(0).path("delta");
                        JsonNode content = delta.path("content");
                        if (!content.isMissingNode() && !content.isNull()) {
                            Map<String, Object> chunk = new LinkedHashMap<>();
                            chunk.put("choices", List.of(
                                    Map.of("delta", Map.of("content", content.asText()))
                            ));
                            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(chunk)));
                        }
                    }
                } catch (Exception e) {
                    // 跳过无法解析的行
                }
            }
        }
        emitter.complete();
    }

    // ==================== 硅基流动 API 调用（非流式） ====================

    private String callSiliconFlow(String userMessage, boolean unused) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiModel);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 1024);
        requestBody.put("temperature", 0.7);

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody), headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, byte[].class);

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText();
        }

        throw new RuntimeException("No response from AI model");
    }

    // ==================== Fallback（API Key 未配置时） ====================

    private void sendFallbackReply(SseEmitter emitter, String message) throws Exception {
        String reply = generateKeywordReply(message);
        // 模拟流式逐字输出
        for (int i = 0; i < reply.length(); i++) {
            Map<String, Object> chunk = new LinkedHashMap<>();
            chunk.put("choices", List.of(
                    Map.of("delta", Map.of("content", String.valueOf(reply.charAt(i))))
            ));
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(chunk)));
            Thread.sleep(30); // 模拟打字效果
        }
        emitter.send(SseEmitter.event().data("[DONE]"));
        emitter.complete();
    }

    private String generateKeywordReply(String message) {
        if (message == null || message.isBlank()) {
            return "你好！我是伴学AI助手 🌟 可以回答你关于学习方法和时间管理的问题。";
        }
        String lower = message.toLowerCase();
        if (lower.contains("你是谁") || lower.contains("你叫什么") || lower.contains("你好") || lower.contains("hello") || lower.contains("hi")) {
            return "你好！我是「伴学AI」🤖 你的智能学习助手！我可以帮你解答关于学习方法、时间管理、考试技巧等方面的问题。有什么想问的尽管说~";
        }
        if (lower.contains("高效") || lower.contains("学习方法") || lower.contains("怎么学") || lower.contains("如何学") || lower.contains("怎样学")) {
            return "高效学习的方法推荐：\n1. 🍅 **番茄工作法**：25分钟专注 + 5分钟休息，每4个循环长休15分钟\n2. 📝 **主动回忆**：合上书本，尝试回忆刚才学的内容\n3. 🧠 **费曼学习法**：用自己的话把知识教给别人\n4. 🔁 **间隔重复**：在1天、3天、7天后复习\n5. 🎯 **目标拆分**：把大任务拆成可执行的小步骤\n6. 📵 **减少干扰**：手机静音、关闭通知，创造专注环境";
        }
        if (lower.contains("专注") || lower.contains("集中") || lower.contains("分心")) {
            return "提高专注力的方法：\n1. 🍅 番茄工作法：25分钟专注 + 5分钟休息\n2. 📱 手机静音并放到视线之外\n3. 🎯 每次只设定一个小目标\n4. 🎵 尝试白噪音或轻音乐\n5. 💧 保持充足水分，适当休息";
        }
        if (lower.contains("计划") || lower.contains("安排") || lower.contains("时间表")) {
            return "制定学习计划的建议：\n1. 📋 明确学习目标（SMART原则）\n2. 📅 按优先级排列任务\n3. ⏰ 为每个任务设定时间限制\n4. ✅ 每天结束时回顾完成情况\n5. 🔄 每周调整计划";
        }
        if (lower.contains("记忆") || lower.contains("背") || lower.contains("忘")) {
            return "高效记忆技巧：\n1. 🔁 间隔重复法\n2. 🧠 费曼学习法\n3. 🗺️ 思维导图\n4. ✍️ 主动回忆\n5. 🔗 联想记忆";
        }
        if (lower.contains("考试") || lower.contains("复习") || lower.contains("备考")) {
            return "考试复习建议：\n1. 📖 先复习重点和难点\n2. 📝 做历年真题找感觉\n3. 🗓️ 制定倒计时复习计划\n4. 👥 和同学组队互相考问\n5. 😴 考前保证充足睡眠";
        }
        if (lower.contains("数学") || lower.contains("高数") || lower.contains("编程") || lower.contains("代码") || lower.contains("英语") || lower.contains("单词")) {
            return "学科学习建议：\n1. 📖 **理解优先**：先搞懂概念和原理，再做题练习\n2. ✍️ **多动手**：数学多做题，编程多写代码，英语多读多说\n3. 🗂️ **错题整理**：建立错题本，定期回顾\n4. 👥 **讨论交流**：和同学讨论能加深理解\n5. 📚 **善用资源**：B站、MOOC等平台有很多优质课程";
        }
        if (lower.contains("压力") || lower.contains("焦虑") || lower.contains("紧张") || lower.contains("心态")) {
            return "学习压力调节建议：\n1. 🏃 适当运动，释放压力\n2. 🎵 听音乐放松\n3. 😴 保证充足睡眠\n4. 💬 和朋友家人倾诉\n5. 📝 写下烦恼，理清思路\n6. 🎯 降低期望，接受不完美";
        }
        return "感谢你的提问！💡 我擅长解答学习方法、时间管理、考试技巧、学科学习等方面的问题。你可以试试问我：\n- 如何高效学习？\n- 怎么提高专注力？\n- 考试复习有什么技巧？\n- 学习压力大怎么办？";
    }

    private String generateFallbackAnalysis(Map<String, Object> body) {
        int totalMinutes = 0;
        if (body.containsKey("totalMinutes")) {
            totalMinutes = ((Number) body.get("totalMinutes")).intValue();
        }
        int qualifiedCount = 0;
        if (body.containsKey("qualifiedCount")) {
            qualifiedCount = ((Number) body.get("qualifiedCount")).intValue();
        }
        int postCount = 0;
        if (body.containsKey("postCount")) {
            postCount = ((Number) body.get("postCount")).intValue();
        }

        if (totalMinutes < 60) {
            return "你才刚刚开始学习之旅，建议每天至少专注30分钟，养成良好习惯。试试使用番茄工作法：25分钟专注 + 5分钟休息，逐步建立学习节奏。";
        } else if (totalMinutes < 300) {
            return "学习势头不错！你已经积累了" + totalMinutes + "分钟的学习时间。建议制定每周学习计划，保持稳定的学习节奏，争取突破5小时大关。";
        } else if (totalMinutes < 600) {
            return "太棒了！累计" + totalMinutes + "分钟的学习时间说明你已经是学习达人了。建议尝试主题学习法，围绕一个主题深入学习，效果会更好。";
        } else {
            return "你是超级学霸！累计" + totalMinutes + "分钟的学习时间非常优秀。" +
                    (postCount > 0 ? "你还积极分享了" + postCount + "篇帖子，继续保持！" : "") +
                    "建议适当休息，保持身心健康，学习效率会更高。";
        }
    }
}
