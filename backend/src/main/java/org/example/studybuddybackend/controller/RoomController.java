package org.example.studybuddybackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.studybuddybackend.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getRoomList(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(roomService.getRoomList(status, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRoomDetail(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomDetail(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String name = (String) body.get("name");
        Integer targetMinutes = (Integer) body.get("targetMinutes");
        Integer maxMembers = (Integer) body.get("maxMembers");
        Integer minStake = (Integer) body.get("minStake");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.createRoom(userId, name, targetMinutes, maxMembers, minStake));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, Object>> joinRoom(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer stakeAmount = (Integer) body.get("stakeAmount");
        return ResponseEntity.ok(roomService.joinRoom(id, userId, stakeAmount));
    }

    @PutMapping("/{id}/timer")
    public ResponseEntity<Map<String, Object>> reportTimer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer minutes = (Integer) body.get("minutes");
        return ResponseEntity.ok(roomService.reportTimer(id, userId, minutes));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<Map<String, Object>> settleRoom(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(roomService.settleRoom(id, userId));
    }
}
