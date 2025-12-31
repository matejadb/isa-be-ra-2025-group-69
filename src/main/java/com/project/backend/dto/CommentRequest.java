package com.project.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Comment cannot be empty")
    @Size(max = 1000, message = "Comment can have a maximum of 1000 characters")
    private String text;
}
