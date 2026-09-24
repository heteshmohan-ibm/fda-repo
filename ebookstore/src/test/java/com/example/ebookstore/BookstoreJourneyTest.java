package com.example.ebookstore;

import com.example.ebookstore.dto.*;
import com.example.ebookstore.entity.*;
import com.example.ebookstore.repository.BookRepository;
import com.example.ebookstore.repository.UserRepository;
import com.example.ebookstore.service.*;
import com.example.ebookstore.util.BaseIntegrationTest;
import com.example.ebookstore.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end acceptance test — 8 steps from the MD spec.
 *
 * Step 1  Fetch books and an address.
 * Step 2  Add B1 quantity 1 to cart; assert subtotal ₹400.00.
 * Step 3  Quote checkout with A1 and 50 points; assert ₹350.00 due.
 * Step 4  Create order with an idempotency key; assert PENDING_PAYMENT.
 * Step 5  Simulate approved card payment; assert PAID.
 * Step 6  Fetch order and history; assert item, address, amount, point deduction.
 * Step 7  Repeat payment request; assert no duplicate charge / deduction.
 * Step 8  Use Buy Again; assert B1 appears in cart at current price.
 */
class BookstoreJourneyTest extends BaseIntegrationTest {

    @Autowired CatalogueService catalogueService;
    @Autowired AccountService   accountService;
    @Autowired CartService      cartService;
    @Autowired CheckoutService  checkoutService;
    @Autowired PaymentService   paymentService;
    @Autowired OrderService     orderService;
    @Autowired TestDataFactory  factory;
    @Autowired UserRepository   userRepository;
    @Autowired BookRepository   bookRepository;

    @Test
    void fullBookstoreJourney_8Steps() {

        // ── Fixtures ──────────────────────────────────────────────────────────
        User u1          = factory.userU1();          // 100 gift points
        Category fiction = factory.fiction();
        Book b1          = factory.bookB1(fiction);   // ₹400, stock 5
        Address a1       = factory.addressA1(u1);
        factory.emptyCart(u1);

        // ── Step 1: Fetch books and verify address ────────────────────────────
        BookPageDto books = catalogueService.listBooks(null, null, null, 0, 20);
        assertThat(books.getContent()).isNotEmpty();

        List<AddressDto> addresses = accountService.listAddresses(u1.getId());
        assertThat(addresses).extracting(AddressDto::getId).contains(a1.getId());

        // ── Step 2: Add B1 qty 1, assert subtotal ₹400 ───────────────────────
        CartDto cart = cartService.addItem(u1.getId(), b1.getId(), 1);
        assertThat(cart.getSubtotal()).isEqualByComparingTo(new BigDecimal("400.00"));

        // ── Step 3: Quote with 50 points, assert ₹350 ────────────────────────
        CheckoutSelection sel = new CheckoutSelection();
        sel.setAddressId(a1.getId());
        sel.setPointsToRedeem(50);
        CheckoutQuoteDto quote = checkoutService.quote(u1.getId(), sel);
        assertThat(quote.getFinalTotal()).isEqualByComparingTo(new BigDecimal("350.00"));

        // User balance unchanged after quote
        assertThat(userRepository.findById(u1.getId()).orElseThrow()
                .getGiftPointsBalance()).isEqualTo(100);

        // ── Step 4: Place order, assert PENDING_PAYMENT ───────────────────────
        String orderKey = UUID.randomUUID().toString();
        OrderDto order = checkoutService.placeOrder(u1.getId(), sel, orderKey);
        assertThat(order.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.getFinalTotal()).isEqualByComparingTo(new BigDecimal("350.00"));
        Long orderId = order.getId();

        // Points deducted at order placement
        assertThat(userRepository.findById(u1.getId()).orElseThrow()
                .getGiftPointsBalance()).isEqualTo(50);

        // ── Step 5: Simulate APPROVED payment, assert PAID ───────────────────
        PaymentInput payInput = new PaymentInput();
        payInput.setMethod("CREDIT_CARD");
        payInput.setSimulateOutcome("APPROVED");
        String payKey = UUID.randomUUID().toString();
        PaymentResultDto result = paymentService.pay(u1.getId(), orderId, payInput, payKey);
        assertThat(result.getPaymentStatus()).isEqualTo("APPROVED");
        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getTransactionReference()).isNotBlank();

        // ── Step 6: Fetch order and history ───────────────────────────────────
        OrderDto paidOrder = orderService.getOrder(u1.getId(), orderId);
        assertThat(paidOrder.getStatus()).isEqualTo("PAID");
        assertThat(paidOrder.getItems()).hasSize(1);
        assertThat(paidOrder.getItems().get(0).getTitle()).isEqualTo(b1.getTitle());
        assertThat(paidOrder.getItems().get(0).getUnitPrice())
                .isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(paidOrder.getDeliveryAddress().getId()).isEqualTo(a1.getId());
        assertThat(paidOrder.getFinalTotal()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(paidOrder.getCreatedAt()).isNotNull();

        // Points: 50 deducted at order, earned = floor(350/100) = 3 → balance = 53
        User freshUser = userRepository.findById(u1.getId()).orElseThrow();
        assertThat(freshUser.getGiftPointsBalance()).isEqualTo(53);

        // Stock decremented 5 → 4
        assertThat(bookRepository.findById(b1.getId()).orElseThrow()
                .getAvailableStock()).isEqualTo(4);

        // Order appears in history
        List<OrderDto> history = orderService.listOrders(u1.getId());
        assertThat(history).extracting(OrderDto::getId).contains(orderId);

        // ── Step 7: Repeat payment — idempotent, no duplicate ─────────────────
        PaymentResultDto replay = paymentService.pay(u1.getId(), orderId, payInput, payKey);
        assertThat(replay.getTransactionReference())
                .isEqualTo(result.getTransactionReference());

        // Balance and stock must not change on replay
        assertThat(userRepository.findById(u1.getId()).orElseThrow()
                .getGiftPointsBalance()).isEqualTo(53);
        assertThat(bookRepository.findById(b1.getId()).orElseThrow()
                .getAvailableStock()).isEqualTo(4);

        // ── Step 8: Buy Again — B1 at current catalogue price ─────────────────
        CartDto newCart = orderService.buyAgain(u1.getId(), orderId);
        assertThat(newCart.getItems()).isNotEmpty();
        assertThat(newCart.getItems().get(0).getBook().getId()).isEqualTo(b1.getId());
        assertThat(newCart.getItems().get(0).getBook().getPrice())
                .isEqualByComparingTo(b1.getPrice());
    }
}
