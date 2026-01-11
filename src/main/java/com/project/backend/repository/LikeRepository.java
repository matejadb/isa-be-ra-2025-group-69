package com.project.backend.repository;

import com.project.backend.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework. stereotype.Repository;

import java. util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // ========== VIDEO LIKES ==========
    Optional<Like> findByUserIdAndVideoId(Long userId, Long videoId);
    boolean existsByUserIdAndVideoId(Long userId, Long videoId);
    void deleteByUserIdAndVideoId(Long userId, Long videoId);
    long countByVideoId(Long videoId);

    // Post metode su OBRISANE jer Post.java ne postoji!
}