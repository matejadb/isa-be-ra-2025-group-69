package com.project.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java. time.LocalDateTime;

@Entity
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "video_id"})  // ← OBRISAO post_id constraint
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore  // ← DODATO!  Sprečava LazyInitializationException
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)  // ← Dodao nullable = false
    @JsonIgnore  // ← DODATO!   Sprečava LazyInitializationException
    private Video video;

    // OBRISAO Post relaciju - ne postoji Post entitet, samo Video!

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}