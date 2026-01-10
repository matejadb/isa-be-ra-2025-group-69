package com.project.backend.dto;

import jakarta.validation.constraints. NotBlank;
import jakarta. validation.constraints.Size;
import lombok. Data;

@Data
public class CreateVideoCommentRequest {

    @NotBlank(message = "Content cannot be empty")
    @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    private String content;
}