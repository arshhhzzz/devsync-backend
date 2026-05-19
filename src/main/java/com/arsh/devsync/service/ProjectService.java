package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateProjectRequest;
import com.arsh.devsync.dto.UpdateProjectRequest;
import com.arsh.devsync.entity.Project;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.ProjectRepository;
import com.arsh.devsync.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project createProject(CreateProjectRequest request, String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = new Project(
                request.getName(),
                request.getDescription()
        );
        project.setOwner(owner);

        return projectRepository.save(project);
    }

    public List<Project> getMyProjects(String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return projectRepository.findByOwner(owner);
    }

    public Project getProjectById(Long id, String email) {
        return getProjectIfOwner(id, email);
    }

    public Project updateProject(Long id, UpdateProjectRequest request, String email) {
        Project project = getProjectIfOwner(id, email);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return projectRepository.save(project);
    }

    public void deleteProject(Long id, String email) {
        Project project = getProjectIfOwner(id, email);
        projectRepository.delete(project);
    }

    private Project getProjectIfOwner(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.getOwner().getEmail().equals(email)) {
            throw new RuntimeException("You are not allowed to access this project");
        }

        return project;
    }
}