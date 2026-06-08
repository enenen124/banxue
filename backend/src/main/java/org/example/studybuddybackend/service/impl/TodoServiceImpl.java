package org.example.studybuddybackend.service.impl;

import org.example.studybuddybackend.entity.Todo;
import org.example.studybuddybackend.repository.TodoRepository;
import org.example.studybuddybackend.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {
    private final TodoRepository todoRepository;

    @Override
    public Todo createTodo(Long userId, String title) {
        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setTitle(title);
        todo.setCompleted(false);
        return todoRepository.save(todo);
    }

    @Override
    public List<Todo> getTodos(Long userId) {
        return todoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Todo updateTodo(Long id, Long userId, Boolean completed) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("待办不存在"));
        if (!todo.getUserId().equals(userId)) {
            throw new RuntimeException("无权限修改此待办");
        }
        if (completed != null) {
            todo.setCompleted(completed);
        }
        return todoRepository.save(todo);
    }

    @Override
    public void deleteTodo(Long id, Long userId) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("待办不存在"));
        if (!todo.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此待办");
        }
        todoRepository.delete(todo);
    }
}
