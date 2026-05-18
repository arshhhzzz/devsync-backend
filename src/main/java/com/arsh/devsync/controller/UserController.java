package com.arsh.devsync.controller;

import com.arsh.devsync.dto.CreateUserRequest;
import com.arsh.devsync.dto.UserResponse;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return new UserResponse(userService.createUser(request));
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers()
                .stream()
                .map(UserResponse::new)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return new UserResponse(userService.getUserById(id));
    }
}