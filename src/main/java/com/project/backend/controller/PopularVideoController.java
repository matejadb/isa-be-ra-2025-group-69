package com.project.backend.controller;

import com.project.backend.dto.PopularVideoDTO;
import com.project.backend.model.User;
import com.project.backend.service.PopularVideoETLService;
import com.project.backend.service.PopularVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/popular-videos")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Popular Videos", description = "ETL-based popular video recommendations")
public class PopularVideoController {

    private final PopularVideoService popularVideoService;
    private final PopularVideoETLService popularVideoETLService;

    @GetMapping
    @Operation(
            summary = "Get top 3 popular videos",
            description = "Returns the top 3 most popular videos based on the latest ETL pipeline run. " +
                         "Popularity is calculated using weighted view counts from the last 7 days."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved popular videos",
                    content = @Content(schema = @Schema(implementation = PopularVideoDTO.class)))
    })
    public ResponseEntity<List<PopularVideoDTO>> getPopularVideos(
            @AuthenticationPrincipal User user
    ) {
        Long currentUserId = user != null ? user.getId() : null;
        List<PopularVideoDTO> popularVideos = popularVideoService.getTop3PopularVideos(currentUserId);
        return ResponseEntity.ok(popularVideos);
    }

    @PostMapping("/run-etl")
    @Operation(
            summary = "Manually trigger ETL pipeline",
            description = "Manually run the ETL pipeline to recalculate popular videos. Available for testing and development.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL pipeline executed successfully")
    })
    public ResponseEntity<?> triggerETL() {
        try {
            popularVideoETLService.runETLPipelineManually();
            return ResponseEntity.ok(Map.of(
                    "message", "ETL pipeline executed successfully",
                    "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "message", "Failed to run ETL pipeline",
                            "error", e.getMessage(),
                            "success", false
                    ));
        }
    }
}
