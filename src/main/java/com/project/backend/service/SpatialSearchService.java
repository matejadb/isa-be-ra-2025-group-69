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
     */
    public List<Video> findVideosWithinRadius(Double latitude, Double longitude, Double radiusKm) {
            return findVideosWithinRadiusPostGIS(latitude, longitude, radiusKm);
    }

    /**
     * PostGIS method - Uses spatial index (GIST) for fast queries
     */
    @SuppressWarnings("unchecked")
    private List<Video> findVideosWithinRadiusPostGIS(Double lat, Double lon, Double radiusKm) {
        log.debug("Using PostGIS spatial index for radius search");

        String sql = """
        SELECT v.* FROM videos v
        WHERE v.location_point IS NOT NULL
        AND ST_DWithin(
            v.location_point,
            CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
            :radiusMeters
        )
        ORDER BY ST_Distance(
            v.location_point,
            CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
        )
    """;

        Query query = entityManager.createNativeQuery(sql, Video.class);
        query.setParameter("latitude", lat);
        query.setParameter("longitude", lon);
        query.setParameter("radiusMeters", radiusKm * 1000);

        return query.getResultList();
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
}
