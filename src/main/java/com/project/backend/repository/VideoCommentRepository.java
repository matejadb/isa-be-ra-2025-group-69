package com.project.backend.repository;

import com.project.backend.model. VideoComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework. data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype. Repository;

@Repository
public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    /**
     * Dobavi komentare za video (najnoviji prvo) sa paginacijom
     */
    @Query("SELECT vc FROM VideoComment vc WHERE vc.video. id = :videoId ORDER BY vc.createdAt DESC")
    Page<VideoComment> findByVideoIdOrderByCreatedAtDesc(@Param("videoId") Long videoId, Pageable pageable);

    /**
     * Broj komentara za video
     */
    long countByVideoId(Long videoId);
}