package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request to upload a new video")
public class VideoUploadRequest {
    @NotBlank
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Schema(description = "Video title (required, max 255 characters)", example = "My Amazing Video", required = true)
    private String title;

    @Schema(description = "Video description (optional)", example = "This video shows...")
    private String description;

    @Schema(description = "List of tags for categorization", example = "[\"tutorial\", \"programming\"]")
    private List<String> tags;

    @Schema(description = "Geographic location (optional)", example = "Belgrade, Serbia")
    private String location;
}
