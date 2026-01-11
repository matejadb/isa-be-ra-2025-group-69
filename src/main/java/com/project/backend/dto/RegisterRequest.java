package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class RegisterRequest {
    @Email(message = "Email needs to be valid")
    @NotBlank(message = "Email is required")
    @Schema(description = "User email address", example = "user@example.com", required = true)
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Schema(description = "Username (3-20 characters)", example = "john_doe", required = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Password (min 8 characters)", example = "SecurePass123", required = true)
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Password confirmation (must match password)", example = "SecurePass123", required = true)
    private String confirmPassword;

    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "John", required = true)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "User's last name", example = "Doe", required = true)
    private String lastName;

    @NotNull(message = "Address is required")
    @Valid
    @Schema(description = "User's address", required = true)
    private AddressDTO address;
}
