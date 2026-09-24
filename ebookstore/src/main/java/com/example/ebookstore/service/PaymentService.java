package com.example.ebookstore.service;

import com.example.ebookstore.dto.PaymentInput;
import com.example.ebookstore.dto.PaymentResultDto;
import com.example.ebookstore.entity.GiftPointsTransaction;
import com.example.ebookstore.entity.Order;
import com.example.ebookstore.entity.Payment;
import com.example.ebookstore.entity.User;
import com.example.ebookstore.exception.BadRequestException;
import com.example.ebookstore.exception.ConflictException;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.GiftPointsTransactionRepository;
import com.example.ebookstore.repository.PaymentRepository;
import com.example.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository              paymentRepository;
    private final UserRepository                 userRepository;
    private final GiftPointsTransactionRepository giftPointsTransactionRepository;
    private final OrderService                   orderService;
    private final CheckoutService                checkoutService;

    // ── POST /me/orders/{orderId}/pay ─────────────────────────────────────────

    @Transactional
    public PaymentResultDto pay(Long userId, Long orderId,
                                PaymentInput input, String idempotencyKey) {

        // Idempotency: return previous payment result if key already used
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toResult)
                .orElseGet(() -> processPayment(userId, orderId, input, idempotencyKey));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private PaymentResultDto processPayment(Long userId, Long orderId,
                                            PaymentInput input, String idempotencyKey) {

        Order order = orderService.loadOrder(userId, orderId);

        // Only PENDING_PAYMENT orders can be paid
        if (order.getStatus() == Order.Status.PAID) {
            throw new ConflictException("Order " + orderId + " is already paid");
        }
        if (order.getStatus() == Order.Status.PAYMENT_FAILED) {
            throw new ConflictException(
                    "Order " + orderId + " previously failed payment. " +
                    "Place a new order to retry.");
        }

        // Validate enum inputs
        Payment.Method method = parseMethod(input.getMethod());
        Payment.Status outcome = parseOutcome(input.getSimulateOutcome());

        // Build payment record
        String txRef = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .amount(order.getFinalTotal())
                .status(outcome)
                .transactionReference(txRef)
                .idempotencyKey(idempotencyKey)
                .build();
        paymentRepository.save(payment);

        // Update order status
        if (outcome == Payment.Status.APPROVED) {
            order.setStatus(Order.Status.PAID);
            orderService.saveOrder(order);

            // Credit earned gift points
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            checkoutService.creditEarnedPoints(user, order);
            log.info("Payment APPROVED for order={}, txRef={}", orderId, txRef);
        } else {
            order.setStatus(Order.Status.PAYMENT_FAILED);
            orderService.saveOrder(order);

            // Restore any gift points that were reserved at order placement
            if (order.getPointsRedeemed() > 0) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                restorePoints(user, order, order.getPointsRedeemed());
            }
            log.info("Payment DECLINED for order={}, txRef={}", orderId, txRef);
        }

        return toResult(payment);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PaymentResultDto toResult(Payment p) {
        return PaymentResultDto.builder()
                .orderId(p.getOrder().getId())
                .status(p.getOrder().getStatus().name())
                .paymentStatus(p.getStatus().name())
                .transactionReference(p.getTransactionReference())
                .build();
    }

    /**
     * Restores gift points to the user when a payment is DECLINED or an order
     * is cancelled. Records a positive-delta GiftPointsTransaction for audit.
     */
    private void restorePoints(User user, Order order, int points) {
        user.setGiftPointsBalance(user.getGiftPointsBalance() + points);
        userRepository.save(user);
        giftPointsTransactionRepository.save(
                GiftPointsTransaction.builder()
                        .user(user)
                        .order(order)
                        .pointsDelta(points)   // positive = restored
                        .build());
        log.debug("Restored {} points to user={} for order={}", points, user.getId(), order.getId());
    }

    private Payment.Method parseMethod(String value) {
        try {
            return Payment.Method.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid payment method '" + value + "'. Must be CREDIT_CARD or DEBIT_CARD");
        }
    }

    private Payment.Status parseOutcome(String value) {
        try {
            return Payment.Status.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid simulateOutcome '" + value + "'. Must be APPROVED or DECLINED");
        }
    }
}
