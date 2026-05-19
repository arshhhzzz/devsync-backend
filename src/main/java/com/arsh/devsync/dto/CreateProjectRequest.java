package com.arsh.devsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotNull(message = "Workspace id is required")
    private Long workspaceId;

    private String description;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}