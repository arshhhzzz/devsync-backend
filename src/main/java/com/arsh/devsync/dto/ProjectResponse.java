package com.arsh.devsync.dto;

import com.arsh.devsync.entity.Project;

public class ProjectResponse {

    private Long id;
    private String name;
    private String description;

    private Long workspaceId;
    private String workspaceName;

    private Long workspaceOwnerId;
    private String workspaceOwnerEmail;

    public ProjectResponse(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();

        if (project.getWorkspace() != null) {

            this.workspaceId = project.getWorkspace().getId();
            this.workspaceName = project.getWorkspace().getName();

            if (project.getWorkspace().getOwner() != null) {
                this.workspaceOwnerId = project.getWorkspace().getOwner().getId();
                this.workspaceOwnerEmail = project.getWorkspace().getOwner().getEmail();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public Long getWorkspaceOwnerId() {
        return workspaceOwnerId;
    }

    public String getWorkspaceOwnerEmail() {
        return workspaceOwnerEmail;
    }
}