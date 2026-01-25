package com.project.backend.controller;

import com.project.backend.dto.LocationDTO;
import com.project.backend.service.IpGeolocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:8081"})
@Tag(name = "Location", description = "IP-based geolocation for fallback when user denies location permission")
@Slf4j
public class LocationController {

    private final IpGeolocationService ipGeolocationService;

    @GetMapping("/from-ip")
    @Operation(
            summary = "Get approximate location from IP address",
            description = "Returns approximate geographic location based on the client's IP address. " +
                         "This is used as a fallback when the user denies browser location permission. " +
                         "Uses ip-api.com free API. Falls back to Belgrade, Serbia if IP lookup fails or for private/localhost IPs."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Location retrieved successfully (from IP or default fallback)",
                    content = @Content(schema = @Schema(implementation = LocationDTO.class))
            )
    })
    public ResponseEntity<LocationDTO> getLocationFromIp(HttpServletRequest request) {
        String clientIp = getClientIP(request);
        log.info("Location request from IP: {}", clientIp);

        LocationDTO location = ipGeolocationService.getLocationFromIp(clientIp);

        return ResponseEntity.ok(location);
    }

    /**
     * Extract client IP address from request
     * Handles proxy headers (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, use the first one
            return xfHeader.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
