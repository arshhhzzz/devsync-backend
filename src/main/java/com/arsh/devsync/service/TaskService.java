package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateTaskRequest;
import com.arsh.devsync.dto.PagedResponse;
import com.arsh.devsync.dto.TaskResponse;
import com.arsh.devsync.dto.UpdateTaskRequest;
import com.arsh.devsync.entity.*;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.ProjectRepository;
import com.arsh.devsync.repository.TaskRepository;
import com.arsh.devsync.repository.UserRepository;
import com.arsh.devsync.repository.WorkspaceMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final AuditLogService auditLogService;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            WorkspaceMembershipRepository membershipRepository,
            AuditLogService auditLogService
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogService = auditLogService;
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

        Task savedTask = taskRepository.save(task);

        auditLogService.log(
                project.getWorkspace().getId(),
                email,
                "TASK_CREATED",
                "TASK",
                savedTask.getId()
        );

        return savedTask;
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
            throw new UnauthorizedActionException("You are not allowed to update this task");
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

        Task updatedTask = taskRepository.save(task);

        auditLogService.log(
                updatedTask.getProject().getWorkspace().getId(),
                email,
                "TASK_UPDATED",
                "TASK",
                updatedTask.getId()
        );

        return updatedTask;
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
            throw new UnauthorizedActionException("You are not allowed to delete this task");
        }

        Long workspaceId = task.getProject().getWorkspace().getId();
        Long taskId = task.getId();

        taskRepository.delete(task);

        auditLogService.log(
                workspaceId,
                email,
                "TASK_DELETED",
                "TASK",
                taskId
        );
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
        User user = getUserByEmail(email);
        Pageable pageable = PageRequest.of(page, size);

        Page<Task> taskPage;

        if (status != null && !status.isBlank()) {
            TaskStatus taskStatus;

            try {
                taskStatus = TaskStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid task status: " + status);
            }

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
                .orElseThrow(() -> new UnauthorizedActionException("You are not a member of this workspace"));
    }

    private User resolveAssignee(Long assigneeId, Project project, User currentUser, WorkspaceMembership membership) {
        if (assigneeId == null) {
            return null;
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found with id: " + assigneeId));

        membershipRepository.findByWorkspaceAndUser(project.getWorkspace(), assignee)
                .orElseThrow(() -> new UnauthorizedActionException("Assignee is not a member of this workspace"));

        boolean isAdminOrOwner = membership.getRole() == WorkspaceRole.OWNER ||
                membership.getRole() == WorkspaceRole.ADMIN;

        if (!isAdminOrOwner && !assignee.getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("MEMBER can only assign task to themselves");
        }

        return assignee;
    }

    @Transactional
    public Task restoreTask(Long taskId, String email) {
        Task task = taskRepository.findByIdIncludingDeleted(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        Long projectId = taskRepository.findProjectIdIncludingDeletedTask(taskId);

        Project project = projectRepository.findByIdIncludingDeleted(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Workspace workspace = project.getWorkspace();

        if (workspace.getDeletedAt() != null) {
            throw new UnauthorizedActionException("Cannot restore task because workspace is deleted");
        }

        if (project.getDeletedAt() != null) {
            throw new UnauthorizedActionException("Cannot restore task because project is deleted");
        }

        User user = getUserByEmail(email);

        WorkspaceMembership membership = membershipRepository
                .findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new UnauthorizedActionException("You are not a member of this workspace"));

        boolean isOwnerOrAdmin =
                membership.getRole() == WorkspaceRole.OWNER ||
                        membership.getRole() == WorkspaceRole.ADMIN;

        boolean isCreator =
                task.getUser() != null &&
                        task.getUser().getId().equals(user.getId());

        if (!isOwnerOrAdmin && !isCreator) {
            throw new UnauthorizedActionException("You are not allowed to restore this task");
        }

        task.setDeletedAt(null);

        Task restoredTask = taskRepository.save(task);

        auditLogService.log(
                workspace.getId(),
                email,
                "TASK_RESTORED",
                "TASK",
                restoredTask.getId()
        );

        return restoredTask;
    }

}