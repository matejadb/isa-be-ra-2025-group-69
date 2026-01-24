package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Geographic location with coordinates")
public class LocationDTO {

    @Schema(description = "Latitude coordinate", example = "44.7866")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "20.4489")
    private Double longitude;

    @Schema(description = "City name", example = "Belgrade")
    private String city;

    @Schema(description = "Country name", example = "Serbia")
    private String country;

    @Schema(description = "IP address used for location", example = "192.168.1.1")
    private String ip;

    @Schema(description = "Location source", example = "IP_GEOLOCATION")
    private String source;

    @Schema(description = "Whether location is approximate", example = "true")
    private Boolean approximate;
}
