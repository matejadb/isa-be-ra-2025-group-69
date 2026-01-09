package com.project.backend.service;

import com.project.backend. dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    Page<PostResponse> getAllPosts(Pageable pageable, Long currentUserId);
    PostResponse getPostById(Long id, Long currentUserId);
    Page<PostResponse> getPostsByUser(Long userId, Pageable pageable, Long currentUserId);
}