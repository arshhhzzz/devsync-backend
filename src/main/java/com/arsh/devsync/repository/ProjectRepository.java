package com.arsh.devsync.repository;

import com.arsh.devsync.entity.Project;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByWorkspaceOwner(User owner);

    List<Project> findByWorkspaceId(Long workspaceId);

    Page<Project> findByWorkspaceId(Long workspaceId, Pageable pageable);

    List<Project> findByWorkspace(Workspace workspace);

    @Query(value = "SELECT * FROM project WHERE id = :id", nativeQuery = true)
    Optional<Project> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT * FROM project WHERE workspace_id = :workspaceId", nativeQuery = true)
    List<Project> findByWorkspaceIdIncludingDeleted(@Param("workspaceId") Long workspaceId);
}