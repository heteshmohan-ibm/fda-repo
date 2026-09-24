package com.example.ebookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentInput {

    @NotNull(message = "method is required")
    private String method;          // CREDIT_CARD | DEBIT_CARD

    @NotNull(message = "simulateOutcome is required")
    private String simulateOutcome; // APPROVED | DECLINED
}
