package com.arsh.devsync.repository;

import com.arsh.devsync.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
