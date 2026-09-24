package com.example.ebookstore.controller;

import com.example.ebookstore.config.AuthUtil;
import com.example.ebookstore.dto.CheckoutQuoteDto;
import com.example.ebookstore.dto.CheckoutSelection;
import com.example.ebookstore.dto.OrderDto;
import com.example.ebookstore.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final AuthUtil        authUtil;

    @PostMapping("/checkout/quote")
    public ResponseEntity<CheckoutQuoteDto> quote(
            @Valid @RequestBody CheckoutSelection selection) {
        return ResponseEntity.ok(
                checkoutService.quote(authUtil.currentUserId(), selection));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> placeOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutSelection selection) {

        Long userId = authUtil.currentUserId();

        // Check BEFORE placing — if the key already exists this is a replay (200)
        boolean isReplay = checkoutService.orderExistsForKey(idempotencyKey);
        OrderDto order   = checkoutService.placeOrder(userId, selection, idempotencyKey);
        HttpStatus status = isReplay ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(order);
    }
}
