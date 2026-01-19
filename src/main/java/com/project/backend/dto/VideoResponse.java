package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Video response containing all video information")
public class VideoResponse {
    @Schema(description = "Unique video identifier", example = "1")
    private Long id;

    @Schema(description = "Video title", example = "My Amazing Video")
    private String title;

    @Schema(description = "Video description", example = "This is a great video about...")
    private String description;

    @Schema(description = "URL path to video file", example = "/api/videos/1/stream")
    private String videoUrl;

    @Schema(description = "URL path to thumbnail image", example = "/api/videos/1/thumbnail")
    private String thumbnailUrl;

    @Schema(description = "List of tags associated with the video", example = "[\"tutorial\", \"coding\", \"java\"]")
    private List<String> tags;

    @Schema(description = "Geographic location (optional)", example = "Belgrade, Serbia")
    private String location;

    @Schema(description = "Video creation timestamp", example = "2025-01-06T15:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "ID of user who uploaded the video", example = "42")
    private Long userId;

    @Schema(description = "Username of video uploader", example = "john_doe")
    private String username;

    @Schema(description = "Number of views", example = "1234")
    private Integer viewCount;

    @Schema(description = "Number of likes", example = "56")
    private Integer likeCount;

    @Schema(description = "Whether current user has liked this video (null if not authenticated)", example = "true")
    private Boolean isLikedByCurrentUser;

    @Schema(description = "Scheduled release date/time (null if not scheduled)", example = "2026-01-20T08:00:00")
    private LocalDateTime scheduledAt;

    @Schema(description = "Video duration in seconds", example = "300")
    private Long durationInSeconds;

    @Schema(description = "Current playback offset in seconds for scheduled videos (calculated from scheduled time)", example = "180")
    private Long currentPlaybackOffsetSeconds;

    @Schema(description = "Whether the video is available (scheduled videos become available at scheduledAt time)", example = "true")
    private Boolean isAvailable;
}
