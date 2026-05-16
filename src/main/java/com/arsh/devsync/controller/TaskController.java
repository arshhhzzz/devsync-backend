package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.service.TaskService;
import jakarta.validation.Valid;
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

    @GetMapping("/{id}")
    public Task getTaaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }
}
