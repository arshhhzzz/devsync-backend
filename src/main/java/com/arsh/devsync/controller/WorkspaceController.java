package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateWorkspaceRequest;
import com.arsh.devsync.dto.UpdateWorkspaceRequest;
import com.arsh.devsync.dto.WorkspaceResponse;
import com.arsh.devsync.service.WorkspaceService;
import jakarta.validation.Valid;
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
    public void deleteWorkspace(
            @PathVariable Long id,
            Authentication authentication
    ) {
        workspaceService.deleteWorkspace(id, authentication.getName());
    }
}