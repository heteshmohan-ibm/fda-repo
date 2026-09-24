package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentResultDto {
    Long orderId;
    String status;           // Order status: PENDING_PAYMENT | PAID | PAYMENT_FAILED
    String paymentStatus;    // APPROVED | DECLINED
    String transactionReference;
}
