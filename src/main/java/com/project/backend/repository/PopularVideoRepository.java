package com.project.backend.repository;

import com.project.backend.model.PopularVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PopularVideoRepository extends JpaRepository<PopularVideo, Long> {

    /**
     * Find the latest popular videos result
     */
    Optional<PopularVideo> findFirstByIsLatestTrueOrderByPipelineRunAtDesc();

    /**
     * Mark all existing records as not latest
     */
    @Modifying
    @Query("UPDATE PopularVideo pv SET pv.isLatest = false WHERE pv.isLatest = true")
    void markAllAsNotLatest();
}
