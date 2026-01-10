package com.project.backend.controller;

import com.project.backend.dto.CreateVideoCommentRequest;
import com.project.backend.dto.VideoCommentDTO;
import com. project.backend.dto.VideoCommentPageResponse;
import com. project.backend.service.VideoCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework. security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos/{videoId}/comments")
@RequiredArgsConstructor
@Tag(name = "Video Comments", description = "Video comment management APIs")
public class VideoCommentController {

    private final VideoCommentService videoCommentService;

    /**
     * Kreiranje komentara (samo za registrovane korisnike)
     */
    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a video comment", description = "Create a new comment on a video (authenticated users only)")
    public ResponseEntity<VideoCommentDTO> createComment(
            @PathVariable Long videoId,
            @Valid @RequestBody CreateVideoCommentRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        VideoCommentDTO comment = videoCommentService.createComment(videoId, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /**
     * Dobavi komentare za video (javno dostupno)
     */
    @GetMapping
    @Operation(summary = "Get comments for video", description = "Get paginated comments sorted by newest first (cached)")
    public ResponseEntity<VideoCommentPageResponse> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        VideoCommentPageResponse comments = videoCommentService.getCommentsByVideoId(videoId, page, size);
        return ResponseEntity.ok(comments);
    }

    /**
     * Broj komentara za video
     */
    @GetMapping("/count")
    @Operation(summary = "Get comment count", description = "Get total number of comments for a video")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long videoId) {
        long count = videoCommentService.getCommentCount(videoId);
        return ResponseEntity.ok(count);
    }

    /**
     * Brisanje komentara (samo autor)
     */
    @DeleteMapping("/{commentId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete comment", description = "Delete your own comment")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long videoId,
            @PathVariable Long commentId,
            Authentication authentication) {

        String username = authentication.getName();
        videoCommentService.deleteComment(commentId, username);
        return ResponseEntity.noContent().build();
    }
}