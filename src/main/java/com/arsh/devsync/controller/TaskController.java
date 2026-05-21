package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.dto.PagedResponse;
import com.arsh.devsync.dto.TaskResponse;
import com.arsh.devsync.dto.UpdateTaskRequest;
import com.arsh.devsync.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId:\\d+}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        return new TaskResponse(
                taskService.createTask(projectId, request, authentication.getName())
        );
    }

    @GetMapping("/projects/{projectId:\\d+}/tasks")
    public List<TaskResponse> getTasksByProject(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return taskService.getTasksByProject(projectId, authentication.getName())
                .stream()
                .map(TaskResponse::new)
                .toList();
    }

    @GetMapping("/tasks/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TaskResponse> getAllTasksForAdmin() {
        return taskService.getAllTasks()
                .stream()
                .map(TaskResponse::new)
                .toList();
    }

    @GetMapping("/tasks/{id:\\d+}")
    public TaskResponse getTaskById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new TaskResponse(
                taskService.getTaskById(id, authentication.getName())
        );
    }

    @PutMapping("/tasks/{id:\\d+}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication
    ) {
        return new TaskResponse(
                taskService.updateTask(id, request, authentication.getName())
        );
    }

    @DeleteMapping("/tasks/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        taskService.deleteTaskById(id, authentication.getName());
    }

    @GetMapping("/tasks/my")
    public PagedResponse<TaskResponse> getMyTasks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        return taskService.getMyTasksPaginated(
                authentication.getName(),
                page,
                size,
                status
        );
    }

    @GetMapping("/tasks/assigned-to-me")
    public List<TaskResponse> getTasksAssignedToMe(Authentication authentication) {
        return taskService.getTasksAssignedToMe(authentication.getName())
                .stream()
                .map(TaskResponse::new)
                .toList();
    }

    @PostMapping("/{id}/restore")
    public TaskResponse restoreTask(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new TaskResponse(
                taskService.restoreTask(id, authentication.getName())
        );
    }
}