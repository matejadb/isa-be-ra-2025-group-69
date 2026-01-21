package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Popular video result from ETL pipeline")
public class PopularVideoDTO {
    @Schema(description = "Video rank (1, 2, or 3)", example = "1")
    private Integer rank;

    @Schema(description = "Video information")
    private VideoResponse video;

    @Schema(description = "Popularity score", example = "42.5")
    private Double popularityScore;

    @Schema(description = "When the ETL pipeline was run")
    private LocalDateTime pipelineRunAt;
}
