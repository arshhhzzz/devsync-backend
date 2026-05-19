package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.dto.PagedResponse;
import com.arsh.devsync.dto.TaskResponse;
import com.arsh.devsync.dto.UpdateTaskRequest;
import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public TaskResponse CreateTask(@Valid @RequestBody CreateTaskRequest request, Authentication authentication) {
        return new TaskResponse(taskService.createTask(request, authentication.getName()));
    }

    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return taskService.getAllTasks()
                .stream()
                .map(TaskResponse::new)
                .toList();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TaskResponse> getAllTasksForAdmin() {
        return taskService.getAllTasks()
                .stream()
                .map(TaskResponse::new)
                .toList();
    }

    @GetMapping("/{id:\\d+}")
    public TaskResponse getTaskById(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        return new TaskResponse(taskService.getTaskById(id, email));
    }

    @DeleteMapping("/{id}")
    public void deleteTaskById(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        taskService.deleteTaskById(id,  email);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request,  Authentication authentication) {
        String email = authentication.getName();
        return new TaskResponse(taskService.updateTask(id, request, email));
    }

    @GetMapping("/my")
    public PagedResponse<TaskResponse> getMyTasks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        String email = authentication.getName();

        return taskService.getMyTasksPaginated(email, page, size, status);
    }
}
