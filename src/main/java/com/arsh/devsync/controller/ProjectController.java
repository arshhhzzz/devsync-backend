package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateProjectRequest;
import com.arsh.devsync.dto.ProjectResponse;
import com.arsh.devsync.dto.UpdateProjectRequest;
import com.arsh.devsync.service.ProjectService;
import jakarta.validation.Valid;
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

    @PostMapping
    public ProjectResponse createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication
    ) {
        return new ProjectResponse(
                projectService.createProject(request, authentication.getName())
        );
    }

    @GetMapping("/my")
    public List<ProjectResponse> getMyProjects(Authentication authentication) {
        return projectService.getMyProjects(authentication.getName())
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