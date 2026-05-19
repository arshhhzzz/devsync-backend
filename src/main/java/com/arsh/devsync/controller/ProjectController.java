package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateProjectRequest;
import com.arsh.devsync.dto.ProjectResponse;
import com.arsh.devsync.dto.UpdateProjectRequest;
import com.arsh.devsync.entity.Project;
import com.arsh.devsync.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
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
    public List<ProjectResponse> getProjectsByWorkspace(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        return projectService.getProjectsByWorkspace(workspaceId, authentication.getName())
                .stream()
                .map(ProjectResponse::new)
                .toList();
    }

    @GetMapping("/{id:\\d+}")
    public ProjectResponse getProjectById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new ProjectResponse(
                projectService.getProjectById(id, authentication.getName())
        );
    }

    @PutMapping("/{id:\\d+}")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication
    ) {
        return new ProjectResponse(
                projectService.updateProject(id, request, authentication.getName())
        );
    }

    @DeleteMapping("/{id:\\d+}")
    public void deleteProject(
            @PathVariable Long id,
            Authentication authentication
    ) {
        projectService.deleteProject(id, authentication.getName());
    }
}