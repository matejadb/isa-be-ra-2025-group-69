package com.project.backend;

import com.project.backend.dto.CreateVideoCommentRequest;
import com.project.backend.dto.VideoCommentDTO;
import com.project.backend.exception.RateLimitExceededException;
import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.repository.UserRepository;
import com.project.backend.repository.VideoRepository;
import com.project.backend.service.VideoCommentService;
import com.project.backend.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VideoCommentRateLimitTest {

    @Autowired
    private VideoCommentService videoCommentService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Video testVideo1;
    private Video testVideo2;

    @BeforeEach
    void setUp() {
        rateLimitService.clearAllCommentRateLimits();

        testUser = new User();
        testUser.setUsername("ratelimit_test_user_" + System.currentTimeMillis());
        testUser.setEmail("ratelimit" + System.currentTimeMillis() + "@test.com");
        testUser.setPassword("password123");
        testUser.setFirstName("Rate");
        testUser.setLastName("Limit");
        testUser = userRepository.save(testUser);

        testVideo1 = new Video();
        testVideo1.setTitle("Test Video 1");
        testVideo1.setDescription("Description 1");
        testVideo1 = videoRepository.save(testVideo1);

        testVideo2 = new Video();
        testVideo2.setTitle("Test Video 2");
        testVideo2.setDescription("Description 2");
        testVideo2 = videoRepository.save(testVideo2);
    }

    @Test
    @DisplayName("User should be able to post 60 comments within an hour")
    void testUserCanPost60Comments() {
        for (int i = 1; i <= 60; i++) {
            CreateVideoCommentRequest request = new CreateVideoCommentRequest();
            request.setContent("Comment " + i);

            VideoCommentDTO comment = videoCommentService.createComment(
                    testVideo1.getId(),
                    request,
                    testUser.getUsername()
            );

            assertNotNull(comment);
            assertEquals("Comment " + i, comment.getContent());
        }

        int currentCount = rateLimitService.getCurrentCommentCount(testUser.getId());
        assertEquals(60, currentCount);

        int remaining = rateLimitService.getRemainingComments(testUser.getId());
        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("User should NOT be able to post 61st comment within an hour")
    void testUserCannotPost61stComment() {
        for (int i = 1; i <= 60; i++) {
            CreateVideoCommentRequest request = new CreateVideoCommentRequest();
            request.setContent("Comment " + i);
            videoCommentService.createComment(testVideo1.getId(), request, testUser.getUsername());
        }

        CreateVideoCommentRequest request61 = new CreateVideoCommentRequest();
        request61.setContent("Comment 61 - Should fail");

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> videoCommentService.createComment(testVideo1.getId(), request61, testUser.getUsername())
        );

        assertEquals(60, exception.getCurrentCount());
        assertEquals(60, exception.getMaxLimit());
        assertNotNull(exception.getResetTime());
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("User can comment on multiple videos but total is limited to 60/hour")
    void testUserCanCommentOnMultipleVideosButTotalIsLimited() {
        for (int i = 1; i <= 30; i++) {
            CreateVideoCommentRequest request = new CreateVideoCommentRequest();
            request.setContent("Video1 Comment " + i);
            videoCommentService.createComment(testVideo1.getId(), request, testUser.getUsername());
        }

        for (int i = 1; i <= 30; i++) {
            CreateVideoCommentRequest request = new CreateVideoCommentRequest();
            request.setContent("Video2 Comment " + i);
            videoCommentService.createComment(testVideo2.getId(), request, testUser.getUsername());
        }

        int currentCount = rateLimitService.getCurrentCommentCount(testUser.getId());
        assertEquals(60, currentCount);

        CreateVideoCommentRequest request61 = new CreateVideoCommentRequest();
        request61.setContent("Should fail");

        assertThrows(
                RateLimitExceededException.class,
                () -> videoCommentService.createComment(testVideo1.getId(), request61, testUser.getUsername())
        );
    }

    @Test
    @DisplayName("Simulate bulk comment posting - stress test")
    void testBulkCommentPosting() {
        int successfulComments = 0;
        int failedComments = 0;
        List<String> errorMessages = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            try {
                CreateVideoCommentRequest request = new CreateVideoCommentRequest();
                request.setContent("Bulk comment " + i);
                videoCommentService.createComment(testVideo1.getId(), request, testUser.getUsername());
                successfulComments++;
            } catch (RateLimitExceededException e) {
                failedComments++;
                if (errorMessages.isEmpty()) {
                    errorMessages.add(e.getMessage());
                }
            }
        }

        assertEquals(60, successfulComments, "Should have created exactly 60 comments");
        assertEquals(40, failedComments, "Should have failed 40 comments");

        int currentCount = rateLimitService.getCurrentCommentCount(testUser.getId());
        assertEquals(60, currentCount);

        int remaining = rateLimitService.getRemainingComments(testUser.getId());
        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("Rate limit should reset after clearing")
    void testRateLimitReset() {
        for (int i = 1; i <= 60; i++) {
            CreateVideoCommentRequest request = new CreateVideoCommentRequest();
            request.setContent("Comment " + i);
            videoCommentService.createComment(testVideo1.getId(), request, testUser.getUsername());
        }

        CreateVideoCommentRequest request61 = new CreateVideoCommentRequest();
        request61.setContent("Should fail");
        assertThrows(RateLimitExceededException.class,
                () -> videoCommentService.createComment(testVideo1.getId(), request61, testUser.getUsername()));

        rateLimitService.clearUserCommentRateLimit(testUser.getId());

        CreateVideoCommentRequest newRequest = new CreateVideoCommentRequest();
        newRequest.setContent("Comment after reset");
        VideoCommentDTO comment = videoCommentService.createComment(
                testVideo1.getId(),
                newRequest,
                testUser.getUsername()
        );

        assertNotNull(comment);
        assertEquals("Comment after reset", comment.getContent());
    }
}