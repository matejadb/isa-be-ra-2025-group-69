package com.project.backend.service;

import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.model.VideoView;
import com.project.backend.repository.VideoRepository;
import com.project.backend.repository.VideoViewRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoViewTrackingService {

    private final VideoViewRepository videoViewRepository;
    private final VideoRepository videoRepository;

    /**
     * Track a video view
     * Creates a record in VideoView table for ETL processing
     */
    @Transactional
    public void trackVideoView(Long videoId, User user, HttpServletRequest request) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            VideoView videoView = new VideoView();
            videoView.setVideo(video);
            videoView.setUser(user);
            videoView.setViewedAt(LocalDateTime.now());

            // Track IP and user agent for analytics
            if (request != null) {
                videoView.setIpAddress(getClientIpAddress(request));
                videoView.setUserAgent(request.getHeader("User-Agent"));
            }

            videoViewRepository.save(videoView);
            log.debug("Tracked view for video {} by user {}", videoId,
                     user != null ? user.getId() : "anonymous");

        } catch (Exception e) {
            log.error("Error tracking video view", e);
            // Don't throw exception - view tracking should not break the application
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
