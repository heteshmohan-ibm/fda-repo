package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CartDto {
    List<CartItemDto> items;
    BigDecimal subtotal;
}
