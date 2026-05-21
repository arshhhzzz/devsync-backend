package com.arsh.devsync.repository;

import com.arsh.devsync.entity.Comment;
import com.arsh.devsync.entity.Project;
import com.arsh.devsync.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByTask(Task task, Pageable pageable);

    Page<Comment> findByProject(Project project, Pageable pageable);
}