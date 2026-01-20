package com.project.backend.service;

import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.repository.UserRepository;
import com.project.backend.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating thread-safe view count increment mechanism
 * Simulates concurrent user access to the same video and verifies correct counting
 */
@SpringBootTest
@ActiveProfiles("test")
public class VideoViewCountConcurrencyTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private Video testVideo;
    private User testUser;

    @BeforeEach
    @Transactional
    public void setup() {
        // Clean up existing data
        videoRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActivated(true);
        testUser = userRepository.save(testUser);

        // Create test video
        testVideo = new Video();
        testVideo.setTitle("Test Video");
        testVideo.setDescription("Test video for concurrent view counting");
        testVideo.setVideoPath("videos/test.mp4");
        testVideo.setThumbnailPath("thumbnails/test.jpg");
        testVideo.setFileSize(1024L);
        testVideo.setCreatedAt(LocalDateTime.now());
        testVideo.setUser(testUser);
        testVideo.setViewCount(0);
        testVideo.setLikeCount(0);
        testVideo = videoRepository.save(testVideo);
    }

    /**
     * Test concurrent view count increments with multiple threads
     * This test simulates multiple users viewing the same video simultaneously
     */
    @Test
    public void testConcurrentViewCountIncrement() throws InterruptedException, ExecutionException {
        final int NUMBER_OF_THREADS = 50; // Simulate 50 concurrent users
        final int VIEWS_PER_THREAD = 10;  // Each user views the video 10 times (refreshes)
        final int EXPECTED_TOTAL_VIEWS = NUMBER_OF_THREADS * VIEWS_PER_THREAD;

        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUMBER_OF_THREADS);

        List<Future<?>> futures = new ArrayList<>();

        // Create tasks that will increment view count
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            final int threadNum = i;
            Future<?> future = executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Simulate multiple views from this user
                    for (int j = 0; j < VIEWS_PER_THREAD; j++) {
                        videoService.incrementViewCount(testVideo.getId());

                        // Small random delay to simulate real-world scenario
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
                    }

                    System.out.println("Thread " + threadNum + " completed " + VIEWS_PER_THREAD + " views");
                } catch (Exception e) {
                    System.err.println("Thread " + threadNum + " encountered error: " + e.getMessage());
                    throw new RuntimeException(e);
                } finally {
                    endLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all threads at once
        System.out.println("Starting concurrent view count test with " + NUMBER_OF_THREADS + " threads...");
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        assertTrue(completed, "All threads should complete within timeout");

        // Check for any exceptions in futures
        for (Future<?> future : futures) {
            future.get(); // Will throw if any thread had an exception
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Verify the final view count
        Integer finalViewCount = videoService.getViewCount(testVideo.getId());

        System.out.println("\n=== Test Results ===");
        System.out.println("Number of concurrent threads: " + NUMBER_OF_THREADS);
        System.out.println("Views per thread: " + VIEWS_PER_THREAD);
        System.out.println("Expected total views: " + EXPECTED_TOTAL_VIEWS);
        System.out.println("Actual total views: " + finalViewCount);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        System.out.println("===================\n");

        // Assert that the view count is exactly what we expect
        assertEquals(EXPECTED_TOTAL_VIEWS, finalViewCount,
                "View count should be exactly " + EXPECTED_TOTAL_VIEWS +
                " after " + NUMBER_OF_THREADS + " threads each increment " + VIEWS_PER_THREAD + " times");

        // Verify data consistency in database
        Video videoFromDb = videoRepository.findById(testVideo.getId()).orElseThrow();
        assertEquals(EXPECTED_TOTAL_VIEWS, videoFromDb.getViewCount(),
                "View count in database should match expected value");
    }

    /**
     * Test that incrementing view count for non-existent video throws exception
     */
    @Test
    public void testIncrementViewCountForNonExistentVideo() {
        Long nonExistentId = 99999L;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            videoService.incrementViewCount(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("Video not found"));
    }

    /**
     * Test sequential view count increments (baseline test)
     */
    @Test
    public void testSequentialViewCountIncrement() {
        final int NUMBER_OF_VIEWS = 100;

        // Initial view count should be 0
        assertEquals(0, videoService.getViewCount(testVideo.getId()));

        // Increment sequentially
        for (int i = 0; i < NUMBER_OF_VIEWS; i++) {
            videoService.incrementViewCount(testVideo.getId());
        }

        // Verify final count
        Integer finalCount = videoService.getViewCount(testVideo.getId());
        assertEquals(NUMBER_OF_VIEWS, finalCount,
                "Sequential increments should result in correct view count");
    }

    /**
     * Test concurrent access with high contention (stress test)
     */
    @Test
    public void testHighConcurrencyStressTest() throws InterruptedException, ExecutionException {
        final int NUMBER_OF_THREADS = 100; // High number of concurrent users
        final int VIEWS_PER_THREAD = 5;
        final int EXPECTED_TOTAL_VIEWS = NUMBER_OF_THREADS * VIEWS_PER_THREAD;

        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        System.out.println("\nStarting stress test with " + NUMBER_OF_THREADS + " concurrent threads...");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            Future<?> future = executorService.submit(() -> {
                try {
                    for (int j = 0; j < VIEWS_PER_THREAD; j++) {
                        videoService.incrementViewCount(testVideo.getId());
                    }
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        boolean completed = latch.await(120, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        assertTrue(completed, "Stress test should complete within timeout");

        // Check for exceptions
        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        Integer finalViewCount = videoService.getViewCount(testVideo.getId());

        System.out.println("=== Stress Test Results ===");
        System.out.println("Threads: " + NUMBER_OF_THREADS);
        System.out.println("Expected views: " + EXPECTED_TOTAL_VIEWS);
        System.out.println("Actual views: " + finalViewCount);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        System.out.println("Throughput: " + (EXPECTED_TOTAL_VIEWS / ((endTime - startTime) / 1000.0)) + " views/sec");
        System.out.println("===========================\n");

        assertEquals(EXPECTED_TOTAL_VIEWS, finalViewCount,
                "High concurrency stress test should maintain data consistency");
    }
}
