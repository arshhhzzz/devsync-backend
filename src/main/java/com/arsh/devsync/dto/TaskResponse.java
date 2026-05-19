package com.arsh.devsync.dto;

import com.arsh.devsync.entity.Task;

public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private String status;
    private Long userId;
    private String userEmail;
    private Long projectId;
    private String projectName;

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();

        if (task.getUser() != null) {
            this.userId = task.getUser().getId();
            this.userEmail = task.getUser().getEmail();
        }

        if (task.getProject() != null) {
            this.projectId = task.getProject().getId();
            this.projectName = task.getProject().getName();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }
}