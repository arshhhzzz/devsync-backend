package com.arsh.devsync.service;

import com.arsh.devsync.dto.*;
import com.arsh.devsync.entity.RefreshToken;
import com.arsh.devsync.entity.User;
import com.arsh.devsync.exception.DuplicateResourceException;
import com.arsh.devsync.exception.UnauthorizedActionException;
import com.arsh.devsync.repository.UserRepository;
import com.arsh.devsync.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse signup(SignupRequest signupRequest) {
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(signupRequest.getPassword());

        User user = new User(
                signupRequest.getName(),
                signupRequest.getEmail(),
                "USER",
                hashedPassword
        );

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateToken(savedUser.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(savedUser).getToken();

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedActionException("Invalid email or password"));

        boolean isPasswordCorrect = passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword()
        );

        if (!isPasswordCorrect) {
            throw new UnauthorizedActionException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(
                request.refreshToken()
        );

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getEmail());
        String newRefreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    public void logout(LogoutRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(
                request.refreshToken()
        );

        refreshTokenService.deleteByUser(refreshToken.getUser());
    }
}