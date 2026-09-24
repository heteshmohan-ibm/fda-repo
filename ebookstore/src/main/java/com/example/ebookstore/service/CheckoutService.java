package com.example.ebookstore.service;

import com.example.ebookstore.dto.CheckoutQuoteDto;
import com.example.ebookstore.dto.CheckoutSelection;
import com.example.ebookstore.dto.DtoMapper;
import com.example.ebookstore.dto.OrderDto;
import com.example.ebookstore.entity.*;
import com.example.ebookstore.exception.BadRequestException;
import com.example.ebookstore.exception.ConflictException;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    /**
     * Gift point conversion rules:
     *  - Earning : 1 point per 100 INR of finalTotal (floor).
     *  - Redeeming: 1 point = 1 INR; finalTotal cannot go below 0.
     */
    private static final int POINTS_PER_100_INR = 1;

    private final CartRepository                 cartRepository;
    private final BookRepository                 bookRepository;
    private final OrderRepository                orderRepository;
    private final UserRepository                 userRepository;
    private final AddressRepository              addressRepository;
    private final GiftPointsTransactionRepository giftPointsTransactionRepository;

    // ── POST /me/checkout/quote ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CheckoutQuoteDto quote(Long userId, CheckoutSelection selection) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty — add books before checking out");
        }

        // Verify address belongs to user
        addressRepository.findByIdAndUserId(selection.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + selection.getAddressId()));

        BigDecimal subtotal = computeSubtotal(cart);
        int pointsToRedeem = resolvePointsToRedeem(userId, selection.getPointsToRedeem(), subtotal);
        BigDecimal finalTotal = computeFinalTotal(subtotal, pointsToRedeem);

        return CheckoutQuoteDto.builder()
                .subtotal(subtotal)
                .pointsRedeemed(pointsToRedeem)
                .finalTotal(finalTotal)
                .build();
    }

    // ── POST /me/orders  (place order) ────────────────────────────────────────

    @Transactional
    public OrderDto placeOrder(Long userId, CheckoutSelection selection, String idempotencyKey) {
        // Idempotency: return existing order if key already used
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(DtoMapper::toDto)
                .orElseGet(() -> createOrder(userId, selection, idempotencyKey));
    }

    // ── Internal: create order in one transaction ─────────────────────────────

    private OrderDto createOrder(Long userId, CheckoutSelection selection, String idempotencyKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty — add books before placing an order");
        }

        Address address = addressRepository.findByIdAndUserId(selection.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + selection.getAddressId()));

        // Validate stock and build order items (snapshot prices)
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            Book book = bookRepository.findById(ci.getBook().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Book", ci.getBook().getId()));
            if (ci.getQuantity() > book.getAvailableStock()) {
                throw new ConflictException(
                        String.format("Insufficient stock for '%s': requested %d, available %d",
                                book.getTitle(), ci.getQuantity(), book.getAvailableStock()));
            }
            BigDecimal lineTotal = book.getPrice()
                    .multiply(BigDecimal.valueOf(ci.getQuantity()));
            orderItems.add(OrderItem.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .unitPrice(book.getPrice())
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .build());
        }

        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pointsToRedeem = resolvePointsToRedeem(userId, selection.getPointsToRedeem(), subtotal);
        BigDecimal finalTotal = computeFinalTotal(subtotal, pointsToRedeem);

        // Build and save order
        Order order = Order.builder()
                .user(user)
                .deliveryAddress(address)
                .subtotal(subtotal)
                .pointsRedeemed(pointsToRedeem)
                .finalTotal(finalTotal)
                .idempotencyKey(idempotencyKey)
                .status(Order.Status.PENDING_PAYMENT)
                .build();

        Order saved = orderRepository.save(order);

        // Attach order items
        for (OrderItem oi : orderItems) {
            oi.setOrder(saved);
        }
        saved.getItems().addAll(orderItems);
        orderRepository.save(saved);

        // Reserve (deduct) gift points immediately if redeemed
        if (pointsToRedeem > 0) {
            deductPoints(user, saved, pointsToRedeem);
        }

        // Decrement stock
        for (CartItem ci : cart.getItems()) {
            Book book = bookRepository.findById(ci.getBook().getId()).orElseThrow();
            book.setAvailableStock(book.getAvailableStock() - ci.getQuantity());
            bookRepository.save(book);
        }

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order id={} placed for user={}, total={}", saved.getId(), userId, finalTotal);
        return DtoMapper.toDto(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal computeSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(ci -> ci.getBook().getPrice()
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Clamp pointsToRedeem:
     *  - Cannot exceed the user's current balance.
     *  - Cannot make finalTotal negative (1 point = 1 INR).
     */
    private int resolvePointsToRedeem(Long userId, int requested, BigDecimal subtotal) {
        if (requested == 0) return 0;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        int balance = user.getGiftPointsBalance();
        if (requested > balance) {
            throw new ConflictException(
                    String.format("Insufficient gift points: requested %d, balance %d",
                            requested, balance));
        }

        // Cap so finalTotal doesn't go below 0
        BigDecimal maxRedeemable = subtotal.max(BigDecimal.ZERO);
        int maxPoints = maxRedeemable.intValue(); // 1 point = 1 INR
        return Math.min(requested, maxPoints);
    }

    private BigDecimal computeFinalTotal(BigDecimal subtotal, int pointsRedeemed) {
        BigDecimal discount = BigDecimal.valueOf(pointsRedeemed);
        BigDecimal total = subtotal.subtract(discount);
        return total.max(BigDecimal.ZERO); // floor at zero
    }

    private void deductPoints(User user, Order order, int points) {
        user.setGiftPointsBalance(user.getGiftPointsBalance() - points);
        userRepository.save(user);

        GiftPointsTransaction tx = GiftPointsTransaction.builder()
                .user(user)
                .order(order)
                .pointsDelta(-points)
                .build();
        giftPointsTransactionRepository.save(tx);
        log.debug("Deducted {} points from user={}", points, user.getId());
    }

    /**
     * Returns true if an order with this idempotency key already exists.
     * Call this BEFORE calling placeOrder to determine the correct HTTP status.
     */
    @Transactional(readOnly = true)
    public boolean orderExistsForKey(String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    /**
     * Called by PaymentService after a successful payment to credit earned points.
     * Earning rule: floor(finalTotal / 100) points.
     */
    @Transactional
    public void creditEarnedPoints(User user, Order order) {
        int earned = order.getFinalTotal()
                .divideToIntegralValue(BigDecimal.valueOf(100))
                .intValue() * POINTS_PER_100_INR;

        if (earned <= 0) return;

        user.setGiftPointsBalance(user.getGiftPointsBalance() + earned);
        userRepository.save(user);

        GiftPointsTransaction tx = GiftPointsTransaction.builder()
                .user(user)
                .order(order)
                .pointsDelta(earned)
                .build();
        giftPointsTransactionRepository.save(tx);
        log.debug("Credited {} points to user={} for order={}", earned, user.getId(), order.getId());
    }
}
