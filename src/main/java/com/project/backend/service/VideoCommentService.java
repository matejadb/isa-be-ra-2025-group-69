package com.project.backend.service;

import com.project.backend.dto.CreateVideoCommentRequest;
import com.project.backend.dto.VideoCommentDTO;
import com. project.backend.dto.VideoCommentPageResponse;
import com. project.backend.exception.ResourceNotFoundException;
import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.model.VideoComment;
import com.project.backend.repository. UserRepository;
import com.project.backend.repository.VideoCommentRepository;
import com.project. backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern. slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache. annotation.Cacheable;
import org.springframework.data.domain. Page;
import org.springframework. data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoCommentService {

    private final VideoCommentRepository videoCommentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    /**
     * Kreiranje komentara
     */
    @Transactional
    @CacheEvict(value = "videoComments", key = "#videoId")
    public VideoCommentDTO createComment(Long videoId, CreateVideoCommentRequest request, String username) {
        log.info("Creating video comment for video {} by user {}", videoId, username);

        // Pronađi korisnika
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.userNotFoundByUsername(username));  // ← IZMENA 1

        // Pronađi video
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ResourceNotFoundException.videoNotFound(videoId));  // ← IZMENA 2

        // Kreiraj komentar
        VideoComment comment = VideoComment.builder()
                .content(request. getContent())
                .video(video)
                .author(author)
                .build();

        VideoComment savedComment = videoCommentRepository.save(comment);
        log.info("Video comment {} created successfully", savedComment.getId());

        return mapToDTO(savedComment);
    }

    /**
     * Dobavi komentare za video (keširano, najnoviji prvo)
     */
    @Cacheable(value = "videoComments", key = "#videoId + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public VideoCommentPageResponse getCommentsByVideoId(Long videoId, int page, int size) {
        log.info("Fetching comments for video {} (page:  {}, size: {})", videoId, page, size);

        // Proveri da video postoji
        if (!videoRepository.existsById(videoId)) {
            throw ResourceNotFoundException.videoNotFound(videoId);  // ← IZMENA 3
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<VideoComment> commentPage = videoCommentRepository
                . findByVideoIdOrderByCreatedAtDesc(videoId, pageable);

        return VideoCommentPageResponse.builder()
                .content(commentPage.getContent().stream().map(this::mapToDTO).toList())
                .pageNumber(commentPage.getNumber())
                .pageSize(commentPage. getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .last(commentPage.isLast())
                .first(commentPage.isFirst())
                .build();
    }

    /**
     * Broj komentara za video
     */
    public long getCommentCount(Long videoId) {
        return videoCommentRepository.countByVideoId(videoId);
    }

    /**
     * Brisanje komentara
     */
    @Transactional
    @CacheEvict(value = "videoComments", allEntries = true)
    public void deleteComment(Long commentId, String username) {
        VideoComment comment = videoCommentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.videoCommentNotFound(commentId));  // ← IZMENA 4

        // Proveri da je autor
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new SecurityException("You can only delete your own comments");
        }

        videoCommentRepository.delete(comment);
        log.info("Video comment {} deleted", commentId);
    }

    /**
     * Mapper - Entity -> DTO
     */
    private VideoCommentDTO mapToDTO(VideoComment comment) {
        return VideoCommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .videoId(comment.getVideo().getId())
                .author(VideoCommentDTO.AuthorDTO. builder()
                        .id(comment.getAuthor().getId())
                        .username(comment. getAuthor().getUsername())
                        .firstName(comment.getAuthor().getFirstName())
                        .lastName(comment.getAuthor().getLastName())
                        .build())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}