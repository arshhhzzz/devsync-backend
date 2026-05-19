package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.dto.PagedResponse;
import com.arsh.devsync.dto.TaskResponse;
import com.arsh.devsync.dto.UpdateTaskRequest;
import com.arsh.devsync.entity.*;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.ProjectRepository;
import com.arsh.devsync.repository.TaskRepository;
import com.arsh.devsync.repository.UserRepository;
import com.arsh.devsync.repository.WorkspaceMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            WorkspaceMembershipRepository membershipRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
    }

    public Task createTask(Long projectId, CreateTaskRequest request, String email) {
        User user = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);

        WorkspaceMembership membership = getMembershipForProject(project, user);

        User assignee = resolveAssignee(
                request.getAssigneeId(),
                project,
                user,
                membership
        );

        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getDueDate()
        );

        task.setUser(user);
        task.setProject(project);
        task.setAssignee(assignee);

        return taskRepository.save(task);
    }

    public List<Task> getTasksByProject(Long projectId, String email) {
        User user = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);

        getMembershipForProject(project, user);

        return taskRepository.findByProjectId(projectId);
    }

    public Task getTaskById(Long id, String email) {
        Task task = getTaskOrThrow(id);
        User user = getUserByEmail(email);

        getMembershipForProject(task.getProject(), user);

        return task;
    }

    public Task updateTask(Long id, UpdateTaskRequest request, String email) {
        Task task = getTaskOrThrow(id);
        User user = getUserByEmail(email);

        WorkspaceMembership membership = getMembershipForProject(task.getProject(), user);

        boolean isTaskCreator = task.getUser().getId().equals(user.getId());
        boolean isAdminOrOwner = membership.getRole() == WorkspaceRole.OWNER ||
                membership.getRole() == WorkspaceRole.ADMIN;

        if (!isTaskCreator && !isAdminOrOwner) {
            throw new RuntimeException("You are not allowed to update this task");
        }

        User assignee = resolveAssignee(
                request.getAssigneeId(),
                task.getProject(),
                user,
                membership
        );

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignee(assignee);

        return taskRepository.save(task);
    }

    public List<Task> getTasksAssignedToMe(String email) {
        User user = getUserByEmail(email);
        return taskRepository.findByAssignee(user);
    }

    public void deleteTaskById(Long id, String email) {
        Task task = getTaskOrThrow(id);
        User user = getUserByEmail(email);

        WorkspaceMembership membership = getMembershipForProject(task.getProject(), user);

        boolean isTaskCreator = task.getUser().getId().equals(user.getId());
        boolean isAdminOrOwner = membership.getRole() == WorkspaceRole.OWNER ||
                membership.getRole() == WorkspaceRole.ADMIN;

        if (!isTaskCreator && !isAdminOrOwner) {
            throw new RuntimeException("You are not allowed to delete this task");
        }

        taskRepository.delete(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getMyTasks(String email) {
        User user = getUserByEmail(email);
        return taskRepository.findByUser(user);
    }

    public PagedResponse<TaskResponse> getMyTasksPaginated(
            String email,
            int page,
            int size,
            String status
    ) {
        TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());

        User user = getUserByEmail(email);

        Pageable pageable = PageRequest.of(page, size);

        Page<Task> taskPage;

        if (status != null && !status.isBlank()) {
            taskPage = taskRepository.findByUserAndStatus(user, taskStatus, pageable);
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

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    private WorkspaceMembership getMembershipForProject(Project project, User user) {
        return membershipRepository.findByWorkspaceAndUser(project.getWorkspace(), user)
                .orElseThrow(() -> new RuntimeException("You are not a member of this workspace"));
    }

    private User resolveAssignee(Long assigneeId, Project project, User currentUser, WorkspaceMembership membership) {
        if (assigneeId == null) {
            return null;
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found with id: " + assigneeId));

        membershipRepository.findByWorkspaceAndUser(project.getWorkspace(), assignee)
                .orElseThrow(() -> new RuntimeException("Assignee is not a member of this workspace"));

        boolean isAdminOrOwner = membership.getRole() == WorkspaceRole.OWNER ||
                membership.getRole() == WorkspaceRole.ADMIN;

        if (!isAdminOrOwner && !assignee.getId().equals(currentUser.getId())) {
            throw new RuntimeException("MEMBER can only assign task to themselves");
        }

        return assignee;
    }


}