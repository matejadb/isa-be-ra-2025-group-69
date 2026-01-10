package com.project.backend.exception;

import lombok. Getter;

/**
 * Exception koji se baca kada traženi resurs nije pronađen u bazi
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    // ========== KONSTRUKTORI ==========

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    // ========== FACTORY METHODS ==========

    public static ResourceNotFoundException videoNotFound(Long videoId) {
        return new ResourceNotFoundException("Video", "id", videoId);
    }

    public static ResourceNotFoundException userNotFound(Long userId) {
        return new ResourceNotFoundException("User", "id", userId);
    }

    public static ResourceNotFoundException userNotFoundByUsername(String username) {
        return new ResourceNotFoundException("User", "username", username);
    }

    public static ResourceNotFoundException videoCommentNotFound(Long commentId) {
        return new ResourceNotFoundException("VideoComment", "id", commentId);
    }

    public static ResourceNotFoundException postNotFound(Long postId) {
        return new ResourceNotFoundException("Post", "id", postId);
    }

    public static ResourceNotFoundException commentNotFound(Long commentId) {
        return new ResourceNotFoundException("Comment", "id", commentId);
    }
}