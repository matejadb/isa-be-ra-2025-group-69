package com.project.backend.service;

import com.project.backend.exception.RateLimitExceededException;
import org.springframework. stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic. AtomicInteger;

@Service
public class RateLimitService {

    // ========== LOGIN RATE LIMITING (existing) ==========

    private final Map<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_ATTEMPT_WINDOW_MS = 60000; // 1 minut

    public boolean isAllowed(String ipAddress) {
        AttemptInfo info = loginAttempts.computeIfAbsent(ipAddress, k -> new AttemptInfo());

        long now = System.currentTimeMillis();
        if (now - info.windowStart > LOGIN_ATTEMPT_WINDOW_MS) {
            // Reset window
            info.windowStart = now;
            info.count.set(0);
        }

        return info.count.incrementAndGet() <= MAX_LOGIN_ATTEMPTS;
    }

    public void resetAttempts(String ipAddress) {
        loginAttempts.remove(ipAddress);
    }

    private static class AttemptInfo {
        long windowStart = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
    }

    // ========== COMMENT RATE LIMITING (new) ==========

    private static final int MAX_COMMENTS_PER_HOUR = 60;
    private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000; // 1 hour

    // Map:  userId -> Queue of comment timestamps
    private final Map<Long, Queue<Long>> userCommentTimestamps = new ConcurrentHashMap<>();

    /**
     * Check if user can post a comment based on rate limit (60 comments per hour)
     *
     * @param userId the user ID
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkCommentRateLimit(Long userId) {
        long currentTime = System.currentTimeMillis();

        // Get or create queue for user
        Queue<Long> timestamps = userCommentTimestamps. computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());

        // Remove timestamps older than 1 hour
        timestamps.removeIf(timestamp -> currentTime - timestamp > ONE_HOUR_IN_MILLIS);

        // Check if user has exceeded rate limit
        if (timestamps.size() >= MAX_COMMENTS_PER_HOUR) {
            // Calculate when the oldest comment will expire (reset time)
            Long oldestTimestamp = timestamps.peek();
            if (oldestTimestamp != null) {
                long resetTimeMillis = oldestTimestamp + ONE_HOUR_IN_MILLIS;
                LocalDateTime resetTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(resetTimeMillis),
                        java.time.ZoneId.systemDefault()
                );
                String resetTimeFormatted = resetTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                throw new RateLimitExceededException(timestamps.size(), MAX_COMMENTS_PER_HOUR, resetTimeFormatted);
            }
        }
    }

    /**
     * Record a comment timestamp for a user
     *
     * @param userId the user ID
     */
    public void recordComment(Long userId) {
        long currentTime = System.currentTimeMillis();
        Queue<Long> timestamps = userCommentTimestamps.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        timestamps.add(currentTime);
    }

    /**
     * Get current comment count for user in the last hour
     *
     * @param userId the user ID
     * @return number of comments in last hour
     */
    public int getCurrentCommentCount(Long userId) {
        long currentTime = System.currentTimeMillis();
        Queue<Long> timestamps = userCommentTimestamps.get(userId);

        if (timestamps == null) {
            return 0;
        }

        // Remove old timestamps
        timestamps.removeIf(timestamp -> currentTime - timestamp > ONE_HOUR_IN_MILLIS);

        return timestamps.size();
    }

    /**
     * Get remaining comments allowed for user
     *
     * @param userId the user ID
     * @return remaining comments allowed
     */
    public int getRemainingComments(Long userId) {
        int currentCount = getCurrentCommentCount(userId);
        return Math.max(0, MAX_COMMENTS_PER_HOUR - currentCount);
    }

    /**
     * Clear rate limit data for a user (useful for testing)
     *
     * @param userId the user ID
     */
    public void clearUserCommentRateLimit(Long userId) {
        userCommentTimestamps.remove(userId);
    }

    /**
     * Clear all comment rate limit data (useful for testing)
     */
    public void clearAllCommentRateLimits() {
        userCommentTimestamps.clear();
    }
}