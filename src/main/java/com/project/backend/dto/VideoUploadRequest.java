package com.project.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class VideoUploadRequest {
    @NotBlank
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;
    private List<String> tags;
    private  String location;
}
