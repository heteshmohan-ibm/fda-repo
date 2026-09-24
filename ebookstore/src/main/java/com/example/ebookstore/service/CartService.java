package com.example.ebookstore.service;

import com.example.ebookstore.dto.CartDto;
import com.example.ebookstore.dto.DtoMapper;
import com.example.ebookstore.entity.*;
import com.example.ebookstore.exception.ConflictException;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository     bookRepository;
    private final UserRepository     userRepository;

    // ── Get or create cart ────────────────────────────────────────────────────

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    // ── GET /me/cart ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CartDto getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(Cart.builder().build()); // empty cart if none exists yet
        return DtoMapper.toDto(cart);
    }

    // ── POST /me/cart/items ───────────────────────────────────────────────────

    @Transactional
    public CartDto addItem(Long userId, Long bookId, int quantity) {
           if (quantity <= 0) {
        throw new IllegalArgumentException("Quantity must be greater than zero");
    }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        Cart cart = getOrCreateCart(userId);

        cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .ifPresentOrElse(
                        existing -> {
                            int newQty = existing.getQuantity() + quantity;
                            validateStock(book, newQty);
                            existing.setQuantity(newQty);
                            cartItemRepository.save(existing);
                            log.debug("Updated cart item bookId={} qty={}", bookId, newQty);
                        },
                        () -> {
                            validateStock(book, quantity);
                            CartItem item = CartItem.builder()
                                    .cart(cart)
                                    .book(book)
                                    .quantity(quantity)
                                    .build();
                            cart.getItems().add(cartItemRepository.save(item));
                            log.debug("Added new cart item bookId={} qty={}", bookId, quantity);
                        });

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return DtoMapper.toDto(reloadCart(userId));
    }

    // ── PUT /me/cart/items/{bookId} ───────────────────────────────────────────

    @Transactional
    public CartDto updateItem(Long userId, Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found for bookId: " + bookId));

        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found for bookId: " + bookId));

        validateStock(book, quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        log.debug("Set cart item bookId={} qty={}", bookId, quantity);
        return DtoMapper.toDto(reloadCart(userId));
    }

    // ── DELETE /me/cart/items/{bookId} ────────────────────────────────────────

    @Transactional
    public void removeItem(Long userId, Long bookId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found for bookId: " + bookId));

        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found for bookId: " + bookId));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        log.debug("Removed cart item bookId={}", bookId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateStock(Book book, int requestedQty) {
        if (requestedQty > book.getAvailableStock()) {
            throw new ConflictException(
                    String.format("Insufficient stock for '%s': requested %d, available %d",
                            book.getTitle(), requestedQty, book.getAvailableStock()));
        }
    }

    /** Reload the cart with fresh items from DB to get accurate totals. */
    private Cart reloadCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }
}
