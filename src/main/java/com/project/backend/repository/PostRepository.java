package com.project.backend. repository;

import com.project. backend.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework. stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Find all posts by author/user ID with pagination
     */
    Page<Post> findByAuthorId(Long userId, Pageable pageable);

    /**
     * Count posts by author/user ID
     */
    long countByAuthorId(Long userId);  // ← DODAJ OVO
}