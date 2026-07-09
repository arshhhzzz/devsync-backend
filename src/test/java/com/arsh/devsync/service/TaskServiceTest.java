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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.arsh.devsync.event.DomainEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TaskService taskService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Test
    void createTask_shouldCreateTask_whenUserIsWorkspaceMember() {
        User creator = user(1L, "Arsh", "arsh@test.com");
        Workspace workspace = workspace(10L, creator);
        Project project = project(20L, workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                creator,
                WorkspaceRole.MEMBER
        );

        CreateTaskRequest request = createTaskRequest(null);

        Task savedTask = new Task(
                "Build auth",
                "JWT auth",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.now().plusDays(3)
        );
        setField(savedTask, "id", 100L);
        savedTask.setUser(creator);
        savedTask.setProject(project);

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(creator));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(membershipRepository.findByWorkspaceAndUser(workspace, creator))
                .thenReturn(Optional.of(membership));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        Task result = taskService.createTask(20L, request, "arsh@test.com");

        assertEquals("Build auth", result.getTitle());
        assertEquals(project, result.getProject());
        assertEquals(creator, result.getUser());
        assertNull(result.getAssignee());

        verify(taskRepository).save(any(Task.class));
        verify(auditLogService).log(
                eq(10L),
                eq("arsh@test.com"),
                eq("TASK_CREATED"),
                eq("TASK"),
                eq(100L)
        );
    }

    @Test
    void createTask_shouldAssignTask_whenAdminAssignsWorkspaceMember() {
        User admin = user(1L, "Admin", "admin@test.com");
        User assignee = user(2L, "Member", "member@test.com");

        Workspace workspace = workspace(10L, admin);
        Project project = project(20L, workspace);

        WorkspaceMembership adminMembership = new WorkspaceMembership(
                workspace,
                admin,
                WorkspaceRole.ADMIN
        );

        WorkspaceMembership assigneeMembership = new WorkspaceMembership(
                workspace,
                assignee,
                WorkspaceRole.MEMBER
        );

        CreateTaskRequest request = createTaskRequest(2L);

        Task savedTask = new Task(
                "Build task",
                "Task assignment",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.now().plusDays(3)
        );
        setField(savedTask, "id", 101L);
        savedTask.setUser(admin);
        savedTask.setProject(project);
        savedTask.setAssignee(assignee);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(membershipRepository.findByWorkspaceAndUser(workspace, admin))
                .thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(membershipRepository.findByWorkspaceAndUser(workspace, assignee))
                .thenReturn(Optional.of(assigneeMembership));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        Task result = taskService.createTask(20L, request, "admin@test.com");

        assertEquals(assignee, result.getAssignee());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrowException_whenUserIsNotWorkspaceMember() {
        User user = user(1L, "Arsh", "arsh@test.com");
        Workspace workspace = workspace(10L, user);
        Project project = project(20L, workspace);

        CreateTaskRequest request = createTaskRequest(null);

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(membershipRepository.findByWorkspaceAndUser(workspace, user))
                .thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedActionException.class,
                () -> taskService.createTask(20L, request, "arsh@test.com")
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_shouldThrowException_whenAssigneeIsNotWorkspaceMember() {
        User admin = user(1L, "Admin", "admin@test.com");
        User assignee = user(2L, "External", "external@test.com");

        Workspace workspace = workspace(10L, admin);
        Project project = project(20L, workspace);

        WorkspaceMembership adminMembership = new WorkspaceMembership(
                workspace,
                admin,
                WorkspaceRole.ADMIN
        );

        CreateTaskRequest request = createTaskRequest(2L);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(membershipRepository.findByWorkspaceAndUser(workspace, admin))
                .thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(membershipRepository.findByWorkspaceAndUser(workspace, assignee))
                .thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedActionException.class,
                () -> taskService.createTask(20L, request, "admin@test.com")
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_shouldThrowException_whenMemberAssignsTaskToAnotherUser() {
        User member = user(1L, "Member", "member@test.com");
        User otherUser = user(2L, "Other", "other@test.com");

        Workspace workspace = workspace(10L, member);
        Project project = project(20L, workspace);

        WorkspaceMembership memberMembership = new WorkspaceMembership(
                workspace,
                member,
                WorkspaceRole.MEMBER
        );

        WorkspaceMembership otherMembership = new WorkspaceMembership(
                workspace,
                otherUser,
                WorkspaceRole.MEMBER
        );

        CreateTaskRequest request = createTaskRequest(2L);

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(membershipRepository.findByWorkspaceAndUser(workspace, member))
                .thenReturn(Optional.of(memberMembership));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(membershipRepository.findByWorkspaceAndUser(workspace, otherUser))
                .thenReturn(Optional.of(otherMembership));

        assertThrows(
                UnauthorizedActionException.class,
                () -> taskService.createTask(20L, request, "member@test.com")
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_shouldUpdateTask_whenRequesterIsTaskCreator() {
        User creator = user(1L, "Creator", "creator@test.com");
        Workspace workspace = workspace(10L, creator);
        Project project = project(20L, workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                creator,
                WorkspaceRole.MEMBER
        );

        Task existingTask = new Task(
                "Old title",
                "Old desc",
                TaskStatus.TODO,
                TaskPriority.LOW,
                LocalDate.now()
        );
        setField(existingTask, "id", 100L);
        existingTask.setUser(creator);
        existingTask.setProject(project);

        UpdateTaskRequest request = updateTaskRequest(null);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(existingTask));
        when(userRepository.findByEmail("creator@test.com")).thenReturn(Optional.of(creator));
        when(membershipRepository.findByWorkspaceAndUser(workspace, creator))
                .thenReturn(Optional.of(membership));
        when(taskRepository.save(existingTask)).thenReturn(existingTask);

        Task result = taskService.updateTask(100L, request, "creator@test.com");

        assertEquals("Updated task", result.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());

        verify(taskRepository).save(existingTask);
        verify(auditLogService).log(
                eq(10L),
                eq("creator@test.com"),
                eq("TASK_UPDATED"),
                eq("TASK"),
                eq(100L)
        );
    }

    @Test
    void updateTask_shouldThrowException_whenRequesterIsDifferentMember() {
        User creator = user(1L, "Creator", "creator@test.com");
        User otherMember = user(2L, "Other", "other@test.com");

        Workspace workspace = workspace(10L, creator);
        Project project = project(20L, workspace);

        WorkspaceMembership otherMembership = new WorkspaceMembership(
                workspace,
                otherMember,
                WorkspaceRole.MEMBER
        );

        Task existingTask = new Task(
                "Old title",
                "Old desc",
                TaskStatus.TODO,
                TaskPriority.LOW,
                LocalDate.now()
        );
        setField(existingTask, "id", 100L);
        existingTask.setUser(creator);
        existingTask.setProject(project);

        UpdateTaskRequest request = updateTaskRequest(null);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(existingTask));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherMember));
        when(membershipRepository.findByWorkspaceAndUser(workspace, otherMember))
                .thenReturn(Optional.of(otherMembership));

        assertThrows(
                UnauthorizedActionException.class,
                () -> taskService.updateTask(100L, request, "other@test.com")
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    void getMyTasksPaginated_shouldReturnAllTasks_whenStatusIsNull() {
        User user = user(1L, "Arsh", "arsh@test.com");

        Task task = new Task(
                "Task",
                "Desc",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.now()
        );

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(taskRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        PagedResponse<TaskResponse> response = taskService.getMyTasksPaginated(
                "arsh@test.com",
                0,
                10,
                null
        );

        assertEquals(1, response.getContent().size());
        verify(taskRepository).findByUser(eq(user), any(Pageable.class));
        verify(taskRepository, never()).findByUserAndStatus(any(), any(), any());
    }

    @Test
    void getMyTasksPaginated_shouldReturnFilteredTasks_whenStatusIsPresent() {
        User user = user(1L, "Arsh", "arsh@test.com");

        Task task = new Task(
                "Task",
                "Desc",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.now()
        );

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(taskRepository.findByUserAndStatus(eq(user), eq(TaskStatus.TODO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        PagedResponse<TaskResponse> response = taskService.getMyTasksPaginated(
                "arsh@test.com",
                0,
                10,
                "TODO"
        );

        assertEquals(1, response.getContent().size());
        verify(taskRepository).findByUserAndStatus(eq(user), eq(TaskStatus.TODO), any(Pageable.class));
    }

    @Test
    void getMyTasksPaginated_shouldThrowException_whenStatusIsInvalid() {
        User user = user(1L, "Arsh", "arsh@test.com");

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> taskService.getMyTasksPaginated(
                        "arsh@test.com",
                        0,
                        10,
                        "INVALID"
                )
        );

        verify(taskRepository, never()).findByUser(any(User.class), any(Pageable.class));
        verify(taskRepository, never()).findByUserAndStatus(any(), any(), any());
    }

    @Test
    void deleteTask_shouldDeleteTask_whenRequesterIsAdmin() {
        User creator = user(1L, "Creator", "creator@test.com");
        User admin = user(2L, "Admin", "admin@test.com");

        Workspace workspace = workspace(10L, creator);
        Project project = project(20L, workspace);

        WorkspaceMembership adminMembership = new WorkspaceMembership(
                workspace,
                admin,
                WorkspaceRole.ADMIN
        );

        Task task = new Task(
                "Task",
                "Desc",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.now()
        );
        setField(task, "id", 100L);
        task.setUser(creator);
        task.setProject(project);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(membershipRepository.findByWorkspaceAndUser(workspace, admin))
                .thenReturn(Optional.of(adminMembership));

        taskService.deleteTaskById(100L, "admin@test.com");

        verify(taskRepository).delete(task);
        verify(auditLogService).log(
                eq(10L),
                eq("admin@test.com"),
                eq("TASK_DELETED"),
                eq("TASK"),
                eq(100L)
        );
    }

    @Test
    void getTaskById_shouldThrowException_whenTaskDoesNotExist() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.getTaskById(999L, "arsh@test.com")
        );
    }

    private User user(Long id, String name, String email) {
        User user = new User(name, email, "USER", "hashedPassword");
        setField(user, "id", id);
        return user;
    }

    private Workspace workspace(Long id, User owner) {
        Workspace workspace = new Workspace("DevSync", "Backend", owner);
        workspace.setId(id);
        return workspace;
    }

    private Project project(Long id, Workspace workspace) {
        Project project = new Project("Backend API", "Spring Boot backend");
        setField(project, "id", id);
        project.setWorkspace(workspace);
        return project;
    }

    private CreateTaskRequest createTaskRequest(Long assigneeId) {
        CreateTaskRequest request = new CreateTaskRequest();
        setField(request, "title", "Build auth");
        setField(request, "description", "JWT auth");
        setField(request, "status", TaskStatus.TODO);
        setField(request, "priority", TaskPriority.HIGH);
        setField(request, "dueDate", LocalDate.now().plusDays(3));
        setField(request, "assigneeId", assigneeId);
        return request;
    }

    private UpdateTaskRequest updateTaskRequest(Long assigneeId) {
        UpdateTaskRequest request = new UpdateTaskRequest();
        setField(request, "title", "Updated task");
        setField(request, "description", "Updated description");
        setField(request, "status", TaskStatus.IN_PROGRESS);
        setField(request, "priority", TaskPriority.MEDIUM);
        setField(request, "dueDate", LocalDate.now().plusDays(5));
        setField(request, "assigneeId", assigneeId);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}