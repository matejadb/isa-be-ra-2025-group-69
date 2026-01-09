package com.project.backend. dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public user profile information")
public class PublicProfileResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "johndoe")
    private String username;

    @Schema(description = "Profile picture URL", example = "https://example.com/avatar.jpg")
    private String profilePictureUrl;

    @Schema(description = "User bio/description", example = "Software developer")
    private String bio;

    @Schema(description = "Account creation date", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Total number of posts", example = "42")
    private int postCount;
}