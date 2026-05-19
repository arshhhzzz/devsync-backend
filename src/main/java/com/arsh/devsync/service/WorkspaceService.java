package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateWorkspaceRequest;
import com.arsh.devsync.dto.UpdateWorkspaceRequest;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.entity.Workspace;
import com.arsh.devsync.entity.WorkspaceMembership;
import com.arsh.devsync.entity.WorkspaceRole;
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

    public WorkspaceService(WorkspaceRepository workspaceRepository, UserRepository userRepository, WorkspaceMembershipRepository membershipRepository) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    private WorkspaceMembership getMembership(Long workspaceId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        return membershipRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new RuntimeException("You are not a member of this workspace"));
    }

    public Workspace createWorkspace(CreateWorkspaceRequest request, String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return membershipRepository.findByUser(user)
                .stream()
                .map(WorkspaceMembership::getWorkspace)
                .toList();
    }

    public Workspace getWorkspaceById(Long id, String email) {
        return getWorkspaceIfOwner(id, email);
    }

    public Workspace updateWorkspace(Long id, UpdateWorkspaceRequest request, String email) {
        Workspace workspace = getWorkspaceIfOwner(id, email);

        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());

        return workspaceRepository.save(workspace);
    }

    public void deleteWorkspace(Long id, String email) {
        Workspace workspace = getWorkspaceIfOwner(id, email);
        workspaceRepository.delete(workspace);
    }

    private Workspace getWorkspaceIfOwner(Long workspaceId, String email) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        if (!workspace.getOwner().getEmail().equals(email)) {
            throw new RuntimeException("You are not allowed to access this workspace");
        }

        return workspace;
    }
}