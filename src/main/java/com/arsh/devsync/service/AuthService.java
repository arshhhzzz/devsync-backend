package com.arsh.devsync.service;

import com.arsh.devsync.dto.LoginRequest;
import com.arsh.devsync.dto.SignupRequest;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(SignupRequest signupRequest) {
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        String hashedPassword = passwordEncoder.encode(signupRequest.getPassword());

        User user = new User(
                signupRequest.getName(),
                signupRequest.getEmail(),
                "User",
                hashedPassword
        );
        return userRepository.save(user);
    }

    public String login(LoginRequest loginRequest) {
        User  user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        boolean isPasswordCorrect = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());

        if (!isPasswordCorrect) {
            throw  new RuntimeException("Invalid email or password");
        }

        return "Login Successful";
    }

}
