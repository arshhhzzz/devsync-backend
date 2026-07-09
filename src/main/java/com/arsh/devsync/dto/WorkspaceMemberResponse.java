package com.arsh.devsync.dto;

import com.arsh.devsync.entity.WorkspaceMembership;
import com.arsh.devsync.entity.WorkspaceRole;

import java.time.LocalDateTime;

public class WorkspaceMemberResponse {

    private final Long membershipId;
    private final Long userId;
    private final String name;
    private final String email;
    private final WorkspaceRole role;
    private final LocalDateTime joinedAt;

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