package com.example.ebookstore.service;

import com.example.ebookstore.dto.CartDto;
import com.example.ebookstore.dto.DtoMapper;
import com.example.ebookstore.dto.OrderDto;
import com.example.ebookstore.entity.Book;
import com.example.ebookstore.entity.Cart;
import com.example.ebookstore.entity.GiftPointsTransaction;
import com.example.ebookstore.entity.Order;
import com.example.ebookstore.entity.OrderItem;
import com.example.ebookstore.entity.User;
import com.example.ebookstore.exception.ConflictException;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.BookRepository;
import com.example.ebookstore.repository.CartRepository;
import com.example.ebookstore.repository.GiftPointsTransactionRepository;
import com.example.ebookstore.repository.OrderRepository;
import com.example.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository                orderRepository;
    private final BookRepository                 bookRepository;
    private final CartRepository                 cartRepository;
    private final CartService                    cartService;
    private final UserRepository                 userRepository;
    private final GiftPointsTransactionRepository giftPointsTransactionRepository;

    // ── GET /me/orders ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    // ── GET /me/orders/{orderId} ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderDto getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return DtoMapper.toDto(order);
    }

    // ── POST /me/orders/{orderId}/buy-again ───────────────────────────────────

    /**
     * Adds available books from a prior order back into the cart at current prices.
     * Books that are now out of stock are silently skipped.
     * Books already in the cart have their quantity incremented.
     */
    @Transactional
    public CartDto buyAgain(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getItems().isEmpty()) {
            throw new ConflictException("Order " + orderId + " has no items to re-add");
        }

        int added = 0;
        for (OrderItem oi : order.getItems()) {
            bookRepository.findById(oi.getBookId()).ifPresent(book -> {
                if (book.getAvailableStock() > 0) {
                    // Add 1 of each book (or quantity from original order, capped to stock)
                    int qty = Math.min(oi.getQuantity(), book.getAvailableStock());
                    try {
                        cartService.addItem(userId, book.getId(), qty);
                    } catch (ConflictException ex) {
                        // Stock constraint hit mid-loop — skip this book
                        log.warn("Skipping book id={} in buyAgain: {}", book.getId(), ex.getMessage());
                    }
                } else {
                    log.debug("buyAgain: book id={} out of stock, skipping", oi.getBookId());
                }
            });
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder().build());
        log.info("buyAgain completed for order={}, user={}", orderId, userId);
        return DtoMapper.toDto(cart);
    }

    // ── POST /me/orders/{orderId}/cancel ──────────────────────────────────────

    /**
     * Cancels an order that is still in PENDING_PAYMENT or PAYMENT_FAILED state.
     * Restores stock for each item so the catalogue reflects availability correctly.
     * PAID and SHIPPED orders cannot be cancelled.
     */
    @Transactional
    public OrderDto cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() == Order.Status.PAID) {
            throw new ConflictException(
                    "Order " + orderId + " is already paid and cannot be cancelled. " +
                    "Contact support for a refund.");
        }
        if (order.getStatus() == Order.Status.SHIPPED) {
            throw new ConflictException(
                    "Order " + orderId + " has already shipped and cannot be cancelled.");
        }
        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new ConflictException("Order " + orderId + " is already cancelled.");
        }

        // Restore stock for each order item
        for (OrderItem oi : order.getItems()) {
            bookRepository.findById(oi.getBookId()).ifPresent(book -> {
                book.setAvailableStock(book.getAvailableStock() + oi.getQuantity());
                bookRepository.save(book);
                log.debug("Restored {} units of book id={}", oi.getQuantity(), book.getId());
            });
        }

        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);

        // Restore gift points that were deducted at order placement
        if (order.getPointsRedeemed() > 0) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            user.setGiftPointsBalance(user.getGiftPointsBalance() + order.getPointsRedeemed());
            userRepository.save(user);
            giftPointsTransactionRepository.save(
                    GiftPointsTransaction.builder()
                            .user(user)
                            .order(order)
                            .pointsDelta(order.getPointsRedeemed())  // positive = restored
                            .build());
            log.debug("Restored {} points to user={} on cancellation of order={}",
                    order.getPointsRedeemed(), userId, orderId);
        }

        log.info("Order id={} cancelled by user={}", orderId, userId);
        return DtoMapper.toDto(order);
    }

    // ── Helper: load order entity (used by PaymentService) ───────────────────

    @Transactional(readOnly = true)
    public Order loadOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }
}
