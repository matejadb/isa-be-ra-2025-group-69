package com.project.backend.repository;

import com.project.backend.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>{
    Page<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId, Pageable pageable);
    long countByVideoId(Long videoId);
}
