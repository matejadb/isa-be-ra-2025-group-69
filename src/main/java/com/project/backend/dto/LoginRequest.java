package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request with email and password")
public class LoginRequest {
    @Email(message = "Email needs to be valid")
    @NotBlank(message = "Email is required")
    @Schema(description = "User email address", example = "user@example.com", required = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "MySecurePassword123", required = true)
    private String password;
}
