package com.project.backend.controller;

import com.project.backend.dto.LocalTrendingRequest;
import com.project.backend.dto.LocalTrendingVideoDTO;
import com.project.backend.model.User;
import com.project.backend.service.LocalTrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trending/local")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Local Trending", description = "Location-based trending videos with multi-parameter popularity scoring")
@Slf4j
public class LocalTrendingController {

    private final LocalTrendingService localTrendingService;

    @GetMapping
    @Operation(
            summary = "Get local trending videos",
            description = "Returns trending videos near the user's location based on multiple popularity parameters:\n" +
                         "- **Views** (40% weight): Recent views with time-based decay\n" +
                         "- **Likes** (30% weight): Total likes with logarithmic scaling\n" +
                         "- **Engagement** (20% weight): Like-to-view ratio\n" +
                         "- **Recency** (10% weight): Newer videos preferred\n\n" +
                         "Videos are filtered by geographic radius using the Haversine formula."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved local trending videos",
                    content = @Content(schema = @Schema(implementation = LocalTrendingVideoDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            )
    })
    public ResponseEntity<List<LocalTrendingVideoDTO>> getLocalTrending(
            @Parameter(description = "User's latitude (-90 to 90)", required = true, example = "44.7866")
            @RequestParam Double latitude,

            @Parameter(description = "User's longitude (-180 to 180)", required = true, example = "20.4489")
            @RequestParam Double longitude,

            @Parameter(description = "Search radius in kilometers (1-500)", example = "50")
            @RequestParam(defaultValue = "50") Double radiusKm,

            @Parameter(description = "Maximum number of videos to return (1-100)", example = "10")
            @RequestParam(defaultValue = "10") Integer limit,

            @Parameter(description = "Time window in days to consider (1-30)", example = "7")
            @RequestParam(defaultValue = "7") Integer days,

            @AuthenticationPrincipal User user
    ) {
        LocalTrendingRequest request = new LocalTrendingRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusKm(radiusKm);
        request.setLimit(limit);
        request.setDays(days);

        Long currentUserId = user != null ? user.getId() : null;
        List<LocalTrendingVideoDTO> trendingVideos = localTrendingService.getLocalTrendingVideos(
                request,
                currentUserId
        );

        return ResponseEntity.ok(trendingVideos);
    }

    @PostMapping
    @Operation(
            summary = "Get local trending videos (POST version)",
            description = "Alternative POST endpoint for getting local trending videos. " +
                         "Accepts request body with all parameters. " +
                         "Uses the same multi-parameter popularity algorithm as the GET endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved local trending videos",
                    content = @Content(schema = @Schema(implementation = LocalTrendingVideoDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body"
            )
    })
    public ResponseEntity<?> getLocalTrendingPost(
            @Valid @RequestBody LocalTrendingRequest request,
            @AuthenticationPrincipal User user
    ) {
        try {
            log.info("POST /api/trending/local - lat: {}, lon: {}, radius: {}km",
                    request.getLatitude(), request.getLongitude(), request.getRadiusKm());

            Long currentUserId = user != null ? user.getId() : null;
            List<LocalTrendingVideoDTO> trendingVideos = localTrendingService.getLocalTrendingVideos(
                    request,
                    currentUserId
            );

            log.info("Successfully returned {} trending videos", trendingVideos.size());
            return ResponseEntity.ok(trendingVideos);

        } catch (Exception e) {
            log.error("Error in getLocalTrendingPost: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to get local trending videos",
                            "message", e.getMessage(),
                            "details", e.getClass().getSimpleName()
                    ));
        }
    }
}
