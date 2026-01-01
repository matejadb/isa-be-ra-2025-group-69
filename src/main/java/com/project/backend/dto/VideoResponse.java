package com.project.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private List<String> tags;
    private String location;
    private LocalDateTime createdAt;
    private Long userId;
    private String username;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isLikedByCurrentUser; // null if user isn't logged in
}
