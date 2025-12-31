package com.project.backend.service;

import com.project.backend.dto.CommentRequest;
import com.project.backend.dto.CommentResponse;
import com.project.backend.model.Comment;
import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.repository.CommentRepository;
import com.project.backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public CommentResponse addComment(Long videoId, CommentRequest request, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Comment comment = new Comment();
        comment.setText(request.getText());
        comment.setUser(user);
        comment.setVideo(video);

        Comment savedComment = commentRepository.save(comment);

        return mapToResponse(savedComment);
    }

    public Page<CommentResponse> getCommentsByVideoId(Long videoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageable);
        return comments.map(this::mapToResponse);
    }

    public long getCommentCount(Long videoId) {
        return commentRepository.countByVideoId(videoId);
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setText(comment.getText());
        response.setUserId(comment.getUser().getId());
        response.setUsername(comment.getUser().getUsername());
        response.setUserFirstName(comment.getUser().getFirstName());
        response.setUserLastName(comment.getUser().getLastName());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}