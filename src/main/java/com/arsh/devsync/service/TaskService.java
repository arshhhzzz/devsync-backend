package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.dto.PagedResponse;
import com.arsh.devsync.dto.TaskResponse;
import com.arsh.devsync.dto.UpdateTaskRequest;
import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.TaskRepository;
import com.arsh.devsync.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private Task getTaskIfOwner(Long taskId, String email) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        if (!task.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You are not allowed to access this task");
        }

        return task;
    }

    public Task createTask(CreateTaskRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        System.out.println(user);
        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                request.getStatus()
        );
        task.setUser(user);
        return taskRepository.save(task);
    }

    public Task getTaskById(Long id, String email) {
        return getTaskIfOwner(id, email);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public void deleteTaskById(Long id,  String email) {
        Task task = getTaskIfOwner(id, email);
        taskRepository.delete(task);
    }

    public Task updateTask(Long id, UpdateTaskRequest request,  String email) {
        Task task = getTaskById(id,  email);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());

        return taskRepository.save(task);
    }

    public List<Task> getMyTasks(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return taskRepository.findByUser(user);
    }

    public PagedResponse<TaskResponse> getMyTasksPaginated(
            String email,
            int page,
            int size,
            String status
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size);

        Page<Task> taskPage;

        if (status != null && !status.isBlank()) {
            taskPage = taskRepository.findByUserAndStatus(user, status, pageable);
        } else {
            taskPage = taskRepository.findByUser(user, pageable);
        }

        List<TaskResponse> content = taskPage.getContent()
                .stream()
                .map(TaskResponse::new)
                .toList();

        return new PagedResponse<>(
                content,
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.isLast()
        );
    }
}
