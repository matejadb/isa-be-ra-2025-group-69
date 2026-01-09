package com.project.backend.controller;

import com.project.backend.dto.ApiResponse;
import com.project. backend.dto.PostResponse;
import com.project.backend.service.PostService;
import com.project.backend.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain. Page;
import org.springframework. data.domain.Pageable;
import org.springframework.data. domain.Sort;
import org. springframework.data.web.PageableDefault;
import org.springframework. http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post management endpoints")
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "Get all posts", description = "Returns paginated posts sorted by creation time (newest first). Accessible to all users.")
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Page<PostResponse> posts = postService.getAllPosts(pageable, currentUserId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID", description = "Returns a single post. Accessible to all users.")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable Long id,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        PostResponse post = postService.getPostById(id, currentUserId);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user", description = "Returns all posts from a specific user. Accessible to all users.")
    public ResponseEntity<Page<PostResponse>> getPostsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Page<PostResponse> posts = postService.getPostsByUser(userId, pageable, currentUserId);
        return ResponseEntity.ok(posts);
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // Extract user ID from authentication principal
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}