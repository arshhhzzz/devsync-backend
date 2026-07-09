package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateProjectRequest;
import com.arsh.devsync.dto.ProjectResponse;
import com.arsh.devsync.dto.UpdateProjectRequest;
import com.arsh.devsync.entity.Project;
import com.arsh.devsync.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/workspaces/{workspaceId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication
    ) {
        Project project = projectService.createProject(
                workspaceId,
                request,
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProjectResponse(project));
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public Page<ProjectResponse> getProjectsByWorkspace(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
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

        return projectService
                .getProjectsByWorkspace(workspaceId, authentication.getName(), pageRequest)
                .map(ProjectResponse::new);
    }

    @GetMapping("/projects/{id:\\d+}")
    public ProjectResponse getProjectById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new ProjectResponse(
                projectService.getProjectById(id, authentication.getName())
        );
    }

    @PutMapping("/projects/{id:\\d+}")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication
    ) {
        return new ProjectResponse(
                projectService.updateProject(id, request, authentication.getName())
        );
    }

    @DeleteMapping("/projects/{id:\\d+}")
    public void deleteProject(
            @PathVariable Long id,
            Authentication authentication
    ) {
        projectService.deleteProject(id, authentication.getName());
    }

    @PostMapping("/projects/{id}/restore")
    public ProjectResponse restoreProject(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new ProjectResponse(
                projectService.restoreProject(id, authentication.getName())
        );
    }
}