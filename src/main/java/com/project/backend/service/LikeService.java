package com.project.backend.service;

import com.project.backend.model.Like;
import com.project.backend.model.User;
import com.project.backend.model.Video;
import com.project.backend.repository.LikeRepository;
import com.project.backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public boolean toggleLike(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check if user already liked
        if (likeRepository.existsByUserIdAndVideoId(user.getId(), videoId)) {
            // Unlike
            likeRepository.deleteByUserIdAndVideoId(user.getId(), videoId);
            video.setLikeCount(video.getLikeCount() - 1);
            videoRepository.save(video);
            return false; // unliked
        } else {
            // Like
            Like like = new Like();
            like.setUser(user);
            like.setVideo(video);
            likeRepository.save(like);

            video.setLikeCount(video.getLikeCount() + 1);
            videoRepository.save(video);
            return true; // liked
        }
    }

    public boolean isLikedByUser(Long videoId, Long userId) {
        return likeRepository.existsByUserIdAndVideoId(userId, videoId);
    }

    public long getLikeCount(Long videoId) {
        return likeRepository.countByVideoId(videoId);
    }
}
