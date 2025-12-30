package com.project.backend.service;

import com.project.backend.dto.VideoResponse;
import com.project.backend.dto.VideoUploadRequest;
import com.project.backend.model.Tag;
import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.repository.TagRepository;
import com.project.backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final TagRepository tagRepository;
    private final FileStorageService fileStorageService;

    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024; // 200 MB

    @Transactional
    public VideoResponse uploadVideo(
            VideoUploadRequest request,
            MultipartFile videoFile,
            MultipartFile thumbnailFile,
            User user
    ){
        // Validate video size
        if(videoFile.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Video file size exceeds the maximum limit of 200 MB.");
        }

        // Validate video format
        String videoContentType = videoFile.getContentType();
        if(videoContentType == null || !videoContentType.equals("video/mp4")) {
            throw new IllegalArgumentException("Only MP4 video format is supported.");
        }

        // Validate thumbnail format
        String thumbnailContentType = thumbnailFile.getContentType();
        if(thumbnailContentType == null || !thumbnailContentType.startsWith("image/")) {
            throw new IllegalArgumentException("Thumbnail must be an image file.");
        }

        try {
            // Store files
            String videoPath = fileStorageService.storeFile(videoFile, "videos");
            String thumbnailPath = fileStorageService.storeFile(thumbnailFile, "thumbnails");

            // Create Video entity
            Video video = new Video();
            video.setTitle(request.getTitle());
            video.setDescription(request.getDescription());
            video.setVideoPath(videoPath);
            video.setThumbnailPath(thumbnailPath);
            video.setFileSize(videoFile.getSize());
            video.setLocation(request.getLocation());
            video.setUser(user);

            // Handle tags
            if(request.getTags() != null && !request.getTags().isEmpty()) {
                Set<Tag> tags = new HashSet<>();
                for(String tagName : request.getTags()) {
                    Tag tag = tagRepository.findByName(tagName.toLowerCase())
                            .orElseGet(() -> tagRepository.save(new Tag(tagName.toLowerCase())));
                    tags.add(tag);
                }
                video.setTags(tags);
            }

            // Save video to database
            Video savedVideo = videoRepository.save(video);

            return mapToResponse(savedVideo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video: " + e.getMessage());
        }
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "thumbnails", key = "#videoId")
    public String getThumbnailPath(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        return video.getThumbnailPath();
    }

    private VideoResponse mapToResponse(Video video) {
        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setVideoUrl("/api/videos/" + video.getId() + "/stream");
        response.setThumbnailUrl("/api/videos/" + video.getId() + "/thumbnail");
        response.setTags(video.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList()));
        response.setLocation(video.getLocation());
        response.setCreatedAt(video.getCreatedAt());
        response.setUserId(video.getUser().getId());
        response.setUsername(video.getUser().getUsername());
        response.setViewCount(video.getViewCount());
        response.setLikeCount(video.getLikeCount());
        return response;
    }
}
