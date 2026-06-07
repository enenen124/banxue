package org.example.studybuddybackend.service;

import java.util.List;
import java.util.Map;

public interface RoomService {
    List<Map<String, Object>> getRoomList(String status, Long currentUserId);
    Map<String, Object> getRoomDetail(Long id);
    Map<String, Object> createRoom(Long userId, String name, Integer targetMinutes, Integer maxMembers, Integer minStake);
    Map<String, Object> joinRoom(Long roomId, Long userId, Integer stakeAmount);
    Map<String, Object> reportTimer(Long roomId, Long userId, Integer minutes);
    Map<String, Object> settleRoom(Long roomId, Long userId);
}
