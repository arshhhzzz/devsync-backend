package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateProjectRequest;
import com.arsh.devsync.dto.UpdateProjectRequest;
import com.arsh.devsync.entity.*;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMembershipRepository membershipRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_shouldCreateProject_whenUserIsOwner() {
        User owner = user(1L, "Owner", "owner@test.com");
        Workspace workspace = workspace(10L, owner);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                owner,
                WorkspaceRole.OWNER
        );

        CreateProjectRequest request = new CreateProjectRequest();
        setField(request, "name", "Backend API");
        setField(request, "description", "Spring Boot backend");

        Project savedProject = new Project("Backend API", "Spring Boot backend");
        setField(savedProject, "id", 100L);
        savedProject.setWorkspace(workspace);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(membership));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        Project result = projectService.createProject(10L, request, "owner@test.com");

        assertEquals("Backend API", result.getName());
        assertEquals(workspace, result.getWorkspace());

        verify(projectRepository).save(any(Project.class));
        verify(auditLogService).log(
                eq(10L),
                eq("owner@test.com"),
                eq("PROJECT_CREATED"),
                eq("PROJECT"),
                eq(100L)
        );
    }

    @Test
    void createProject_shouldThrowException_whenUserIsOnlyMember() {
        User member = user(1L, "Member", "member@test.com");
        Workspace workspace = workspace(10L, member);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                member,
                WorkspaceRole.MEMBER
        );

        CreateProjectRequest request = new CreateProjectRequest();
        setField(request, "name", "Backend API");
        setField(request, "description", "Spring Boot backend");

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, member))
                .thenReturn(Optional.of(membership));

        assertThrows(
                UnauthorizedActionException.class,
                () -> projectService.createProject(10L, request, "member@test.com")
        );

        verify(projectRepository, never()).save(any());
    }

    @Test
    void getProjectById_shouldReturnProject_whenUserIsWorkspaceMember() {
        User member = user(1L, "Member", "member@test.com");
        Workspace workspace = workspace(10L, member);
        Project project = project(100L, workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                member,
                WorkspaceRole.MEMBER
        );

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, member))
                .thenReturn(Optional.of(membership));

        Project result = projectService.getProjectById(100L, "member@test.com");

        assertEquals(100L, result.getId());
        assertEquals("Backend API", result.getName());
    }

    @Test
    void getProjectById_shouldThrowException_whenProjectDoesNotExist() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> projectService.getProjectById(999L, "member@test.com")
        );
    }

    @Test
    void updateProject_shouldUpdateProject_whenUserIsAdmin() {
        User admin = user(1L, "Admin", "admin@test.com");
        Workspace workspace = workspace(10L, admin);
        Project project = project(100L, workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                admin,
                WorkspaceRole.ADMIN
        );

        UpdateProjectRequest request = new UpdateProjectRequest();
        setField(request, "name", "Updated API");
        setField(request, "description", "Updated description");

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, admin))
                .thenReturn(Optional.of(membership));
        when(projectRepository.save(project)).thenReturn(project);

        Project result = projectService.updateProject(100L, request, "admin@test.com");

        assertEquals("Updated API", result.getName());
        assertEquals("Updated description", result.getDescription());

        verify(projectRepository).save(project);
        verify(auditLogService).log(
                eq(10L),
                eq("admin@test.com"),
                eq("PROJECT_UPDATED"),
                eq("PROJECT"),
                eq(100L)
        );
    }

    @Test
    void updateProject_shouldThrowException_whenUserIsMember() {
        User member = user(1L, "Member", "member@test.com");
        Workspace workspace = workspace(10L, member);
        Project project = project(100L, workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                member,
                WorkspaceRole.MEMBER
        );

        UpdateProjectRequest request = new UpdateProjectRequest();
        setField(request, "name", "Updated API");
        setField(request, "description", "Updated description");

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, member))
                .thenReturn(Optional.of(membership));

        assertThrows(
                UnauthorizedActionException.class,
                () -> projectService.updateProject(100L, request, "member@test.com")
        );

        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_shouldDeleteProject_whenUserIsOwner() {
        User owner = user(1L, "Owner", "owner@test.com");
        Workspace workspace = workspace(10L, owner);
        Project project = project(100L, workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                owner,
                WorkspaceRole.OWNER
        );

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(membership));
        when(taskRepository.findByProject(project)).thenReturn(List.of());

        projectService.deleteProject(100L, "owner@test.com");

        verify(taskRepository).findByProject(project);
        verify(projectRepository).delete(project);
        verify(auditLogService).log(
                eq(10L),
                eq("owner@test.com"),
                eq("PROJECT_DELETED"),
                eq("PROJECT"),
                eq(100L)
        );
    }

    @Test
    void deleteProject_shouldSoftDeleteTasksBeforeProject() {
        User owner = user(1L, "Owner", "owner@test.com");
        Workspace workspace = workspace(10L, owner);
        Project project = project(100L, workspace);

        Task task = new Task(
                "Task",
                "Desc",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                java.time.LocalDate.now()
        );

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                owner,
                WorkspaceRole.OWNER
        );

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(membership));
        when(taskRepository.findByProject(project)).thenReturn(List.of(task));

        projectService.deleteProject(100L, "owner@test.com");

        verify(taskRepository).delete(task);
        verify(projectRepository).delete(project);
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