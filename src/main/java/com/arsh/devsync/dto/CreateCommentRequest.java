package com.arsh.devsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String content
) {
}