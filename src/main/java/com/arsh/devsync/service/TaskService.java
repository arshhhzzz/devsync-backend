package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.TaskRepository;
import com.arsh.devsync.repository.UserRepository;
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

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
}
