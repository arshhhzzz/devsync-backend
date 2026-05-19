package com.arsh.devsync.repository;

import com.arsh.devsync.entity.User;
import com.arsh.devsync.entity.Workspace;
import com.arsh.devsync.entity.WorkspaceMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, Long> {

    Optional<WorkspaceMembership> findByWorkspaceAndUser(Workspace workspace, User user);

    boolean existsByWorkspaceAndUser(Workspace workspace, User user);

    List<WorkspaceMembership> findByUser(User user);

    List<WorkspaceMembership> findByWorkspace(Workspace workspace);
}