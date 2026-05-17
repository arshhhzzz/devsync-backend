package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.dto.UpdateTaskRequest;
import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    @PostMapping
    public Task CreateTask(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(request);
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/my")
    public List<Task> getMyTasks(Authentication authentication) {
        String email = authentication.getName();
        return taskService.getMyTasks(email);
    }

    @GetMapping("/{id:\\d+}")
    public Task getTaskById(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        return taskService.getTaskById(id, email);
    }

    @DeleteMapping("/{id}")
    public void deleteTaskById(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        taskService.deleteTaskById(id,  email);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request,  Authentication authentication) {
        String email = authentication.getName();
        return taskService.updateTask(id, request, email);
    }
}
