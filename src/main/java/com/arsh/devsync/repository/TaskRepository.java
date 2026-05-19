package com.arsh.devsync.repository;

import com.arsh.devsync.entity.Task;
import com.arsh.devsync.entity.TaskStatus;
import com.arsh.devsync.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);

    List<Task> findByProjectId(Long projectId);

    Page<Task> findByUser(User user, Pageable pageable);

    Page<Task> findByUserAndStatus(User user, TaskStatus status, Pageable pageable);

    List<Task> findByAssignee(User assignee);
}