package com.arsh.devsync.controller;

import com.arsh.devsync.dto.AuthResponse;
import com.arsh.devsync.dto.LoginRequest;
import com.arsh.devsync.dto.SignupRequest;
import com.arsh.devsync.dto.UserResponse;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return new UserResponse(authService.signup(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}