package com.project.backend.service;

import com.project.backend.dto.AuthResponse;
import com.project.backend.dto.LoginRequest;
import com.project.backend.dto.RegisterRequest;
import com.project.backend.model.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    @Transactional
    public void register(RegisterRequest request) {
        // Check if passwords match
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("PAsswords do not match");
        }

        // Check if email or username already exists
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already in use");
        }

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already in use");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());
        user.setActivated(false);
        user.setActivationToken(UUID.randomUUID().toString());

        // Save user to database
        userRepository.save(user);

        // Send activation email
        emailService.sendActivationEmail(user.getEmail(), user.getActivationToken());
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        // Rate limiting
        if(!rateLimitService.isAllowed(ipAddress)) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check if user is activated
        if(!user.isActivated()) {
            throw new RuntimeException("Account not activated. Please check your email.");
        }

        // Check password
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        rateLimitService.resetAttempts(ipAddress);

        String token = jwtUtils.generateToken(user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    @Transactional
    public void activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid activation token"));

        user.setActivated(true);
        user.setActivationToken(null);
        userRepository.save(user);
    }
}
