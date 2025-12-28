package com.project.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final Map<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW_MS = 60000; // 1 minut

    public boolean isAllowed(String ipAddress) {
        AttemptInfo info = loginAttempts.computeIfAbsent(ipAddress, k -> new AttemptInfo());

        long now = System.currentTimeMillis();
        if (now - info.windowStart > ATTEMPT_WINDOW_MS) {
            // Reset window
            info.windowStart = now;
            info.count.set(0);
        }

        return info.count.incrementAndGet() <= MAX_ATTEMPTS;
    }

    public void resetAttempts(String ipAddress) {
        loginAttempts.remove(ipAddress);
    }

    private static class AttemptInfo {
        long windowStart = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
    }
}
