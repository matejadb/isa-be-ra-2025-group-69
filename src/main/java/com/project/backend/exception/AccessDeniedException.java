package com.project.backend.exception;

/**
 * Exception thrown when a user attempts to perform an action they don't have permission for.
 * For example, trying to delete another user's comment.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    // Factory methods for common scenarios

    public static AccessDeniedException deleteOwnCommentOnly() {
        return new AccessDeniedException("You can only delete your own comments");
    }

    public static AccessDeniedException editOwnCommentOnly() {
        return new AccessDeniedException("You can only edit your own comments");
    }

    public static AccessDeniedException deleteOwnPostOnly() {
        return new AccessDeniedException("You can only delete your own posts");
    }

    public static AccessDeniedException custom(String message) {
        return new AccessDeniedException(message);
    }
}