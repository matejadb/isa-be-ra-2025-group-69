package com.project.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoCommentDTO {
    private Long id;
    private String content;
    private Long videoId;
    private AuthorDTO author;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorDTO {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
    }
}