package com.project.backend.dto;

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

    @Schema(description = "User ID")
    private Long id;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Profile picture URL")
    private String profilePictureUrl;

    @Schema(description = "User bio")
    private String bio;

    @Schema(description = "Account creation date")
    private LocalDateTime createdAt;

    @Schema(description = "Total number of posts")
    private int postCount;
}