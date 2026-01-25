package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request parameters for getting local trending videos")
public class LocalTrendingRequest {

    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    @Schema(description = "User's current latitude", example = "44.7866", required = true)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    @Schema(description = "User's current longitude", example = "20.4489", required = true)
    private Double longitude;

    @Min(value = 1, message = "Radius must be at least 1 km")
    @Max(value = 500, message = "Radius cannot exceed 500 km")
    @Schema(description = "Search radius in kilometers (default: 50km)", example = "50", defaultValue = "50")
    private Double radiusKm = 50.0;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    @Schema(description = "Maximum number of trending videos to return (default: 10)", example = "10", defaultValue = "10")
    private Integer limit = 10;

    @Min(value = 1, message = "Days must be at least 1")
    @Max(value = 30, message = "Days cannot exceed 30")
    @Schema(description = "Time window in days to consider for trending calculation (default: 7)", example = "7", defaultValue = "7")
    private Integer days = 7;
}
