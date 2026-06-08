package org.example.studybuddybackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.studybuddybackend.entity.MintRecord;
import org.example.studybuddybackend.repository.AchievementRepository;
import org.example.studybuddybackend.repository.MintRecordRepository;
import org.example.studybuddybackend.repository.UserAchievementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {
    private final MintRecordRepository mintRecordRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AchievementRepository achievementRepository;

    @PostMapping("/mint")
    public ResponseEntity<Map<String, Object>> mintAchievement(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String achievementCode = body.get("achievementCode");

        if (achievementCode == null || achievementCode.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "成就代码不能为空"));
        }

        // 检查成就是否已解锁
        if (!userAchievementRepository.existsByUserIdAndAchievementCode(userId, achievementCode)) {
            return ResponseEntity.badRequest().body(Map.of("message", "你还没有解锁该成就"));
        }

        // 检查是否已铸造
        if (mintRecordRepository.existsByUserIdAndAchievementCode(userId, achievementCode)) {
            return ResponseEntity.badRequest().body(Map.of("message", "该成就已经铸造过了"));
        }

        // 模拟 NFT 铸造
        MintRecord record = new MintRecord();
        record.setUserId(userId);
        record.setAchievementCode(achievementCode);
        record.setTokenId("NFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        record.setTxHash("0x" + UUID.randomUUID().toString().replace("-", ""));
        record.setStatus("minted");
        mintRecordRepository.save(record);

        var achievement = achievementRepository.findByCode(achievementCode).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "NFT 铸造成功！");
        result.put("tokenId", record.getTokenId());
        result.put("txHash", record.getTxHash());
        result.put("achievementCode", achievementCode);
        result.put("achievementName", achievement != null ? achievement.getName() : achievementCode);
        result.put("mintedAt", record.getMintedAt());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/mint-records")
    public ResponseEntity<List<Map<String, Object>>> getMintRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        List<Map<String, Object>> records = mintRecordRepository.findByUserIdOrderByMintedAtDesc(userId)
                .stream()
                .map(record -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", record.getId());
                    map.put("achievementCode", record.getAchievementCode());
                    map.put("tokenId", record.getTokenId());
                    map.put("txHash", record.getTxHash());
                    map.put("status", record.getStatus());
                    map.put("mintedAt", record.getMintedAt());

                    var achievement = achievementRepository.findByCode(record.getAchievementCode()).orElse(null);
                    if (achievement != null) {
                        map.put("achievementName", achievement.getName());
                        map.put("description", achievement.getDescription());
                    }
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(records);
    }
}
