package com.project.backend.repository;

import com.project.backend.model.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    /**
     * Get all views within the last 7 days
     */
    @Query("SELECT vv FROM VideoView vv WHERE vv.viewedAt >= :startDate")
    List<VideoView> findViewsSince(@Param("startDate") LocalDateTime startDate);

    /**
     * Count views for a specific video within a date range
     */
    @Query("SELECT COUNT(vv) FROM VideoView vv WHERE vv.video.id = :videoId " +
           "AND vv.viewedAt >= :startDate AND vv.viewedAt < :endDate")
    Long countViewsByVideoAndDateRange(
        @Param("videoId") Long videoId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
