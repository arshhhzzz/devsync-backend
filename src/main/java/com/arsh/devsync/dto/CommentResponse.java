package com.arsh.devsync.dto;

import com.arsh.devsync.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        Long authorId,
        String authorEmail,
        Long taskId,
        Long projectId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public CommentResponse(Comment comment) {
        this(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getId(),
                comment.getAuthor().getEmail(),
                comment.getTask() != null ? comment.getTask().getId() : null,
                comment.getProject() != null ? comment.getProject().getId() : null,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}