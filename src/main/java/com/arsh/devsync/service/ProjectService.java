package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateProjectRequest;
import com.arsh.devsync.dto.UpdateProjectRequest;
import com.arsh.devsync.entity.Project;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.entity.Workspace;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.ProjectRepository;
import com.arsh.devsync.repository.UserRepository;
import com.arsh.devsync.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,  WorkspaceRepository workspaceRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public Project createProject(Long workspaceId, CreateProjectRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (!workspace.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to create project in this workspace");
        }

        Project project = new Project(
                request.getName(),
                request.getDescription()
        );

        project.setWorkspace(workspace);

        return projectRepository.save(project);
    }

    public List<Project> getProjectsByWorkspace(Long workspaceId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (!workspace.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to access this workspace");
        }

        return projectRepository.findByWorkspaceId(workspaceId);
    }

    public Project getProjectById(Long id, String email) {
        return getProjectIfWorkspaceOwner(id, email);
    }

    public Project updateProject(Long id, UpdateProjectRequest request, String email) {
        Project project = getProjectIfWorkspaceOwner(id, email);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return projectRepository.save(project);
    }

    public void deleteProject(Long id, String email) {
        Project project = getProjectIfWorkspaceOwner(id, email);
        projectRepository.delete(project);
    }

    private Project getProjectIfWorkspaceOwner(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.getWorkspace().getOwner().getEmail().equals(email)) {
            throw new RuntimeException("You are not allowed to access this project");
        }

        return project;
    }
}