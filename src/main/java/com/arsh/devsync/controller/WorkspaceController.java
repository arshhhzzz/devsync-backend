package com.arsh.devsync.controller;

import com.arsh.devsync.dto.*;
import com.arsh.devsync.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            Authentication authentication
    ) {
        return new WorkspaceResponse(
                workspaceService.createWorkspace(request, authentication.getName())
        );
    }

    @GetMapping("/my")
    public List<WorkspaceResponse> getMyWorkspaces(Authentication authentication) {
        return workspaceService.getMyWorkspaces(authentication.getName())
                .stream()
                .map(WorkspaceResponse::new)
                .toList();
    }

    @GetMapping("/{id:\\d+}")
    public WorkspaceResponse getWorkspaceById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new WorkspaceResponse(
                workspaceService.getWorkspaceById(id, authentication.getName())
        );
    }

    @PutMapping("/{id:\\d+}")
    public WorkspaceResponse updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            Authentication authentication
    ) {
        return new WorkspaceResponse(
                workspaceService.updateWorkspace(id, request, authentication.getName())
        );
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkspace(
            @PathVariable Long id,
            Authentication authentication
    ) {
        workspaceService.deleteWorkspace(id, authentication.getName());
    }

    @PostMapping("/{workspaceId:\\d+}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceMemberResponse addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request,
            Authentication authentication
    ) {
        return new WorkspaceMemberResponse(
                workspaceService.addMember(workspaceId, request, authentication.getName())
        );
    }

    @GetMapping("/{workspaceId:\\d+}/members")
    public Page<WorkspaceMemberResponse> getWorkspaceMembers(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "joinedAt") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            Authentication authentication
    ) {
        Sort.Direction direction = order.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        return workspaceService
                .getWorkspaceMembers(workspaceId, authentication.getName(), pageRequest)
                .map(WorkspaceMemberResponse::new);
    }

    @DeleteMapping("/{workspaceId:\\d+}/members/{userId:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        workspaceService.removeMember(workspaceId, userId, authentication.getName());
    }

    @GetMapping("/{workspaceId:\\d+}/audit-logs")
    public List<AuditLogResponse> getAuditLogs(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        return workspaceService.getAuditLogs(workspaceId, authentication.getName())
                .stream()
                .map(AuditLogResponse::new)
                .toList();
    }
}