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
@Schema(description = "Post data transfer object")
public class PostResponse {

    @Schema(description = "Post ID")
    private Long id;

    @Schema(description = "Post content")
    private String content;

    @Schema(description = "Post creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Author information")
    private UserSummary author;

    @Schema(description = "Number of likes")
    private int likeCount;

    @Schema(description = "Number of comments")
    private int commentCount;

    @Schema(description = "Whether current user liked this post (null for unauthenticated)")
    private Boolean likedByCurrentUser;
}