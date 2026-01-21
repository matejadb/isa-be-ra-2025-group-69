package com.project.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime pipelineRunAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_place_video_id")
    private Video firstPlaceVideo;

    @Column
    private Double firstPlaceScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "second_place_video_id")
    private Video secondPlaceVideo;

    @Column
    private Double secondPlaceScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_place_video_id")
    private Video thirdPlaceVideo;

    @Column
    private Double thirdPlaceScore;

    @Column(nullable = false)
    private Boolean isLatest = true;
}
