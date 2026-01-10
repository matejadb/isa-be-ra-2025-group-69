package com.project.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_comments", indexes = {
        @Index(name = "idx_video_created", columnList = "video_id,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoComment {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}