package com.arsh.devsync.dto;

import com.arsh.devsync.entity.Project;

public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerEmail;

    public ProjectResponse(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();

        if (project.getOwner() != null) {
            this.ownerId = project.getOwner().getId();
            this.ownerEmail = project.getOwner().getEmail();
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

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }
}