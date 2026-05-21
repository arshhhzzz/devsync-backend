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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_shouldCreateWorkspaceAndOwnerMembership() {
        User owner = new User("Arsh", "arsh@test.com", "USER", "hashedPassword");
        setField(owner, "id", 1L);

        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        setField(request, "name", "DevSync");
        setField(request, "description", "Project management backend");

        Workspace savedWorkspace = new Workspace("DevSync", "Project management backend", owner);
        savedWorkspace.setId(10L);

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(savedWorkspace);
        when(membershipRepository.save(any(WorkspaceMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Workspace result = workspaceService.createWorkspace(request, "arsh@test.com");

        assertEquals("DevSync", result.getName());
        assertEquals(owner, result.getOwner());

        verify(workspaceRepository).save(any(Workspace.class));
        verify(membershipRepository).save(argThat(membership ->
                membership.getWorkspace().equals(savedWorkspace)
                        && membership.getUser().equals(owner)
                        && membership.getRole() == WorkspaceRole.OWNER
        ));
        verify(auditLogService).log(
                eq(10L),
                eq("arsh@test.com"),
                eq("WORKSPACE_CREATED"),
                eq("WORKSPACE"),
                eq(10L)
        );
    }

    @Test
    void createWorkspace_shouldThrowException_whenUserNotFound() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        setField(request, "name", "DevSync");
        setField(request, "description", "Project management backend");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> workspaceService.createWorkspace(request, "missing@test.com")
        );

        verify(workspaceRepository, never()).save(any());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateWorkspace_shouldUpdateWorkspace_whenRequesterIsOwner() {
        User owner = new User("Arsh", "arsh@test.com", "USER", "hashedPassword");
        Workspace workspace = new Workspace("Old", "Old description", owner);
        workspace.setId(10L);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                owner,
                WorkspaceRole.OWNER
        );

        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        setField(request, "name", "Updated");
        setField(request, "description", "Updated description");

        when(userRepository.findByEmail("arsh@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(membership));
        when(workspaceRepository.save(workspace)).thenReturn(workspace);

        Workspace result = workspaceService.updateWorkspace(10L, request, "arsh@test.com");

        assertEquals("Updated", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(workspaceRepository).save(workspace);
    }

    @Test
    void updateWorkspace_shouldThrowException_whenRequesterIsMember() {
        User member = new User("Member", "member@test.com", "USER", "hashedPassword");
        User owner = new User("Owner", "owner@test.com", "USER", "hashedPassword");

        Workspace workspace = new Workspace("DevSync", "Backend", owner);
        workspace.setId(10L);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                member,
                WorkspaceRole.MEMBER
        );

        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        setField(request, "name", "Updated");
        setField(request, "description", "Updated description");

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, member))
                .thenReturn(Optional.of(membership));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> workspaceService.updateWorkspace(10L, request, "member@test.com")
        );

        assertEquals("You are not allowed to modify this workspace", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void addMember_shouldAddMember_whenRequesterIsOwner() {
        User owner = new User("Owner", "owner@test.com", "USER", "hashedPassword");
        User userToAdd = new User("Member", "member@test.com", "USER", "hashedPassword");

        Workspace workspace = new Workspace("DevSync", "Backend", owner);
        workspace.setId(10L);

        WorkspaceMembership ownerMembership = new WorkspaceMembership(
                workspace,
                owner,
                WorkspaceRole.OWNER
        );

        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest();
        setField(request, "email", "member@test.com");
        setField(request, "role", WorkspaceRole.MEMBER);

        WorkspaceMembership savedMembership = new WorkspaceMembership(
                workspace,
                userToAdd,
                WorkspaceRole.MEMBER
        );
        setField(savedMembership, "id", 100L);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(ownerMembership));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(userToAdd));
        when(membershipRepository.existsByWorkspaceAndUser(workspace, userToAdd))
                .thenReturn(false);
        when(membershipRepository.save(any(WorkspaceMembership.class)))
                .thenReturn(savedMembership);

        WorkspaceMembership result = workspaceService.addMember(
                10L,
                request,
                "owner@test.com"
        );

        assertEquals(WorkspaceRole.MEMBER, result.getRole());
        assertEquals(userToAdd, result.getUser());

        verify(auditLogService).log(
                eq(10L),
                eq("owner@test.com"),
                eq("MEMBER_ADDED"),
                eq("WORKSPACE_MEMBERSHIP"),
                eq(100L)
        );
    }

    @Test
    void addMember_shouldThrowException_whenUserAlreadyMember() {
        User owner = new User("Owner", "owner@test.com", "USER", "hashedPassword");
        User userToAdd = new User("Member", "member@test.com", "USER", "hashedPassword");

        Workspace workspace = new Workspace("DevSync", "Backend", owner);
        workspace.setId(10L);

        WorkspaceMembership ownerMembership = new WorkspaceMembership(
                workspace,
                owner,
                WorkspaceRole.OWNER
        );

        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest();
        setField(request, "email", "member@test.com");
        setField(request, "role", WorkspaceRole.MEMBER);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(ownerMembership));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(userToAdd));
        when(membershipRepository.existsByWorkspaceAndUser(workspace, userToAdd))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> workspaceService.addMember(10L, request, "owner@test.com")
        );

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void deleteWorkspace_shouldDeleteWorkspace_whenRequesterIsOwner() {
        User owner = new User("Owner", "owner@test.com", "USER", "hashedPassword");
        Workspace workspace = new Workspace("DevSync", "Backend", owner);
        workspace.setId(10L);

        WorkspaceMembership membership = new WorkspaceMembership(
                workspace,
                owner,
                WorkspaceRole.OWNER
        );

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(membershipRepository.findByWorkspaceAndUser(workspace, owner))
                .thenReturn(Optional.of(membership));

        workspaceService.deleteWorkspace(10L, "owner@test.com");

        verify(workspaceRepository).delete(workspace);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}