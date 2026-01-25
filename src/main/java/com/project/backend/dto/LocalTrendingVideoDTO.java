package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Local trending video with popularity metrics")
public class LocalTrendingVideoDTO {

    @Schema(description = "Rank in the trending list (1 = most popular)", example = "1")
    private Integer rank;

    @Schema(description = "Video information")
    private VideoResponse video;

    @Schema(description = "Overall popularity score (0-100)", example = "85.5")
    private Double popularityScore;

    @Schema(description = "Distance from user location in kilometers", example = "12.5")
    private Double distanceKm;

    @Schema(description = "Breakdown of popularity metrics")
    private PopularityMetrics metrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Detailed popularity metrics")
    public static class PopularityMetrics {

        @Schema(description = "Score from views (0-40)", example = "35.2")
        private Double viewScore;

        @Schema(description = "Score from likes (0-30)", example = "28.5")
        private Double likeScore;

        @Schema(description = "Score from engagement rate (0-20)", example = "18.0")
        private Double engagementScore;

        @Schema(description = "Score from recency/freshness (0-10)", example = "8.5")
        private Double recencyScore;

        @Schema(description = "Total views in the time window", example = "1234")
        private Integer totalViews;

        @Schema(description = "Total likes", example = "56")
        private Integer totalLikes;

        @Schema(description = "Engagement rate percentage", example = "4.54")
        private Double engagementRate;

        @Schema(description = "Video age in days", example = "5")
        private Long ageInDays;
    }
}
