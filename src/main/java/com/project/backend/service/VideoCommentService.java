package com.project.backend.service;

import com.project.backend.dto.CreateVideoCommentRequest;
import com.project.backend.dto.VideoCommentDTO;
import com.project.backend.dto. VideoCommentPageResponse;
import com.project.backend.exception. AccessDeniedException;
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
    private final RateLimitService rateLimitService;  // ← DODATO

    /**
     * Kreiranje komentara (sa rate limiting - 60 komentara/sat)
     */
    @Transactional
    @CacheEvict(value = "videoComments", key = "#videoId")
    public VideoCommentDTO createComment(Long videoId, CreateVideoCommentRequest request, String username) {
        log.info("Creating video comment for video {} by user {}", videoId, username);

        // Pronađi korisnika
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.userNotFoundByUsername(username));

        // ✅ PROVERI RATE LIMIT (60 komentara/sat)
        rateLimitService.checkCommentRateLimit(author.getId());

        // Pronađi video
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ResourceNotFoundException.videoNotFound(videoId));

        // Kreiraj komentar
        VideoComment comment = VideoComment.builder()
                .content(request.getContent())
                .video(video)
                .author(author)
                .build();

        VideoComment savedComment = videoCommentRepository.save(comment);

        // ✅ ZABELEZI KOMENTAR (za rate limiting)
        rateLimitService.recordComment(author.getId());

        log.info("Video comment {} created successfully for video {} by user {}",
                savedComment.getId(), videoId, username);

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
            throw ResourceNotFoundException.videoNotFound(videoId);
        }

        Pageable pageable = PageRequest. of(page, size);
        Page<VideoComment> commentPage = videoCommentRepository
                .findByVideoIdOrderByCreatedAtDesc(videoId, pageable);

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
    @Transactional(readOnly = true)
    public long getCommentCount(Long videoId) {
        log.debug("Getting comment count for video {}", videoId);

        // Proveri da video postoji
        if (!videoRepository.existsById(videoId)) {
            throw ResourceNotFoundException.videoNotFound(videoId);
        }

        return videoCommentRepository. countByVideoId(videoId);
    }

    /**
     * Brisanje komentara (samo autor može obrisati svoj komentar)
     */
    @Transactional
    @CacheEvict(value = "videoComments", allEntries = true)
    public void deleteComment(Long commentId, String username) {
        log.info("Deleting comment {} by user {}", commentId, username);

        // Pronađi komentar
        VideoComment comment = videoCommentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException. videoCommentNotFound(commentId));

        // ✅ PROVERI DA JE AUTOR (zamenjeno SecurityException sa AccessDeniedException)
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw AccessDeniedException.deleteOwnCommentOnly();
        }

        videoCommentRepository.delete(comment);
        log.info("Video comment {} deleted successfully", commentId);
    }

    /**
     * Mapper - Entity -> DTO
     */
    private VideoCommentDTO mapToDTO(VideoComment comment) {
        return VideoCommentDTO.builder()
                .id(comment. getId())
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