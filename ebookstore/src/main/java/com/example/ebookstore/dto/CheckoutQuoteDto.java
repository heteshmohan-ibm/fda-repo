package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CheckoutQuoteDto {
    BigDecimal subtotal;
    Integer pointsRedeemed;
    BigDecimal finalTotal;
}
