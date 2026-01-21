package com.project.backend.service;

import com.project.backend.model.PopularVideo;
import com.project.backend.model.Video;
import com.project.backend.model.VideoView;
import com.project.backend.repository.PopularVideoRepository;
import com.project.backend.repository.VideoRepository;
import com.project.backend.repository.VideoViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularVideoETLService {

    private final VideoViewRepository videoViewRepository;
    private final VideoRepository videoRepository;
    private final PopularVideoRepository popularVideoRepository;

    /**
     * ETL Pipeline that runs daily at 2 AM
     * Calculates popularity scores based on views in the last 7 days with weighted scoring
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM every day
    @Transactional
    public void runETLPipeline() {
        log.info("Starting ETL pipeline for popular videos");

        try {
            LocalDateTime now = LocalDateTime.now();

            // EXTRACT: Get all views from the last 7 days
            LocalDateTime sevenDaysAgo = now.minusDays(7);
            List<VideoView> recentViews = videoViewRepository.findViewsSince(sevenDaysAgo);

            log.info("Extracted {} views from the last 7 days", recentViews.size());

            // TRANSFORM: Calculate popularity scores
            Map<Long, Double> popularityScores = calculatePopularityScores(recentViews, now);

            log.info("Calculated popularity scores for {} videos", popularityScores.size());

            // Get top 3 videos
            List<Map.Entry<Long, Double>> top3 = popularityScores.entrySet()
                    .stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            // LOAD: Store results in database
            savePopularVideos(top3, now);

            log.info("ETL pipeline completed successfully. Top 3 videos: {}",
                    top3.stream().map(e -> "Video " + e.getKey() + " (score: " + e.getValue() + ")")
                            .collect(Collectors.joining(", ")));

        } catch (Exception e) {
            log.error("Error running ETL pipeline", e);
        }
    }

    /**
     * Calculate popularity scores for all videos
     * Formula: For each video, sum of (views_on_day_X * weight_for_day_X)
     * where weight = 7 - days_ago + 1
     * (7 for yesterday, 6 for 2 days ago, ..., 1 for 7 days ago)
     */
    private Map<Long, Double> calculatePopularityScores(List<VideoView> views, LocalDateTime now) {
        Map<Long, Double> scores = new HashMap<>();

        // Group views by video and date
        Map<Long, Map<LocalDate, Long>> viewsByVideoAndDate = views.stream()
                .collect(Collectors.groupingBy(
                        view -> view.getVideo().getId(),
                        Collectors.groupingBy(
                                view -> view.getViewedAt().toLocalDate(),
                                Collectors.counting()
                        )
                ));

        // Calculate weighted scores
        for (Map.Entry<Long, Map<LocalDate, Long>> videoEntry : viewsByVideoAndDate.entrySet()) {
            Long videoId = videoEntry.getKey();
            Map<LocalDate, Long> viewsByDate = videoEntry.getValue();

            double totalScore = 0.0;

            for (Map.Entry<LocalDate, Long> dateEntry : viewsByDate.entrySet()) {
                LocalDate viewDate = dateEntry.getKey();
                Long viewCount = dateEntry.getValue();

                // Calculate days ago (0 for today, 1 for yesterday, etc.)
                long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(viewDate, now.toLocalDate());

                // Weight: 7 for yesterday (daysAgo=1), down to 1 for 7 days ago (daysAgo=7)
                // For today (daysAgo=0), we use weight 8
                double weight = 8 - daysAgo;

                if (weight > 0) {
                    totalScore += viewCount * weight;
                }
            }

            scores.put(videoId, totalScore);
        }

        return scores;
    }

    /**
     * Save the top 3 popular videos to the database
     */
    private void savePopularVideos(List<Map.Entry<Long, Double>> top3, LocalDateTime runTime) {
        // Mark all existing records as not latest
        popularVideoRepository.markAllAsNotLatest();

        PopularVideo popularVideo = new PopularVideo();
        popularVideo.setPipelineRunAt(runTime);
        popularVideo.setIsLatest(true);

        // Set top 3 videos
        if (!top3.isEmpty()) {
            Video firstPlace = videoRepository.findById(top3.get(0).getKey())
                    .orElse(null);
            popularVideo.setFirstPlaceVideo(firstPlace);
            popularVideo.setFirstPlaceScore(top3.get(0).getValue());
        }

        if (top3.size() >= 2) {
            Video secondPlace = videoRepository.findById(top3.get(1).getKey())
                    .orElse(null);
            popularVideo.setSecondPlaceVideo(secondPlace);
            popularVideo.setSecondPlaceScore(top3.get(1).getValue());
        }

        if (top3.size() >= 3) {
            Video thirdPlace = videoRepository.findById(top3.get(2).getKey())
                    .orElse(null);
            popularVideo.setThirdPlaceVideo(thirdPlace);
            popularVideo.setThirdPlaceScore(top3.get(2).getValue());
        }

        popularVideoRepository.save(popularVideo);
        log.info("Saved popular videos result to database");
    }

    /**
     * Manual trigger for testing purposes
     */
    @Transactional
    public void runETLPipelineManually() {
        log.info("Manually triggered ETL pipeline");
        runETLPipeline();
    }
}
