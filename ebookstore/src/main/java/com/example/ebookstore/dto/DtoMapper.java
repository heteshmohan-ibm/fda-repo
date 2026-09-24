package com.example.ebookstore.dto;

import com.example.ebookstore.entity.*;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless mapping helpers — entity → DTO.
 * No MapStruct dependency needed for capstone scope.
 */
@UtilityClass
public class DtoMapper {

    public static CategoryDto toDto(Category c) {
        return CategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .build();
    }

    public static BookDto toDto(Book b) {
        return BookDto.builder()
                .id(b.getId())
                .title(b.getTitle())
                .author(b.getAuthor())
                .publisher(b.getPublisher())
                .description(b.getDescription())
                .categoryId(b.getCategory().getId())
                .price(b.getPrice())
                .coverUrl(b.getCoverUrl())
                .availableStock(b.getAvailableStock())
                .build();
    }

    public static UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .giftPointsBalance(u.getGiftPointsBalance())
                .build();
    }

    public static AddressDto toDto(Address a) {
        return AddressDto.builder()
                .id(a.getId())
                .recipient(a.getRecipient())
                .line1(a.getLine1())
                .line2(a.getLine2())
                .city(a.getCity())
                .state(a.getState())
                .postalCode(a.getPostalCode())
                .build();
    }

    public static CartItemDto toDto(CartItem ci) {
        BigDecimal lineTotal = ci.getBook().getPrice()
                .multiply(BigDecimal.valueOf(ci.getQuantity()));
        return CartItemDto.builder()
                .book(toDto(ci.getBook()))
                .quantity(ci.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }

    public static CartDto toDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
        BigDecimal subtotal = items.stream()
                .map(CartItemDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartDto.builder()
                .items(items)
                .subtotal(subtotal)
                .build();
    }

    public static OrderItemDto toDto(OrderItem oi) {
        return OrderItemDto.builder()
                .bookId(oi.getBookId())
                .title(oi.getTitle())
                .unitPrice(oi.getUnitPrice())
                .quantity(oi.getQuantity())
                .lineTotal(oi.getLineTotal())
                .build();
    }

    public static OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
        return OrderDto.builder()
                .id(o.getId())
                .status(o.getStatus().name())
                .items(items)
                .deliveryAddress(toDto(o.getDeliveryAddress()))
                .subtotal(o.getSubtotal())
                .pointsRedeemed(o.getPointsRedeemed())
                .finalTotal(o.getFinalTotal())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
