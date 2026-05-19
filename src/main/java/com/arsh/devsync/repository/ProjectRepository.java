package com.arsh.devsync.repository;

import com.arsh.devsync.entity.Project;
import com.arsh.devsync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByWorkspaceOwner(User owner);

    List<Project> findByWorkspaceId(Long workspaceId);
}
