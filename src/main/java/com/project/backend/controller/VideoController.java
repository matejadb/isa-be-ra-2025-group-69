package com.project.backend.controller;

import com.project.backend.dto.VideoResponse;
import com.project.backend.dto.VideoUploadRequest;
import com.project.backend.model.User;
import com.project.backend.service.LikeService;
import com.project.backend.service.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Video", description = "Video management endpoints - upload, view, stream, and like videos")
public class VideoController {

    private final VideoService videoService;
    private final LikeService likeService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a new video",
            description = "Upload a video with thumbnail, title, description, tags, and optional location. Requires authentication. Max video size: 200MB.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video uploaded successfully",
                    content = @Content(schema = @Schema(implementation = VideoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or file upload failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> uploadVideo(
            @Parameter(description = "Video file in MP4 format (max 200MB)", required = true)
            @RequestPart("video") MultipartFile videoFile,
            @Parameter(description = "Thumbnail image (JPG/PNG)", required = true)
            @RequestPart("thumbnail") MultipartFile thumbnailFile,
            @Parameter(description = "Video metadata (title, description, tags, location) in JSON format", required = true)
            @RequestPart("data") String videoDataJson,
            @AuthenticationPrincipal User user
    ) {
        try {
            VideoUploadRequest request = objectMapper.readValue(videoDataJson, VideoUploadRequest.class);
            VideoResponse response = videoService.uploadVideo(request, videoFile, thumbnailFile, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(
            summary = "Get all videos",
            description = "Retrieve a list of all videos with their metadata, like counts, and user interaction status"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved video list",
                    content = @Content(schema = @Schema(implementation = VideoResponse.class)))
    })
    public ResponseEntity<List<VideoResponse>> getAllVideos(
            @AuthenticationPrincipal User user
    ) {
        Long currentUserId = user != null ? user.getId() : null;
        List<VideoResponse> videos = videoService.getAllVideos(currentUserId);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get video by ID",
            description = "Retrieve detailed information about a specific video by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video found",
                    content = @Content(schema = @Schema(implementation = VideoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Video not found")
    })
    public ResponseEntity<VideoResponse> getVideoById(
            @Parameter(description = "Video ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        try {
            Long currentUserId = user != null ? user.getId() : null;
            VideoResponse video = videoService.getVideoById(id, currentUserId);
            return ResponseEntity.ok(video);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/thumbnail")
    @Operation(
            summary = "Get video thumbnail",
            description = "Retrieve the thumbnail image for a specific video. Thumbnails are cached for performance."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thumbnail retrieved successfully",
                    content = @Content(mediaType = "image/jpeg")),
            @ApiResponse(responseCode = "404", description = "Thumbnail not found")
    })
    public ResponseEntity<Resource> getThumbnail(
            @Parameter(description = "Video ID", required = true) @PathVariable Long id
    ) {
        try {
            String thumbnailPath = videoService.getThumbnailPath(id);
            Path filePath = Paths.get("uploads").resolve(thumbnailPath);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/stream")
    @Operation(
            summary = "Stream video",
            description = "Stream video content in MP4 format"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Video streaming",
                    content = @Content(mediaType = "video/mp4")),
            @ApiResponse(responseCode = "404", description = "Video file not found")
    })
    public ResponseEntity<Resource> streamVideo(
            @Parameter(description = "Video ID", required = true) @PathVariable Long id
    ) {
        try {
            String videoPath = videoService.getVideoPath(id);
            Path filePath = Paths.get("uploads").resolve(videoPath);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("video/mp4"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ====== LIKE ENDPOINTS ======

    @PostMapping("/{id}/like")
    @Operation(
            summary = "Toggle like on video",
            description = "Like or unlike a video. Returns the new like status and total like count. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Like toggled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> toggleLike(
            @Parameter(description = "Video ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        try {
            boolean isLiked = likeService.toggleLike(id, user);
            return ResponseEntity.ok(Map.of(
                    "liked", isLiked,
                    "likeCount", likeService.getLikeCount(id)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/liked")
    @Operation(
            summary = "Check if video is liked by current user",
            description = "Check whether the authenticated user has liked this video. Returns false if not authenticated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Like status retrieved")
    })
    public ResponseEntity<?> checkIfLiked(
            @Parameter(description = "Video ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
        boolean isLiked = likeService.isLikedByUser(id, user.getId());
        return ResponseEntity.ok(Map.of("liked", isLiked));
    }
}