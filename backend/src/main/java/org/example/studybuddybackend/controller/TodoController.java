package org.example.studybuddybackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.studybuddybackend.entity.Todo;
import org.example.studybuddybackend.service.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {
    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<Todo> createTodo(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.createTodo(userId, body.get("title")));
    }

    @GetMapping
    public ResponseEntity<List<Todo>> getTodos(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(todoService.getTodos(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Boolean completed = (Boolean) body.get("completed");
        return ResponseEntity.ok(todoService.updateTodo(id, userId, completed));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTodo(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        todoService.deleteTodo(id, userId);
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }
}
