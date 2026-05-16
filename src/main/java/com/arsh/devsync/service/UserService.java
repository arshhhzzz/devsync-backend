package com.arsh.devsync.service;

import com.arsh.devsync.dto.CreateUserRequest;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.ResourceNotFoundException;
import com.arsh.devsync.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(CreateUserRequest request) {
        User user = new User(
                request.getName(),
                request.getEmail(),
                request.getRole()
        );

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}