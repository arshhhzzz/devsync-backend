package com.arsh.devsync.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}