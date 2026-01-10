package com.project.backend.exception;

public class RateLimitExceededException extends RuntimeException {

    private final int currentCount;
    private final int maxLimit;
    private final String resetTime;

    public RateLimitExceededException(int currentCount, int maxLimit, String resetTime) {
        super(String.format("Rate limit exceeded.  You have made %d comments in the last hour.  Maximum allowed is %d. Try again after %s.",
                currentCount, maxLimit, resetTime));
        this.currentCount = currentCount;
        this.maxLimit = maxLimit;
        this.resetTime = resetTime;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public String getResetTime() {
        return resetTime;
    }
}