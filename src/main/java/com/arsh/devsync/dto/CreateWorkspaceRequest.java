package com.arsh.devsync.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    private String name;

    private String description;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}