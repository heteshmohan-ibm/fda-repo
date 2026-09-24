package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class OrderDto {
    Long id;
    String status;
    List<OrderItemDto> items;
    AddressDto deliveryAddress;
    BigDecimal subtotal;
    Integer pointsRedeemed;
    BigDecimal finalTotal;
    LocalDateTime createdAt;
}
