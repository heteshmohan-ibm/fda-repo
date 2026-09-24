package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderItemDto {
    Long bookId;
    String title;
    BigDecimal unitPrice;
    Integer quantity;
    BigDecimal lineTotal;
}
