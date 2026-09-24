package com.example.ebookstore.controller;

import com.example.ebookstore.config.AuthUtil;
import com.example.ebookstore.dto.CartDto;
import com.example.ebookstore.dto.CartItemInput;
import com.example.ebookstore.dto.QuantityInput;
import com.example.ebookstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final AuthUtil    authUtil;

    @GetMapping
    public ResponseEntity<CartDto> getCart() {
        return ResponseEntity.ok(cartService.getCart(authUtil.currentUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(@Valid @RequestBody CartItemInput input) {
        return ResponseEntity.ok(
                cartService.addItem(authUtil.currentUserId(),
                        input.getBookId(), input.getQuantity()));
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartDto> updateItem(
            @PathVariable Long bookId,
            @Valid @RequestBody QuantityInput input) {
        return ResponseEntity.ok(
                cartService.updateItem(authUtil.currentUserId(), bookId, input.getQuantity()));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long bookId) {
        cartService.removeItem(authUtil.currentUserId(), bookId);
        return ResponseEntity.noContent().build();
    }
}
