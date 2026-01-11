package com.project.backend.service;

import com.project.backend.dto.AuthResponse;
import com.project.backend. dto.LoginRequest;
import com.project.backend.dto.RegisterRequest;
import com.project. backend.model.User;
import com.project.backend.model. Address;
import com.project. backend.repository.UserRepository;
import com.project.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security. crypto.password.PasswordEncoder;
import org.springframework.stereotype. Service;
import org.springframework. transaction.annotation.Transactional;

import java.util.Optional;
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
            throw new RuntimeException("Passwords do not match");
        }

        // Check if email or username already exists
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already in use");
        }

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already in use");
        }

        // Create address string
        Address address = new Address();
        address.setStreet(request.getAddress().getStreet());
        address.setCity(request.getAddress().getCity());
        address.setPostalCode(request. getAddress().getPostalCode());
        address.setCountry(request.getAddress().getCountry());

        // Create new user
        User user = new User();
        user.setEmail(request. getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request. getLastName());
        user.setAddress(address);
        user.setActivated(false);
        user.setActivationToken(UUID.randomUUID().toString());

        // Save user to database
        userRepository.save(user);

        // Send activation email
        emailService.sendActivationEmail(user.getEmail(), user.getActivationToken());
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        System.out.println("=== LOGIN START ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("Password length: " + request.getPassword().length());

        // Rate limiting
        System.out.println("Checking rate limit...");
        if(!rateLimitService.isAllowed(ipAddress)) {
            System.out.println("Rate limit exceeded!");
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }
        System. out.println("Rate limit OK");

        // Find user by email
        System.out.println("Searching for user by email:  " + request.getEmail());
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        System.out.println("User found: " + userOptional.isPresent());

        if (! userOptional.isPresent()) {
            System.out.println("User NOT found in database!");
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOptional.get();
        System.out.println("User loaded - ID: " + user.getId() + ", Username: " + user. getUsername());
        System.out.println("User activated: " + user.isActivated());
        System.out.println("User has address: " + (user.getAddress() != null));

        // Check if user is activated
        if(!user.isActivated()) {
            System.out. println("User is NOT activated!");
            throw new RuntimeException("Account not activated. Please check your email.");
        }
        System.out.println("Activation check OK");

        // Check password
        System.out.println("Checking password...");
        System.out.println("Input password: " + request.getPassword());
        System.out.println("Stored password hash: " + user.getPassword().substring(0, 20) + "...");

        boolean passwordMatch = passwordEncoder. matches(request.getPassword(), user.getPassword());
        System.out.println("Password match result: " + passwordMatch);

        if(!passwordMatch) {
            System.out.println("Password INCORRECT!");
            throw new RuntimeException("Invalid email or password");
        }
        System.out.println("Password OK");

        System.out.println("Resetting rate limit attempts...");
        rateLimitService.resetAttempts(ipAddress);

        System.out.println("Generating JWT token...");
        String token = jwtUtils.generateToken(user.getEmail());
        System.out.println("Token generated successfully");
        System.out.println("=== LOGIN SUCCESS ===");

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    @Transactional
    public void activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid activation token"));

        user.setActivated(true);
        user.setActivationToken(null);
        userRepository. save(user);
    }
}