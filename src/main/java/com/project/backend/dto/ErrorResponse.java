package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API error response")
public class ErrorResponse {

    @Schema(description = "Error code")
    private String code;

    @Schema(description = "Error message")
    private String message;

    @Schema(description = "Whether authentication is required")
    private boolean authenticationRequired;
}