package com.project.backend.controller;

import com.project.backend.dto.AuthResponse;
import com.project.backend.dto.LoginRequest;
import com.project.backend.dto.RegisterRequest;
import com.project.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Register a new user account. An activation email will be sent to the provided email address."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful, check email for activation"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already exists")
    })
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            authService.register(request);
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully. Please check your email to activate your account."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticate user and receive JWT access token. Rate limited to prevent brute force attacks."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or account not activated")
    })
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            String ipAddress = getClientIP(httpRequest);
            AuthResponse response = authService.login(request, ipAddress);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/activate")
    @Operation(
            summary = "Activate user account",
            description = "Activate a user account using the token sent via email during registration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account activated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired activation token")
    })
    public ResponseEntity<?> activateAccount(
            @Parameter(description = "Activation token from email", required = true)
            @RequestParam String token
    ) {
        try{
            authService.activateAccount(token);
            return ResponseEntity.ok(Map.of(
                    "message", "Account activated successfully. You can now log in."));
        }catch(Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }


}
