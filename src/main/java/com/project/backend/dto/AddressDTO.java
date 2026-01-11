package com.project.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Address information")
public class AddressDTO {
    @NotBlank(message = "Street is required")
    @Schema(description = "Street address", example = "123 Main Street", required = true)
    private String street;

    @NotBlank(message = "City is required")
    @Schema(description = "City name", example = "Belgrade", required = true)
    private String city;

    @Schema(description = "Postal code", example = "11000")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Schema(description = "Country name", example = "Serbia", required = true)
    private String country;
}
