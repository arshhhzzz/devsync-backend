package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CommentResponse;
import com.arsh.devsync.dto.CreateCommentRequest;
import com.arsh.devsync.dto.UpdateCommentRequest;
import com.arsh.devsync.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/tasks/{taskId}/comments")
    public CommentResponse addCommentToTask(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        return new CommentResponse(
                commentService.addCommentToTask(
                        taskId,
                        request,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/tasks/{taskId}/comments")
    public Page<CommentResponse> getTaskComments(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            Authentication authentication
    ) {
        PageRequest pageRequest = buildPageRequest(page, size, sortBy, order);

        return commentService
                .getTaskComments(taskId, authentication.getName(), pageRequest)
                .map(CommentResponse::new);
    }

    @PostMapping("/projects/{projectId}/comments")
    public CommentResponse addCommentToProject(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        return new CommentResponse(
                commentService.addCommentToProject(
                        projectId,
                        request,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/projects/{projectId}/comments")
    public Page<CommentResponse> getProjectComments(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            Authentication authentication
    ) {
        PageRequest pageRequest = buildPageRequest(page, size, sortBy, order);

        return commentService
                .getProjectComments(projectId, authentication.getName(), pageRequest)
                .map(CommentResponse::new);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication
    ) {
        return new CommentResponse(
                commentService.updateComment(
                        commentId,
                        request,
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        commentService.deleteComment(commentId, authentication.getName());
    }

    private PageRequest buildPageRequest(
            int page,
            int size,
            String sortBy,
            String order
    ) {
        Sort.Direction direction = order.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );
    }
}