package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateCommentRequest;
import com.arsh.devsync.dto.UpdateCommentRequest;
import com.arsh.devsync.entity.*;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final AuditLogService auditLogService;

    public CommentService(
            CommentRepository commentRepository,
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            WorkspaceMembershipRepository membershipRepository,
            AuditLogService auditLogService
    ) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogService = auditLogService;
    }

    public Comment addCommentToTask(
            Long taskId,
            CreateCommentRequest request,
            String email
    ) {
        User author = getUserByEmail(email);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        Workspace workspace = task.getProject().getWorkspace();

        ensureWorkspaceMember(workspace, author);

        Comment comment = new Comment(
                request.content(),
                author,
                task,
                null
        );

        Comment savedComment = commentRepository.save(comment);

        auditLogService.log(
                workspace.getId(),
                email,
                "COMMENT_ADDED_TO_TASK",
                "COMMENT",
                savedComment.getId()
        );

        return savedComment;
    }

    public Comment addCommentToProject(
            Long projectId,
            CreateCommentRequest request,
            String email
    ) {
        User author = getUserByEmail(email);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Workspace workspace = project.getWorkspace();

        ensureWorkspaceMember(workspace, author);

        Comment comment = new Comment(
                request.content(),
                author,
                null,
                project
        );

        Comment savedComment = commentRepository.save(comment);

        auditLogService.log(
                workspace.getId(),
                email,
                "COMMENT_ADDED_TO_PROJECT",
                "COMMENT",
                savedComment.getId()
        );

        return savedComment;
    }

    public Page<Comment> getTaskComments(
            Long taskId,
            String email,
            Pageable pageable
    ) {
        User user = getUserByEmail(email);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        ensureWorkspaceMember(task.getProject().getWorkspace(), user);

        return commentRepository.findByTask(task, pageable);
    }

    public Page<Comment> getProjectComments(
            Long projectId,
            String email,
            Pageable pageable
    ) {
        User user = getUserByEmail(email);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        ensureWorkspaceMember(project.getWorkspace(), user);

        return commentRepository.findByProject(project, pageable);
    }

    @Transactional
    public Comment updateComment(
            Long commentId,
            UpdateCommentRequest request,
            String email
    ) {
        User user = getUserByEmail(email);

        Comment comment = getCommentOrThrow(commentId);

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Only comment author can update this comment");
        }

        comment.setContent(request.content());

        Comment updatedComment = commentRepository.save(comment);

        auditLogService.log(
                getWorkspaceFromComment(comment).getId(),
                email,
                "COMMENT_UPDATED",
                "COMMENT",
                updatedComment.getId()
        );

        return updatedComment;
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        User user = getUserByEmail(email);

        Comment comment = getCommentOrThrow(commentId);

        Workspace workspace = getWorkspaceFromComment(comment);

        WorkspaceMembership membership = ensureWorkspaceMember(workspace, user);

        boolean isAuthor = comment.getAuthor().getId().equals(user.getId());
        boolean isOwnerOrAdmin =
                membership.getRole() == WorkspaceRole.OWNER ||
                        membership.getRole() == WorkspaceRole.ADMIN;

        if (!isAuthor && !isOwnerOrAdmin) {
            throw new UnauthorizedActionException("You are not allowed to delete this comment");
        }

        Long deletedCommentId = comment.getId();

        commentRepository.delete(comment);

        auditLogService.log(
                workspace.getId(),
                email,
                "COMMENT_DELETED",
                "COMMENT",
                deletedCommentId
        );
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
    }

    private Workspace getWorkspaceFromComment(Comment comment) {
        if (comment.getTask() != null) {
            return comment.getTask().getProject().getWorkspace();
        }

        if (comment.getProject() != null) {
            return comment.getProject().getWorkspace();
        }

        throw new IllegalStateException("Comment is not linked to a task or project");
    }

    private WorkspaceMembership ensureWorkspaceMember(Workspace workspace, User user) {
        return membershipRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new UnauthorizedActionException("You are not a member of this workspace"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}