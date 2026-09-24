package com.example.ebookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutSelection {

    @NotNull(message = "addressId is required")
    private Long addressId;

    @NotNull(message = "pointsToRedeem is required")
    @Min(value = 0, message = "pointsToRedeem cannot be negative")
    private Integer pointsToRedeem;
}
