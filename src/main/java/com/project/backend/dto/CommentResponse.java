package com.project.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String text;
    private Long userId;
    private String username;
    private String userFirstName;
    private String userLastName;
    private LocalDateTime createdAt;
}