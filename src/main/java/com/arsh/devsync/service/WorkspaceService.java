package com.arsh.devsync.service;

import com.arsh.devsync.dto.AddWorkspaceMemberRequest;
import com.arsh.devsync.dto.CreateWorkspaceRequest;
import com.arsh.devsync.dto.UpdateWorkspaceRequest;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.entity.Workspace;
import com.arsh.devsync.entity.WorkspaceMembership;
import com.arsh.devsync.entity.WorkspaceRole;
import com.arsh.devsync.exception.DuplicateResourceException;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.UserRepository;
import com.arsh.devsync.repository.WorkspaceMembershipRepository;
import com.arsh.devsync.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            WorkspaceMembershipRepository membershipRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
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

    public void deleteWorkspace(Long id, String email) {
        Workspace workspace = getWorkspaceIfOwner(id, email);
        workspaceRepository.delete(workspace);
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

        return membershipRepository.save(membership);
    }

    public List<WorkspaceMembership> getWorkspaceMembers(Long workspaceId, String email) {
        Workspace workspace = getWorkspaceIfMember(workspaceId, email);
        return membershipRepository.findByWorkspace(workspace);
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

        membershipRepository.delete(targetMembership);
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
}