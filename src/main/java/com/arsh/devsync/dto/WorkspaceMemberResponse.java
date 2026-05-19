package com.arsh.devsync.dto;

import com.arsh.devsync.entity.WorkspaceMembership;
import com.arsh.devsync.entity.WorkspaceRole;

import java.time.LocalDateTime;

public class WorkspaceMemberResponse {

    private Long membershipId;
    private Long userId;
    private String name;
    private String email;
    private WorkspaceRole role;
    private LocalDateTime joinedAt;

    public WorkspaceMemberResponse(WorkspaceMembership membership) {
        this.membershipId = membership.getId();
        this.userId = membership.getUser().getId();
        this.name = membership.getUser().getName();
        this.email = membership.getUser().getEmail();
        this.role = membership.getRole();
        this.joinedAt = membership.getJoinedAt();
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}