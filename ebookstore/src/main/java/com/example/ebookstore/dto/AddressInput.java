package com.example.ebookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressInput {

    @NotBlank(message = "Recipient is required")
    private String recipient;

    @NotBlank(message = "Line 1 is required")
    private String line1;

    private String line2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;
}
