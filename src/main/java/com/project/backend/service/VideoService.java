package com.project.backend.service;

import com.project.backend.dto.VideoResponse;
import com.project.backend.dto.VideoStreamResponse;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final TagRepository tagRepository;
    private final FileStorageService fileStorageService;
    private final LikeService likeService;

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

            // Calculate video duration
            Long durationInSeconds = calculateVideoDuration(videoPath);

            // Create Video entity
            Video video = new Video();
            video.setTitle(request.getTitle());
            video.setDescription(request.getDescription());
            video.setVideoPath(videoPath);
            video.setThumbnailPath(thumbnailPath);
            video.setFileSize(videoFile.getSize());
            video.setLocation(request.getLocation());
            video.setUser(user);
            video.setScheduledAt(request.getScheduledAt());
            video.setDurationInSeconds(durationInSeconds);

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

            return mapToResponse(savedVideo, user.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video: " + e.getMessage());
        }
    }

    public List<VideoResponse> getAllVideos(Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(video -> isVideoAvailable(video, now))
                .map(video -> mapToResponse(video, currentUserId))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "thumbnails", key = "#videoId")
    public String getThumbnailPath(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        return video.getThumbnailPath();
    }

    private VideoResponse mapToResponse(Video video, Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
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
        response.setScheduledAt(video.getScheduledAt());
        response.setDurationInSeconds(video.getDurationInSeconds());

        // Calculate availability and current offset for scheduled videos
        boolean isAvailable = isVideoAvailable(video, now);
        response.setIsAvailable(isAvailable);

        if (video.getScheduledAt() != null && isAvailable) {
            long offsetSeconds = calculatePlaybackOffset(video.getScheduledAt(), now, video.getDurationInSeconds());
            response.setCurrentPlaybackOffsetSeconds(offsetSeconds);
        }

        // Proveri da li je trenutni korisnik lajkovao video
        if (currentUserId != null) {
            response.setIsLikedByCurrentUser(likeService.isLikedByUser(video.getId(), currentUserId));
        }

        return response;
    }

    public VideoResponse getVideoById(Long id, Long currentUserId) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return mapToResponse(video, currentUserId);
    }

    public String getVideoPath(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return video.getVideoPath();
    }

    /**
     * Check if a video is available for viewing based on its scheduled time
     */
    private boolean isVideoAvailable(Video video, LocalDateTime now) {
        if (video.getScheduledAt() == null) {
            return true; // Not scheduled, always available
        }
        return !now.isBefore(video.getScheduledAt()); // Available if current time is after or equal to scheduled time
    }

    /**
     * Calculate the current playback offset in seconds for a scheduled video
     * All users should be watching the same position based on elapsed time since scheduled start
     */
    private long calculatePlaybackOffset(LocalDateTime scheduledAt, LocalDateTime currentTime, Long durationInSeconds) {
        long elapsedSeconds = ChronoUnit.SECONDS.between(scheduledAt, currentTime);

        if (elapsedSeconds < 0) {
            return 0; // Video hasn't started yet
        }

        if (durationInSeconds != null && elapsedSeconds >= durationInSeconds) {
            return durationInSeconds; // Video has finished
        }

        return elapsedSeconds;
    }

    /**
     * Calculate video duration using FFmpeg
     * Returns duration in seconds, or null if unable to determine
     */
    private Long calculateVideoDuration(String videoPath) {
        try {
            Path fullPath = Paths.get("uploads").resolve(videoPath);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    fullPath.toString()
            );

            Process process = processBuilder.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
            );

            String durationStr = reader.readLine();
            process.waitFor();

            if (durationStr != null && !durationStr.isEmpty()) {
                return (long) Double.parseDouble(durationStr);
            }
        } catch (Exception e) {
            System.err.println("Could not determine video duration: " + e.getMessage());
            // Return null if we can't determine duration - video will still work
        }
        return null;
    }

    /**
     * Get streaming information for a video with time synchronization
     */
    public VideoStreamResponse getStreamInfo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        LocalDateTime now = LocalDateTime.now();
        VideoStreamResponse response = new VideoStreamResponse();
        response.setVideoId(video.getId());
        response.setTitle(video.getTitle());
        response.setDurationInSeconds(video.getDurationInSeconds());
        response.setScheduledAt(video.getScheduledAt());
        response.setCurrentTime(now);
        response.setStreamUrl("/api/videos/" + video.getId() + "/stream");

        // Check if video is available
        boolean isAvailable = isVideoAvailable(video, now);
        response.setIsAvailable(isAvailable);

        if (!isAvailable) {
            response.setMessage("Video will be available at " + video.getScheduledAt());
            response.setCurrentOffsetSeconds(0L);
            response.setIsFinished(false);
        } else if (video.getScheduledAt() != null) {
            // Scheduled video that is now available
            long offset = calculatePlaybackOffset(video.getScheduledAt(), now, video.getDurationInSeconds());
            response.setCurrentOffsetSeconds(offset);

            boolean isFinished = video.getDurationInSeconds() != null && offset >= video.getDurationInSeconds();
            response.setIsFinished(isFinished);

            if (isFinished) {
                response.setMessage("Video has finished streaming");
            }
        } else {
            // Regular video (not scheduled)
            response.setCurrentOffsetSeconds(0L);
            response.setIsFinished(false);
        }

        return response;
    }


}
