package com.example.ebookstore.controller;

import com.example.ebookstore.config.AuthUtil;
import com.example.ebookstore.dto.CartDto;
import com.example.ebookstore.dto.OrderDto;
import com.example.ebookstore.dto.PaymentInput;
import com.example.ebookstore.dto.PaymentResultDto;
import com.example.ebookstore.service.OrderService;
import com.example.ebookstore.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService   orderService;
    private final PaymentService paymentService;
    private final AuthUtil       authUtil;

    @GetMapping
    public ResponseEntity<List<OrderDto>> listOrders() {
        return ResponseEntity.ok(orderService.listOrders(authUtil.currentUserId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(authUtil.currentUserId(), orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<PaymentResultDto> pay(
            @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentInput input) {
        return ResponseEntity.ok(
                paymentService.pay(authUtil.currentUserId(), orderId, input, idempotencyKey));
    }

    @PostMapping("/{orderId}/buy-again")
    public ResponseEntity<CartDto> buyAgain(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.buyAgain(authUtil.currentUserId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancel(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(authUtil.currentUserId(), orderId));
    }
}
