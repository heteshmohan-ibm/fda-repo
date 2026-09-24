package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CartItemDto {
    BookDto book;
    Integer quantity;
    BigDecimal lineTotal;
}
