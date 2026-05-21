package com.arsh.devsync.repository;

import com.arsh.devsync.entity.User;
import com.arsh.devsync.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findByOwner(User owner);

    @Query(value = "SELECT * FROM workspaces WHERE id = :id", nativeQuery = true)
    Optional<Workspace> findByIdIncludingDeleted(@Param("id") Long id);
}