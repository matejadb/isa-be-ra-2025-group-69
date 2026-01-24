package com.project.backend.service;

import com.project.backend.model.Video;
import com.project.backend.repository.VideoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for spatial queries with configurable radius
 * Supports both Haversine formula (works everywhere) and PostGIS (optimized with spatial index)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpatialSearchService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final VideoRepository videoRepository;

    @Value("${app.spatial.use-postgis:false}")
    private boolean usePostGIS;

    /**
     * Find videos within radius using optimal method
     * - If PostGIS enabled: Uses spatial index (fast, O(log n))
     * - If PostGIS disabled: Uses Haversine formula (slower, O(n) but works without PostGIS)
     */
    public List<Video> findVideosWithinRadius(Double latitude, Double longitude, Double radiusKm) {
        if (usePostGIS) {
            return findVideosWithinRadiusPostGIS(latitude, longitude, radiusKm);
        } else {
            return findVideosWithinRadiusHaversine(latitude, longitude, radiusKm);
        }
    }

    /**
     * PostGIS method - Uses spatial index (GIST) for fast queries
     * Requires PostGIS extension and spatial index on videos table
     * Performance: O(log n) with spatial index
     */
    @SuppressWarnings("unchecked")
    private List<Video> findVideosWithinRadiusPostGIS(Double lat, Double lon, Double radiusKm) {
        log.debug("Using PostGIS spatial index for radius search");

        String sql = """
            SELECT v.* FROM videos v
            WHERE v.latitude IS NOT NULL 
            AND v.longitude IS NOT NULL
            AND ST_DWithin(
                ST_MakePoint(v.longitude, v.latitude)::geography,
                ST_MakePoint(:longitude, :latitude)::geography,
                :radiusMeters
            )
            ORDER BY ST_Distance(
                ST_MakePoint(v.longitude, v.latitude)::geography,
                ST_MakePoint(:longitude, :latitude)::geography
            )
            """;

        Query query = entityManager.createNativeQuery(sql, Video.class);
        query.setParameter("latitude", lat);
        query.setParameter("longitude", lon);
        query.setParameter("radiusMeters", radiusKm * 1000); // Convert km to meters

        return query.getResultList();
    }

    /**
     * Haversine method - Works without PostGIS but slower
     * Performance: O(n) - checks all videos
     */
    private List<Video> findVideosWithinRadiusHaversine(Double lat, Double lon, Double radiusKm) {
        log.debug("Using Haversine formula for radius search (no spatial index)");

        // Get all videos with eager loading
        List<Video> allVideos = videoRepository.findAllWithRelations();

        return allVideos.stream()
                .filter(video -> video.getLatitude() != null && video.getLongitude() != null)
                .filter(video -> {
                    double distance = calculateHaversineDistance(
                            lat, lon,
                            video.getLatitude(), video.getLongitude()
                    );
                    return distance <= radiusKm;
                })
                .toList();
    }

    /**
     * Calculate distance using Haversine formula
     */
    private double calculateHaversineDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate distance for a specific video from given coordinates
     */
    public double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        return calculateHaversineDistance(lat1, lon1, lat2, lon2);
    }

    /**
     * Check if PostGIS is enabled
     */
    public boolean isPostGISEnabled() {
        return usePostGIS;
    }
}
