package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateCommentRequest;
import com.arsh.devsync.dto.UpdateCommentRequest;
import com.arsh.devsync.entity.*;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceMembershipRepository membershipRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addCommentToTask_shouldCreateComment_whenUserIsWorkspaceMember() {
        User user = user(1L, "Arsh", "arsh@test.com");
        Workspace workspace = workspace(10L, user);
        Project project = project(20L, workspace);
        Task task = task(30L, project, user);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                user,
                WorkspaceRole.MEMBER
        );

        CreateCommentRequest request = new CreateCommentRequest("Looks good");

        Comment savedComment = new Comment("Looks good", user, task, null);
        setField(savedComment, "id", 100L);

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(membershipRepository.findByWorkspaceAndUser(workspace, user))
                .thenReturn(Optional.of(membership));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = commentService.addCommentToTask(30L, request, "arsh@test.com");

        assertEquals("Looks good", result.getContent());
        assertEquals(user, result.getAuthor());
        assertEquals(task, result.getTask());
        assertNull(result.getProject());

        verify(commentRepository).save(any(Comment.class));
        verify(auditLogService).log(
                eq(10L),
                eq("arsh@test.com"),
                eq("COMMENT_ADDED_TO_TASK"),
                eq("COMMENT"),
                eq(100L)
        );
    }

    @Test
    void addCommentToProject_shouldCreateComment_whenUserIsWorkspaceMember() {
        User user = user(1L, "Arsh", "arsh@test.com");
        Workspace workspace = workspace(10L, user);
        Project project = project(20L, workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                user,
                WorkspaceRole.MEMBER
        );

        CreateCommentRequest request = new CreateCommentRequest("Project looks good");

        Comment savedComment = new Comment("Project looks good", user, null, project);
        setField(savedComment, "id", 101L);

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(membershipRepository.findByWorkspaceAndUser(workspace, user))
                .thenReturn(Optional.of(membership));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = commentService.addCommentToProject(20L, request, "arsh@test.com");

        assertEquals("Project looks good", result.getContent());
        assertEquals(user, result.getAuthor());
        assertNull(result.getTask());
        assertEquals(project, result.getProject());
    }

    @Test
    void addCommentToTask_shouldThrowException_whenUserIsNotWorkspaceMember() {
        User outsider = user(2L, "Outsider", "outsider@test.com");
        User owner = user(1L, "Owner", "owner@test.com");

        Workspace workspace = workspace(10L, owner);
        Project project = project(20L, workspace);
        Task task = task(30L, project, owner);

        CreateCommentRequest request = new CreateCommentRequest("I should not comment");

        when(userRepository.findByEmail("outsider@test.com")).thenReturn(Optional.of(outsider));
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(membershipRepository.findByWorkspaceAndUser(workspace, outsider))
                .thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedActionException.class,
                () -> commentService.addCommentToTask(30L, request, "outsider@test.com")
        );

        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_shouldUpdateComment_whenUserIsAuthor() {
        User author = user(1L, "Author", "author@test.com");
        Workspace workspace = workspace(10L, author);
        Project project = project(20L, workspace);

        Comment comment = new Comment("Old comment", author, null, project);
        setField(comment, "id", 100L);

        UpdateCommentRequest request = new UpdateCommentRequest("Updated comment");

        when(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        Comment result = commentService.updateComment(100L, request, "author@test.com");

        assertEquals("Updated comment", result.getContent());

        verify(commentRepository).save(comment);
        verify(auditLogService).log(
                eq(10L),
                eq("author@test.com"),
                eq("COMMENT_UPDATED"),
                eq("COMMENT"),
                eq(100L)
        );
    }

    @Test
    void updateComment_shouldThrowException_whenUserIsNotAuthor() {
        User author = user(1L, "Author", "author@test.com");
        User otherUser = user(2L, "Other", "other@test.com");

        Workspace workspace = workspace(10L, author);
        Project project = project(20L, workspace);

        Comment comment = new Comment("Old comment", author, null, project);
        setField(comment, "id", 100L);

        UpdateCommentRequest request = new UpdateCommentRequest("Hacked update");

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        assertThrows(
                UnauthorizedActionException.class,
                () -> commentService.updateComment(100L, request, "other@test.com")
        );

        verify(commentRepository, never()).save(any());
    }

    @Test
    void deleteComment_shouldDeleteComment_whenUserIsAuthor() {
        User author = user(1L, "Author", "author@test.com");
        Workspace workspace = workspace(10L, author);
        Project project = project(20L, workspace);

        Comment comment = new Comment("Delete me", author, null, project);
        setField(comment, "id", 100L);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                author,
                WorkspaceRole.MEMBER
        );

        when(userRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(membershipRepository.findByWorkspaceAndUser(workspace, author))
                .thenReturn(Optional.of(membership));

        commentService.deleteComment(100L, "author@test.com");

        verify(commentRepository).delete(comment);
        verify(auditLogService).log(
                eq(10L),
                eq("author@test.com"),
                eq("COMMENT_DELETED"),
                eq("COMMENT"),
                eq(100L)
        );
    }

    @Test
    void deleteComment_shouldDeleteComment_whenUserIsAdmin() {
        User author = user(1L, "Author", "author@test.com");
        User admin = user(2L, "Admin", "admin@test.com");

        Workspace workspace = workspace(10L, author);
        Project project = project(20L, workspace);

        Comment comment = new Comment("Delete by admin", author, null, project);
        setField(comment, "id", 100L);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                admin,
                WorkspaceRole.ADMIN
        );

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(membershipRepository.findByWorkspaceAndUser(workspace, admin))
                .thenReturn(Optional.of(membership));

        commentService.deleteComment(100L, "admin@test.com");

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_shouldThrowException_whenMemberDeletesOthersComment() {
        User author = user(1L, "Author", "author@test.com");
        User member = user(2L, "Member", "member@test.com");

        Workspace workspace = workspace(10L, author);
        Project project = project(20L, workspace);

        Comment comment = new Comment("Protected comment", author, null, project);
        setField(comment, "id", 100L);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                member,
                WorkspaceRole.MEMBER
        );

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(membershipRepository.findByWorkspaceAndUser(workspace, member))
                .thenReturn(Optional.of(membership));

        assertThrows(
                UnauthorizedActionException.class,
                () -> commentService.deleteComment(100L, "member@test.com")
        );

        verify(commentRepository, never()).delete(any());
    }

    @Test
    void getTaskComments_shouldReturnPagedComments_whenUserIsWorkspaceMember() {
        User user = user(1L, "Arsh", "arsh@test.com");
        Workspace workspace = workspace(10L, user);
        Project project = project(20L, workspace);
        Task task = task(30L, project, user);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                user,
                WorkspaceRole.MEMBER
        );

        Comment comment = new Comment("Task comment", user, task, null);

        PageRequest pageRequest = PageRequest.of(0, 10);

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(user));
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(membershipRepository.findByWorkspaceAndUser(workspace, user))
                .thenReturn(Optional.of(membership));
        when(commentRepository.findByTask(task, pageRequest))
                .thenReturn(new PageImpl<>(List.of(comment)));

        var result = commentService.getTaskComments(30L, "arsh@test.com", pageRequest);

        assertEquals(1, result.getContent().size());
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

    private Task task(Long id, Project project, User creator) {
        Task task = new Task(
                "Build comments",
                "Add comments feature",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.now().plusDays(3)
        );
        setField(task, "id", id);
        task.setProject(project);
        task.setUser(creator);
        return task;
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