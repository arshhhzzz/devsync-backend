package com.arsh.devsync.dto;

import com.arsh.devsync.entity.Workspace;

public class WorkspaceResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerEmail;

    public WorkspaceResponse(Workspace workspace) {
        this.id = workspace.getId();
        this.name = workspace.getName();
        this.description = workspace.getDescription();

        if (workspace.getOwner() != null) {
            this.ownerId = workspace.getOwner().getId();
            this.ownerEmail = workspace.getOwner().getEmail();
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerEmail() { return ownerEmail; }
}