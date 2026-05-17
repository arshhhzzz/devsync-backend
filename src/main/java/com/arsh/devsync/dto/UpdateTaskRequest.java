package com.arsh.devsync.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateTaskRequest {

    @NotBlank(message="Title is required")
    private String title;

    private String description;

    @NotBlank(message="Status is required")
    private String status;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}
