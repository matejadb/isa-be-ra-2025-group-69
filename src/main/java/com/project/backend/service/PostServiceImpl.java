package com.project.backend.service.impl;

import com.project.backend.dto.PostResponse;
import com.project.backend.dto. UserSummary;
import com.project.backend.model.Post;  // ← ISPRAVKA:  model, ne entity!
import com.project.backend. repository.PostRepository;
import com.project.backend.repository.LikeRepository;
import com. project.backend.service.PostService;
import com.project.backend.exception.ResourceNotFoundException;  // ← Dodaj ovu klasu
import lombok.RequiredArgsConstructor;
import org. springframework.data.domain.Page;
import org.springframework.data. domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction. annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;

    @Override
    public Page<PostResponse> getAllPosts(Pageable pageable, Long currentUserId) {
        return postRepository.findAll(pageable)
                .map(post -> mapToResponse(post, currentUserId));
    }

    @Override
    public PostResponse getPostById(Long id, Long currentUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return mapToResponse(post, currentUserId);
    }

    @Override
    public Page<PostResponse> getPostsByUser(Long userId, Pageable pageable, Long currentUserId) {
        return postRepository.findByAuthorId(userId, pageable)
                .map(post -> mapToResponse(post, currentUserId));
    }

    private PostResponse mapToResponse(Post post, Long currentUserId) {
        Boolean likedByCurrentUser = null;

        // Only check like status for authenticated users
        if (currentUserId != null) {
            likedByCurrentUser = likeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);
        }

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .author(UserSummary.builder()
                        .id(post.getAuthor().getId())
                        .username(post.getAuthor().getUsername())
                        .profilePictureUrl(post.getAuthor().getProfilePictureUrl())
                        .build())
                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0)
                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                .likedByCurrentUser(likedByCurrentUser)
                .build();
    }
}