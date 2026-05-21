package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateProjectRequest;
import com.arsh.devsync.dto.UpdateProjectRequest;
import com.arsh.devsync.entity.*;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final AuditLogService auditLogService;
    private final TaskRepository taskRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            AuditLogService auditLogService,
            TaskRepository taskRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogService = auditLogService;
        this.taskRepository = taskRepository;
    }

    public Project createProject(Long workspaceId, CreateProjectRequest request, String email) {
        WorkspaceMembership membership = getWorkspaceMembership(workspaceId, email);

        validateCanManageProjects(membership);

        Project project = new Project(
                request.getName(),
                request.getDescription()
        );

        project.setWorkspace(membership.getWorkspace());

        Project savedProject = projectRepository.save(project);

        auditLogService.log(
                savedProject.getWorkspace().getId(),
                email,
                "PROJECT_CREATED",
                "PROJECT",
                savedProject.getId()
        );

        return savedProject;
    }

    public Page<Project> getProjectsByWorkspace(
            Long workspaceId,
            String email,
            Pageable pageable
    ) {
        getWorkspaceMembership(workspaceId, email);

        return projectRepository.findByWorkspaceId(workspaceId, pageable);
    }

    public Project getProjectById(Long id, String email) {
        return getProjectIfWorkspaceMember(id, email);
    }

    public Project updateProject(Long id, UpdateProjectRequest request, String email) {
        Project project = getProjectIfCanManage(id, email);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project updatedProject = projectRepository.save(project);

        auditLogService.log(
                updatedProject.getWorkspace().getId(),
                email,
                "PROJECT_UPDATED",
                "PROJECT",
                updatedProject.getId()
        );

        return updatedProject;
    }

    @Transactional
    public void deleteProject(Long id, String email) {
        Project project = getProjectIfCanManage(id, email);

        Long workspaceId = project.getWorkspace().getId();
        Long projectId = project.getId();

        List<Task> tasks = taskRepository.findByProject(project);

        for (Task task : tasks) {
            taskRepository.delete(task);
        }

        projectRepository.delete(project);

        auditLogService.log(
                workspaceId,
                email,
                "PROJECT_DELETED",
                "PROJECT",
                projectId
        );
    }

    @Transactional
    public Project restoreProject(Long id, String email) {
        Project project = projectRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        Workspace workspace = project.getWorkspace();

        if (workspace.getDeletedAt() != null) {
            throw new UnauthorizedActionException("Cannot restore project because workspace is deleted");
        }

        WorkspaceMembership membership = getWorkspaceMembershipIncludingActiveWorkspace(
                workspace,
                email
        );

        validateCanManageProjects(membership);

        project.setDeletedAt(null);

        Project restoredProject = projectRepository.save(project);

        auditLogService.log(
                workspace.getId(),
                email,
                "PROJECT_RESTORED",
                "PROJECT",
                restoredProject.getId()
        );

        return restoredProject;
    }

    private Project getProjectIfWorkspaceMember(Long projectId, String email) {
        Project project = getProjectOrThrow(projectId);

        getWorkspaceMembership(project.getWorkspace().getId(), email);

        return project;
    }

    private Project getProjectIfCanManage(Long projectId, String email) {
        Project project = getProjectOrThrow(projectId);

        WorkspaceMembership membership = getWorkspaceMembership(
                project.getWorkspace().getId(),
                email
        );

        validateCanManageProjects(membership);

        return project;
    }

    private WorkspaceMembership getWorkspaceMembership(Long workspaceId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        return membershipRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new UnauthorizedActionException("You are not a member of this workspace"));
    }

    private WorkspaceMembership getWorkspaceMembershipIncludingActiveWorkspace(
            Workspace workspace,
            String email
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return membershipRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new UnauthorizedActionException("You are not a member of this workspace"));
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private void validateCanManageProjects(WorkspaceMembership membership) {
        if (membership.getRole() != WorkspaceRole.OWNER &&
                membership.getRole() != WorkspaceRole.ADMIN) {
            throw new UnauthorizedActionException("You are not allowed to manage projects in this workspace");
        }
    }
}