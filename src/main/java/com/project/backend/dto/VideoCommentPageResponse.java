package com.project.backend.dto;

import lombok. AllArgsConstructor;
import lombok.Builder;
import lombok. Data;
import lombok.NoArgsConstructor;

import java. util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoCommentPageResponse {
    private List<VideoCommentDTO> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
}