package org.example.studybuddybackend.service;

import org.example.studybuddybackend.entity.Todo;

import java.util.List;

public interface TodoService {
    Todo createTodo(Long userId, String title);
    List<Todo> getTodos(Long userId);
    Todo updateTodo(Long id, Long userId, Boolean completed);
    void deleteTodo(Long id, Long userId);
}
