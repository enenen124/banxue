package org.example.studybuddybackend.service.impl;

import org.example.studybuddybackend.entity.*;
import org.example.studybuddybackend.repository.*;
import org.example.studybuddybackend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final StudyRoomRepository roomRepository;
    private final StudyRoomMemberRepository memberRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final UserRepository userRepository;

    @Override
    public List<Map<String, Object>> getRoomList(String status, Long currentUserId) {
        // 先自动结算到期的自习室
        autoSettleExpiredRooms();

        List<StudyRoom> rooms;
        if (status != null && !status.isEmpty()) {
            rooms = roomRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            rooms = roomRepository.findAllByOrderByCreatedAtDesc();
        }

        return rooms.stream().map(room -> buildRoomSummary(room, currentUserId)).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getRoomDetail(Long id) {
        StudyRoom room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("自习室不存在"));
        return buildRoomDetail(room);
    }

    @Override
    @Transactional
    public Map<String, Object> createRoom(Long userId, String name, Integer targetMinutes, Integer maxMembers, Integer minStake) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getPoints() < minStake) {
            throw new RuntimeException("积分不足，无法创建");
        }

        StudyRoom room = new StudyRoom();
        room.setName(name);
        room.setTargetMinutes(targetMinutes);
        room.setMaxMembers(maxMembers);
        room.setMinStake(minStake);
        room.setCreatorId(userId);
        room.setStatus("进行中");
        room.setEndAt(LocalDateTime.now().plusDays(7)); // 默认7天后结束

        StudyRoom saved = roomRepository.save(room);

        // 创建者自动加入
        StudyRoomMember member = new StudyRoomMember();
        member.setRoomId(saved.getId());
        member.setUserId(userId);
        member.setStakeAmount(minStake);
        member.setStudyMinutes(0);
        memberRepository.save(member);

        // 扣除积分
        user.setPoints(user.getPoints() - minStake);
        userRepository.save(user);

        return buildRoomDetail(saved);
    }

    @Override
    @Transactional
    public Map<String, Object> joinRoom(Long roomId, Long userId, Integer stakeAmount) {
        StudyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("自习室不存在"));

        if (!"进行中".equals(room.getStatus())) {
            throw new RuntimeException("自习室已关闭，无法加入");
        }

        if (memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("你已在自习室中");
        }

        long memberCount = memberRepository.countByRoomId(roomId);
        if (memberCount >= room.getMaxMembers()) {
            throw new RuntimeException("人数已满");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getPoints() < stakeAmount) {
            throw new RuntimeException("积分不足");
        }

        if (stakeAmount < room.getMinStake()) {
            throw new RuntimeException("押金不能低于最低要求: " + room.getMinStake());
        }

        StudyRoomMember member = new StudyRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setStakeAmount(stakeAmount);
        member.setStudyMinutes(0);
        memberRepository.save(member);

        user.setPoints(user.getPoints() - stakeAmount);
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "加入成功");
        result.put("room", buildRoomDetail(room));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> reportTimer(Long roomId, Long userId, Integer minutes) {
        StudyRoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("你不是该自习室的成员"));

        member.setStudyMinutes(member.getStudyMinutes() + minutes);
        memberRepository.save(member);

        // 记录学习记录
        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        record.setRoomId(roomId);
        record.setMinutes(minutes);
        studyRecordRepository.save(record);

        StudyRoom room = roomRepository.findById(roomId).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "计时已更新");
        result.put("studyMinutes", member.getStudyMinutes());
        result.put("targetMinutes", room != null ? room.getTargetMinutes() : null);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> settleRoom(Long roomId, Long userId) {
        StudyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("自习室不存在"));

        if ("已关闭".equals(room.getStatus())) {
            throw new RuntimeException("自习室已经结算过了");
        }

        return doSettle(room);
    }

    private Map<String, Object> doSettle(StudyRoom room) {
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
                poolStake += m.getStakeAmount(); // 不合格者的押金进入奖池
            }
        }

        // 分配奖池给合格成员
        int bonusPerPerson = qualifiedCount > 0 ? poolStake / qualifiedCount : 0;
        for (StudyRoomMember m : members) {
            if (Boolean.TRUE.equals(m.getQualified())) {
                m.setBonus(bonusPerPerson);
                // 返还押金 + 奖金
                User u = userRepository.findById(m.getUserId()).orElse(null);
                if (u != null) {
                    u.setPoints(u.getPoints() + m.getStakeAmount() + bonusPerPerson);
                    userRepository.save(u);
                }
            } else {
                // 不合格者不返还押金（已扣除）
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

    private void autoSettleExpiredRooms() {
        List<StudyRoom> activeRooms = roomRepository.findByStatusOrderByCreatedAtDesc("进行中");
        for (StudyRoom room : activeRooms) {
            if (room.getEndAt().isBefore(LocalDateTime.now())) {
                doSettle(room);
            }
        }
    }

    private Map<String, Object> buildRoomSummary(StudyRoom room, Long currentUserId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", room.getId());
        map.put("name", room.getName());
        map.put("targetMinutes", room.getTargetMinutes());
        map.put("maxMembers", room.getMaxMembers());
        map.put("minStake", room.getMinStake());
        map.put("status", room.getStatus());
        map.put("creator", buildAuthorInfo(room.getCreatorId()));

        List<StudyRoomMember> members = memberRepository.findByRoomId(room.getId());
        map.put("members", members.stream().map(this::buildMemberMap).collect(Collectors.toList()));
        map.put("memberCount", members.size());

        boolean isJoined = currentUserId != null && members.stream().anyMatch(m -> m.getUserId().equals(currentUserId));
        map.put("isJoined", isJoined);
        map.put("createdAt", room.getCreatedAt());
        map.put("endAt", room.getEndAt());

        // 如果已结算，添加结算信息
        if ("已关闭".equals(room.getStatus())) {
            map.put("settleSummary", buildSettleSummary(room, currentUserId));
        }

        return map;
    }

    private Map<String, Object> buildRoomDetail(StudyRoom room) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", room.getId());
        map.put("name", room.getName());
        map.put("targetMinutes", room.getTargetMinutes());
        map.put("maxMembers", room.getMaxMembers());
        map.put("minStake", room.getMinStake());
        map.put("status", room.getStatus());
        map.put("creator", buildAuthorInfo(room.getCreatorId()));

        List<StudyRoomMember> members = memberRepository.findByRoomId(room.getId());
        map.put("members", members.stream().map(this::buildMemberMap).collect(Collectors.toList()));
        map.put("createdAt", room.getCreatedAt());
        map.put("endAt", room.getEndAt());
        return map;
    }

    private Map<String, Object> buildMemberMap(StudyRoomMember m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user", buildAuthorInfo(m.getUserId()));
        map.put("stakeAmount", m.getStakeAmount());
        map.put("studyMinutes", m.getStudyMinutes());
        map.put("qualified", m.getQualified());
        return map;
    }

    private Map<String, Object> buildSettleSummary(StudyRoom room, Long currentUserId) {
        List<StudyRoomMember> members = memberRepository.findByRoomId(room.getId());

        int qualifiedCount = 0;
        int unqualifiedCount = 0;
        int poolStake = 0;

        for (StudyRoomMember m : members) {
            if (Boolean.TRUE.equals(m.getQualified())) {
                qualifiedCount++;
            } else if (Boolean.FALSE.equals(m.getQualified())) {
                unqualifiedCount++;
                poolStake += m.getStakeAmount();
            }
        }

        int bonusPerPerson = qualifiedCount > 0 ? poolStake / qualifiedCount : 0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("qualifiedCount", qualifiedCount);
        summary.put("unqualifiedCount", unqualifiedCount);
        summary.put("poolStake", poolStake);
        summary.put("bonusPerPerson", bonusPerPerson);

        // 当前用户的结算结果
        if (currentUserId != null) {
            members.stream()
                    .filter(m -> m.getUserId().equals(currentUserId))
                    .findFirst()
                    .ifPresent(m -> {
                        Map<String, Object> myResult = new LinkedHashMap<>();
                        boolean qualified = Boolean.TRUE.equals(m.getQualified());
                        myResult.put("qualified", qualified);
                        myResult.put("stakeReturn", qualified ? m.getStakeAmount() : 0);
                        myResult.put("bonus", m.getBonus());
                        myResult.put("netGain", qualified ? m.getBonus() : -m.getStakeAmount());
                        summary.put("myResult", myResult);
                    });
        }

        return summary;
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
}
