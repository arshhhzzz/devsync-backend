package com.arsh.devsync.service;

import com.arsh.devsync.dto.AddWorkspaceMemberRequest;
import com.arsh.devsync.dto.CreateWorkspaceRequest;
import com.arsh.devsync.dto.UpdateWorkspaceRequest;
import com.arsh.devsync.entity.*;
import com.arsh.devsync.exception.DuplicateResourceException;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final AuditLogService auditLogService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            WorkspaceMembershipRepository membershipRepository,
            AuditLogService auditLogService,
            ProjectRepository projectRepository,
            TaskRepository taskRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogService = auditLogService;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    public Workspace createWorkspace(CreateWorkspaceRequest request, String email) {
        User owner = getUserByEmail(email);

        Workspace workspace = new Workspace(
                request.getName(),
                request.getDescription(),
                owner
        );

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMembership membership = new WorkspaceMembership(
                savedWorkspace,
                owner,
                WorkspaceRole.OWNER
        );

        membershipRepository.save(membership);

        auditLogService.log(
                savedWorkspace.getId(),
                email,
                "WORKSPACE_CREATED",
                "WORKSPACE",
                savedWorkspace.getId()
        );

        return savedWorkspace;
    }

    public List<Workspace> getMyWorkspaces(String email) {
        User user = getUserByEmail(email);

        return membershipRepository.findByUser(user)
                .stream()
                .map(WorkspaceMembership::getWorkspace)
                .toList();
    }

    public Workspace getWorkspaceById(Long id, String email) {
        return getWorkspaceIfMember(id, email);
    }

    public Workspace updateWorkspace(Long id, UpdateWorkspaceRequest request, String email) {
        Workspace workspace = getWorkspaceIfAdminOrOwner(id, email);

        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());

        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void deleteWorkspace(Long id, String email) {
        Workspace workspace = getWorkspaceIfOwner(id, email);

        List<Project> projects = projectRepository.findByWorkspace(workspace);

        for (Project project : projects) {
            List<Task> tasks = taskRepository.findByProject(project);

            taskRepository.deleteAll(tasks);

            projectRepository.delete(project);
        }

        workspaceRepository.delete(workspace);

        auditLogService.log(
                workspace.getId(),
                email,
                "WORKSPACE_DELETED",
                "WORKSPACE",
                workspace.getId()
        );
    }

    public WorkspaceMembership addMember(Long workspaceId, AddWorkspaceMemberRequest request, String email) {
        Workspace workspace = getWorkspaceIfAdminOrOwner(workspaceId, email);

        if (request.getRole() == WorkspaceRole.OWNER) {
            throw new RuntimeException("Cannot add another OWNER to workspace");
        }

        User userToAdd = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (membershipRepository.existsByWorkspaceAndUser(workspace, userToAdd)) {
            throw new DuplicateResourceException("User is already a member of this workspace");
        }

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                userToAdd,
                request.getRole()
        );

        WorkspaceMembership savedMembership = membershipRepository.save(membership);

        auditLogService.log(
                workspace.getId(),
                email,
                "MEMBER_ADDED",
                "WORKSPACE_MEMBERSHIP",
                savedMembership.getId()
        );

        return savedMembership;
    }

    public Page<WorkspaceMembership> getWorkspaceMembers(
            Long workspaceId,
            String email,
            Pageable pageable
    ) {
        Workspace workspace = getWorkspaceIfMember(workspaceId, email);
        return membershipRepository.findByWorkspace(workspace, pageable);
    }

    public void removeMember(Long workspaceId, Long userId, String email) {
        WorkspaceMembership requesterMembership = getMembership(workspaceId, email);

        if (requesterMembership.getRole() != WorkspaceRole.OWNER &&
                requesterMembership.getRole() != WorkspaceRole.ADMIN) {
            throw new RuntimeException("You are not allowed to remove members");
        }

        Workspace workspace = requesterMembership.getWorkspace();

        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        WorkspaceMembership targetMembership = membershipRepository.findByWorkspaceAndUser(workspace, userToRemove)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this workspace"));

        if (targetMembership.getRole() == WorkspaceRole.OWNER) {
            throw new RuntimeException("Workspace OWNER cannot be removed");
        }

        if (requesterMembership.getRole() == WorkspaceRole.ADMIN &&
                targetMembership.getRole() == WorkspaceRole.ADMIN) {
            throw new RuntimeException("ADMIN cannot remove another ADMIN");
        }

        Long membershipId = targetMembership.getId();

        membershipRepository.delete(targetMembership);

        auditLogService.log(
                workspace.getId(),
                email,
                "MEMBER_REMOVED",
                "WORKSPACE_MEMBERSHIP",
                membershipId
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Workspace getWorkspaceOrThrow(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
    }

    private WorkspaceMembership getMembership(Long workspaceId, String email) {
        User user = getUserByEmail(email);
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        return membershipRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new RuntimeException("You are not a member of this workspace"));
    }

    private Workspace getWorkspaceIfMember(Long workspaceId, String email) {
        return getMembership(workspaceId, email).getWorkspace();
    }

    private Workspace getWorkspaceIfAdminOrOwner(Long workspaceId, String email) {
        WorkspaceMembership membership = getMembership(workspaceId, email);

        if (membership.getRole() != WorkspaceRole.OWNER &&
                membership.getRole() != WorkspaceRole.ADMIN) {
            throw new RuntimeException("You are not allowed to modify this workspace");
        }

        return membership.getWorkspace();
    }

    private Workspace getWorkspaceIfOwner(Long workspaceId, String email) {
        WorkspaceMembership membership = getMembership(workspaceId, email);

        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new RuntimeException("Only workspace owner can perform this action");
        }

        return membership.getWorkspace();
    }

    public List<AuditLog> getAuditLogs(Long workspaceId, String email) {
        getWorkspaceIfAdminOrOwner(workspaceId, email);
        return auditLogService.getAuditLogsByWorkspace(workspaceId);
    }

    @Transactional
    public Workspace restoreWorkspace(Long id, String email) {
        Workspace workspace = workspaceRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));

        if (!workspace.getOwner().getEmail().equals(email)) {
            throw new RuntimeException("Only workspace owner can restore this workspace");
        }

        workspace.setDeletedAt(null);

        Workspace restoredWorkspace = workspaceRepository.save(workspace);

        auditLogService.log(
                restoredWorkspace.getId(),
                email,
                "WORKSPACE_RESTORED",
                "WORKSPACE",
                restoredWorkspace.getId()
        );

        return restoredWorkspace;
    }
}