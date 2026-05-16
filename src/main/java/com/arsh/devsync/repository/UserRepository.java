package com.arsh.devsync.repository;

import com.arsh.devsync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}