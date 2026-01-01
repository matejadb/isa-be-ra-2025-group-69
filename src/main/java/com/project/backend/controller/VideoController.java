package com.project.backend.controller;

import com.project.backend.dto.VideoResponse;
import com.project.backend.dto.VideoUploadRequest;
import com.project.backend.model.User;
import com.project.backend.service.LikeService;
import com.project.backend.service.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class VideoController {

    private final VideoService videoService;
    private final LikeService likeService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(
            @RequestPart("video") MultipartFile videoFile,
            @RequestPart("thumbnail") MultipartFile thumbnailFile,
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
    public ResponseEntity<List<VideoResponse>> getAllVideos(
            @AuthenticationPrincipal User user
    ) {
        Long currentUserId = user != null ? user.getId() : null;
        List<VideoResponse> videos = videoService.getAllVideos(currentUserId);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideoById(
            @PathVariable Long id,
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
    public ResponseEntity<Resource> getThumbnail(@PathVariable Long id) {
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
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id) {
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
    public ResponseEntity<?> toggleLike(
            @PathVariable Long id,
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
    public ResponseEntity<?> checkIfLiked(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
        boolean isLiked = likeService.isLikedByUser(id, user.getId());
        return ResponseEntity.ok(Map.of("liked", isLiked));
    }
}