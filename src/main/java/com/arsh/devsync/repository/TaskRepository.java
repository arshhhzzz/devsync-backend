package com.arsh.devsync.repository;

import com.arsh.devsync.entity.Project;
import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.TaskStatus;
import com.arsh.devsync.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);

    List<Task> findByProjectId(Long projectId);

    Page<Task> findByUser(User user, Pageable pageable);

    Page<Task> findByUserAndStatus(User user, TaskStatus status, Pageable pageable);

    List<Task> findByAssignee(User assignee);

    List<Task> findByProject(Project project);

    @Query(value = "SELECT * FROM tasks WHERE id = :id", nativeQuery = true)
    Optional<Task> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT * FROM tasks WHERE project_id = :projectId", nativeQuery = true)
    List<Task> findByProjectIdIncludingDeleted(@Param("projectId") Long projectId);

    @Query(value = "SELECT project_id FROM tasks WHERE id = :taskId", nativeQuery = true)
    Long findProjectIdIncludingDeletedTask(@Param("taskId") Long taskId);
}