package com.arsh.devsync.dto;

import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.TaskPriority;
import com.arsh.devsync.entity.TaskStatus;

import java.time.LocalDate;

public class TaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private Long userId;
    private String userEmail;
    private Long projectId;
    private String projectName;
    private final TaskPriority priority;
    private final LocalDate dueDate;
    private Long assigneeId;

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
        this.priority = task.getPriority();
        this.dueDate = task.getDueDate();
        if (task.getAssignee() != null) {
            this.assigneeId = task.getAssignee().getId();
        }

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

    public TaskStatus getStatus() {
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

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }
}