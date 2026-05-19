package com.arsh.devsync.dto;

import com.arsh.devsync.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddWorkspaceMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    @NotNull(message = "Role is required")
    private WorkspaceRole role;

    public String getEmail() {
        return email;
    }

    public WorkspaceRole getRole() {
        return role;
    }
}