package com.project.backend.repository;

import com.project.backend.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findAllByOrderByCreatedAtDesc();
    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Atomically increment view count for a video
     * Uses a native UPDATE query to ensure thread-safety and consistency
     */
    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") Long videoId);

    /**
     * Fetch video with all relationships eagerly loaded
     * Prevents LazyInitializationException during JSON serialization
     */
    @Query("SELECT v FROM Video v " +
           "LEFT JOIN FETCH v.user " +
           "LEFT JOIN FETCH v.tags " +
           "WHERE v.id = :id")
    Optional<Video> findByIdWithRelations(@Param("id") Long id);

    /**
     * Fetch all videos with eager loading for local trending
     */
    @Query("SELECT DISTINCT v FROM Video v " +
           "LEFT JOIN FETCH v.user " +
           "LEFT JOIN FETCH v.tags")
    List<Video> findAllWithRelations();
}


