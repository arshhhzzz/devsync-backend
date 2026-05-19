package com.arsh.devsync.repository;

import com.arsh.devsync.entity.User;
import com.arsh.devsync.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByOwner(User owner);
}