package com.project.backend.service;

import com.project.backend.dto.VideoStreamResponse;
import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceScheduledVideoTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private LikeService likeService;

    @InjectMocks
    private VideoService videoService;

    private Video scheduledVideo;
    private Video regularVideo;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Scheduled video that started 3 minutes ago
        scheduledVideo = new Video();
        scheduledVideo.setId(1L);
        scheduledVideo.setTitle("Scheduled Stream");
        scheduledVideo.setVideoPath("videos/test-scheduled.mp4");
        scheduledVideo.setThumbnailPath("thumbnails/test-scheduled.jpg");
        scheduledVideo.setFileSize(10000000L);
        scheduledVideo.setUser(testUser);
        scheduledVideo.setScheduledAt(LocalDateTime.now().minusMinutes(3));
        scheduledVideo.setDurationInSeconds(600L); // 10 minutes
        scheduledVideo.setViewCount(0);
        scheduledVideo.setLikeCount(0);

        // Regular video (no schedule)
        regularVideo = new Video();
        regularVideo.setId(2L);
        regularVideo.setTitle("Regular Video");
        regularVideo.setVideoPath("videos/test-regular.mp4");
        regularVideo.setThumbnailPath("thumbnails/test-regular.jpg");
        regularVideo.setFileSize(10000000L);
        regularVideo.setUser(testUser);
        regularVideo.setScheduledAt(null); // Not scheduled
        regularVideo.setDurationInSeconds(300L);
        regularVideo.setViewCount(0);
        regularVideo.setLikeCount(0);
    }

    @Test
    void testScheduledVideoStreamInfo_VideoIsAvailable() {
        // Given: Video scheduled 3 minutes ago
        when(videoRepository.findById(1L)).thenReturn(Optional.of(scheduledVideo));

        // When: Get stream info
        VideoStreamResponse response = videoService.getStreamInfo(1L);

        // Then: Video should be available with offset of ~180 seconds
        assertNotNull(response);
        assertTrue(response.getIsAvailable());
        assertFalse(response.getIsFinished());
        assertTrue(response.getCurrentOffsetSeconds() >= 170 && response.getCurrentOffsetSeconds() <= 190);
        assertEquals(600L, response.getDurationInSeconds());
        assertNotNull(response.getScheduledAt());
        assertNull(response.getMessage());
    }

    @Test
    void testScheduledVideoStreamInfo_VideoNotYetAvailable() {
        // Given: Video scheduled 1 hour in the future
        scheduledVideo.setScheduledAt(LocalDateTime.now().plusHours(1));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(scheduledVideo));

        // When: Get stream info
        VideoStreamResponse response = videoService.getStreamInfo(1L);

        // Then: Video should not be available
        assertNotNull(response);
        assertFalse(response.getIsAvailable());
        assertFalse(response.getIsFinished());
        assertEquals(0L, response.getCurrentOffsetSeconds());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("will be available"));
    }

    @Test
    void testScheduledVideoStreamInfo_VideoHasFinished() {
        // Given: Video scheduled 15 minutes ago with 10 minute duration
        scheduledVideo.setScheduledAt(LocalDateTime.now().minusMinutes(15));
        scheduledVideo.setDurationInSeconds(600L); // 10 minutes
        when(videoRepository.findById(1L)).thenReturn(Optional.of(scheduledVideo));

        // When: Get stream info
        VideoStreamResponse response = videoService.getStreamInfo(1L);

        // Then: Video should be finished
        assertNotNull(response);
        assertTrue(response.getIsAvailable());
        assertTrue(response.getIsFinished());
        assertEquals(600L, response.getCurrentOffsetSeconds()); // At end of video
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("finished"));
    }

    @Test
    void testRegularVideoStreamInfo_AlwaysAvailable() {
        // Given: Regular video (not scheduled)
        when(videoRepository.findById(2L)).thenReturn(Optional.of(regularVideo));

        // When: Get stream info
        VideoStreamResponse response = videoService.getStreamInfo(2L);

        // Then: Video should always be available from the beginning
        assertNotNull(response);
        assertTrue(response.getIsAvailable());
        assertFalse(response.getIsFinished());
        assertEquals(0L, response.getCurrentOffsetSeconds());
        assertNull(response.getScheduledAt());
    }

    @Test
    void testScheduledVideoStreamInfo_ExactlyAtScheduledTime() {
        // Given: Video scheduled at current time
        scheduledVideo.setScheduledAt(LocalDateTime.now());
        when(videoRepository.findById(1L)).thenReturn(Optional.of(scheduledVideo));

        // When: Get stream info
        VideoStreamResponse response = videoService.getStreamInfo(1L);

        // Then: Video should be available from the start
        assertNotNull(response);
        assertTrue(response.getIsAvailable());
        assertFalse(response.getIsFinished());
        assertTrue(response.getCurrentOffsetSeconds() >= 0 && response.getCurrentOffsetSeconds() <= 2);
    }

    @Test
    void testGetVideoPath() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(scheduledVideo));

        // When
        String path = videoService.getVideoPath(1L);

        // Then
        assertEquals("videos/test-scheduled.mp4", path);
    }

    @Test
    void testGetThumbnailPath() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(scheduledVideo));

        // When
        String path = videoService.getThumbnailPath(1L);

        // Then
        assertEquals("thumbnails/test-scheduled.jpg", path);
    }

    @Test
    void testScheduledVideoStreamInfo_VideoNotFound() {
        // Given: Video doesn't exist
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then: Should throw exception
        assertThrows(RuntimeException.class, () -> {
            videoService.getStreamInfo(999L);
        });
    }
}
