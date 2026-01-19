package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Video streaming response with time synchronization info")
public class VideoStreamResponse {
    @Schema(description = "Video ID", example = "1")
    private Long videoId;

    @Schema(description = "Video title", example = "My Scheduled Video")
    private String title;

    @Schema(description = "Current playback offset in seconds (for scheduled videos)", example = "180")
    private Long currentOffsetSeconds;

    @Schema(description = "Video duration in seconds", example = "600")
    private Long durationInSeconds;

    @Schema(description = "Whether the video has finished streaming", example = "false")
    private Boolean isFinished;

    @Schema(description = "Scheduled start time", example = "2026-01-20T08:00:00")
    private LocalDateTime scheduledAt;

    @Schema(description = "Current server time", example = "2026-01-20T08:03:00")
    private LocalDateTime currentTime;

    @Schema(description = "Stream URL path", example = "/api/videos/1/stream")
    private String streamUrl;

    @Schema(description = "Whether video is available for viewing", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Message if video is not available", example = "Video will be available at 2026-01-20 08:00:00")
    private String message;
}
