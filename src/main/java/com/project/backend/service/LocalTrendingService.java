package com.project.backend.service;

import com.project.backend.dto.LocalTrendingRequest;
import com.project.backend.dto.LocalTrendingVideoDTO;
import com.project.backend.dto.VideoResponse;
import com.project.backend.model.Video;
import com.project.backend.model.VideoView;
import com.project.backend.repository.VideoRepository;
import com.project.backend.repository.VideoViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalTrendingService {

    private final VideoRepository videoRepository;
    private final VideoViewRepository videoViewRepository;
    private final VideoService videoService;
    private final SpatialSearchService spatialSearchService;

    // Scoring weights (total = 100)
    private static final double VIEWS_WEIGHT = 40.0;      // 40% weight
    private static final double LIKES_WEIGHT = 30.0;      // 30% weight
    private static final double ENGAGEMENT_WEIGHT = 20.0; // 20% weight
    private static final double RECENCY_WEIGHT = 10.0;    // 10% weight

    /**
     * Calculate local trending videos based on user's location and multiple popularity parameters
     */
    @Transactional(readOnly = true)
    public List<LocalTrendingVideoDTO> getLocalTrendingVideos(
            LocalTrendingRequest request,
            Long currentUserId
    ) {
        try {
            log.info("Calculating local trending videos for location: {}, {}, radius: {} km, days: {}",
                    request.getLatitude(), request.getLongitude(), request.getRadiusKm(), request.getDays());

            // 1. Find all videos within the specified radius
            List<Video> videosInRadius = findVideosInRadius(
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getRadiusKm()
            );

            if (videosInRadius.isEmpty()) {
                log.info("No videos found within {} km radius", request.getRadiusKm());
                return Collections.emptyList();
            }

            log.info("Found {} videos within radius", videosInRadius.size());

            // 2. Calculate time window
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusDays(request.getDays());

            // 3. Get view data for the time window
            log.debug("Fetching view data since {}", startTime);
            Map<Long, List<VideoView>> viewsByVideo = videoViewRepository
                    .findViewsSince(startTime)
                    .stream()
                    .filter(view -> videosInRadius.stream()
                            .anyMatch(v -> v.getId().equals(view.getVideo().getId())))
                    .collect(Collectors.groupingBy(view -> view.getVideo().getId()));

            log.debug("Found view data for {} videos", viewsByVideo.size());

            // 4. Calculate popularity scores for each video
            List<LocalTrendingVideoDTO> trendingVideos = new ArrayList<>();

            for (Video video : videosInRadius) {
                try {
                    List<VideoView> videoViews = viewsByVideo.getOrDefault(video.getId(), Collections.emptyList());

                    // Calculate distance from user location
                    double distance = spatialSearchService.calculateDistance(
                            request.getLatitude(),
                            request.getLongitude(),
                            video.getLatitude(),
                            video.getLongitude()
                    );

                    // Calculate detailed metrics
                    LocalTrendingVideoDTO.PopularityMetrics metrics = calculateMetrics(
                            video,
                            videoViews,
                            now,
                            request.getDays()
                    );

                    // Calculate overall popularity score (0-100)
                    double popularityScore = calculatePopularityScore(metrics);

                    log.debug("Processing video {}: score={}, distance={}km",
                            video.getId(), popularityScore, distance);

                    // Get video response - this will trigger lazy loading within transaction
                    VideoResponse videoResponse = videoService.getVideoById(video.getId(), currentUserId);

                    // Create trending DTO
                    LocalTrendingVideoDTO trendingVideo = new LocalTrendingVideoDTO();
                    trendingVideo.setVideo(videoResponse);
                    trendingVideo.setPopularityScore(popularityScore);
                    trendingVideo.setDistanceKm(distance);
                    trendingVideo.setMetrics(metrics);

                    trendingVideos.add(trendingVideo);

                } catch (Exception e) {
                    log.error("Error processing video {}: {}", video.getId(), e.getMessage(), e);
                    // Continue with other videos instead of failing entire request
                }
            }

            // 5. Sort by popularity score and assign ranks
            List<LocalTrendingVideoDTO> sortedVideos = trendingVideos.stream()
                    .sorted(Comparator.comparingDouble(LocalTrendingVideoDTO::getPopularityScore).reversed())
                    .limit(request.getLimit())
                    .collect(Collectors.toList());

            // Assign ranks
            for (int i = 0; i < sortedVideos.size(); i++) {
                sortedVideos.get(i).setRank(i + 1);
            }

            log.info("Successfully calculated popularity scores for {} videos", sortedVideos.size());

            return sortedVideos;

        } catch (Exception e) {
            log.error("Fatal error in getLocalTrendingVideos: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate local trending videos: " + e.getMessage(), e);
        }
    }

    /**
     * Find all videos within a specified radius using optimal spatial search
     * Uses SpatialSearchService which automatically chooses:
     * - PostGIS with spatial index (if enabled) - O(log n)
     * - Haversine formula (fallback) - O(n)
     */
    private List<Video> findVideosInRadius(Double userLat, Double userLon, Double radiusKm) {
        log.debug("Finding videos in radius: lat={}, lon={}, radius={}km", userLat, userLon, radiusKm);

        // Use SpatialSearchService for optimal spatial query
        List<Video> videosInRadius = spatialSearchService.findVideosWithinRadius(userLat, userLon, radiusKm);

        log.debug("Videos with coordinates in radius: {}", videosInRadius.size());

        return videosInRadius;
    }


    /**
     * Calculate detailed popularity metrics for a video
     */
    private LocalTrendingVideoDTO.PopularityMetrics calculateMetrics(
            Video video,
            List<VideoView> recentViews,
            LocalDateTime now,
            int days
    ) {
        LocalTrendingVideoDTO.PopularityMetrics metrics = new LocalTrendingVideoDTO.PopularityMetrics();

        int totalViews = recentViews.size();
        metrics.setTotalViews(totalViews);

        int totalLikes = video.getLikeCount();
        metrics.setTotalLikes(totalLikes);

        double engagementRate = totalViews > 0 ? (totalLikes * 100.0 / totalViews) : 0.0;
        metrics.setEngagementRate(engagementRate);

        long ageInDays = ChronoUnit.DAYS.between(video.getCreatedAt(), now);
        metrics.setAgeInDays(ageInDays);


        // 1. View Score (0-40): Weighted views based on recency
        double viewScore = calculateViewScore(recentViews, now, days);
        metrics.setViewScore(viewScore);

        // 2. Like Score (0-30): Based on total likes with logarithmic scaling
        double likeScore = calculateLikeScore(totalLikes);
        metrics.setLikeScore(likeScore);

        // 3. Engagement Score (0-20): Based on engagement rate
        double engagementScore = calculateEngagementScore(engagementRate);
        metrics.setEngagementScore(engagementScore);

        // 4. Recency Score (0-10): Newer videos get higher scores
        double recencyScore = calculateRecencyScore(ageInDays);
        metrics.setRecencyScore(recencyScore);

        return metrics;
    }

    /**
     * Calculate view score with recency weighting (0-40)
     */
    private double calculateViewScore(List<VideoView> views, LocalDateTime now, int days) {
        if (views.isEmpty()) {
            return 0.0;
        }

        double weightedViews = 0.0;

        for (VideoView view : views) {
            long daysAgo = ChronoUnit.DAYS.between(view.getViewedAt().toLocalDate(), now.toLocalDate());

            double weight = 1.0 - (daysAgo * 0.1);
            if (weight < 0.1) weight = 0.1; 

            weightedViews += weight;
        }

        double normalizedScore = Math.log10(1 + weightedViews) * 10;
        return Math.min(normalizedScore, VIEWS_WEIGHT);
    }

    /**
     * Calculate like score with logarithmic scaling (0-30)
     */
    private double calculateLikeScore(int likes) {
        if (likes == 0) {
            return 0.0;
        }

        double normalizedScore = Math.log10(1 + likes) * 10;
        return Math.min(normalizedScore, LIKES_WEIGHT);
    }

    /**
     * Calculate engagement score (0-20)
     */
    private double calculateEngagementScore(double engagementRate) {
        return Math.min(engagementRate, ENGAGEMENT_WEIGHT);
    }

    /**
     * Calculate recency score - newer videos get higher scores (0-10)
     */
    private double calculateRecencyScore(long ageInDays) {
        if (ageInDays < 1) {
            return RECENCY_WEIGHT;
        }

        double score = RECENCY_WEIGHT - ageInDays;
        return Math.max(score, 0.0);
    }

    /**
     * Calculate overall popularity score by summing all component scores
     */
    private double calculatePopularityScore(LocalTrendingVideoDTO.PopularityMetrics metrics) {
        return metrics.getViewScore()
                + metrics.getLikeScore()
                + metrics.getEngagementScore()
                + metrics.getRecencyScore();
    }
}
