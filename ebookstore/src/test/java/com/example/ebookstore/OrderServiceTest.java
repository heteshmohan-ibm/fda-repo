package com.example.ebookstore;

import com.example.ebookstore.dto.*;
import com.example.ebookstore.entity.*;
import com.example.ebookstore.exception.ConflictException;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.BookRepository;
import com.example.ebookstore.repository.UserRepository;
import com.example.ebookstore.service.*;
import com.example.ebookstore.util.BaseIntegrationTest;
import com.example.ebookstore.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * ORD-01 through ORD-03, AGAIN-01 through AGAIN-03
 */
class OrderServiceTest extends BaseIntegrationTest {

    @Autowired OrderService    orderService;
    @Autowired PaymentService  paymentService;
    @Autowired CheckoutService checkoutService;
    @Autowired CartService     cartService;
    @Autowired TestDataFactory factory;
    @Autowired UserRepository  userRepository;
    @Autowired BookRepository  bookRepository;

    User    u1;
    Book    b1;
    Address a1;

    @BeforeEach
    void setUp() {
        u1 = factory.userU1();
        Category fiction = factory.fiction();
        b1 = factory.bookB1(fiction);           // ₹400, stock 5
        a1 = factory.addressA1(u1);
        factory.emptyCart(u1);
    }

    /** Place and pay a full order, returns DTO */
    private OrderDto placePaidOrder(int pointsToRedeem) {
        cartService.addItem(u1.getId(), b1.getId(), 1);
        CheckoutSelection sel = new CheckoutSelection();
        sel.setAddressId(a1.getId());
        sel.setPointsToRedeem(pointsToRedeem);
        String orderKey = UUID.randomUUID().toString();
        OrderDto order = checkoutService.placeOrder(u1.getId(), sel, orderKey);

        PaymentInput pay = new PaymentInput();
        pay.setMethod("CREDIT_CARD");
        pay.setSimulateOutcome("APPROVED");
        paymentService.pay(u1.getId(), order.getId(), pay, UUID.randomUUID().toString());
        return orderService.getOrder(u1.getId(), order.getId());
    }

    /** ORD-01 – get paid order returns PAID status, correct items, address, totals, timestamp */
    @Test
    void ord01_getPaidOrder_returnsFullDetails() {
        OrderDto paid = placePaidOrder(0);

        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(paid.getItems()).hasSize(1);
        assertThat(paid.getItems().get(0).getTitle()).isEqualTo(b1.getTitle());
        assertThat(paid.getItems().get(0).getUnitPrice())
                .isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(paid.getDeliveryAddress().getId()).isEqualTo(a1.getId());
        assertThat(paid.getFinalTotal()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(paid.getCreatedAt()).isNotNull();
    }

    /** ORD-02 – list orders includes new order, newest first */
    @Test
    void ord02_listOrders_includesPaidOrder_newestFirst() {
        OrderDto first  = placePaidOrder(0);
        cartService.addItem(u1.getId(), b1.getId(), 1);
        CheckoutSelection sel2 = new CheckoutSelection();
        sel2.setAddressId(a1.getId());
        sel2.setPointsToRedeem(0);
        checkoutService.placeOrder(u1.getId(), sel2, UUID.randomUUID().toString());

        List<OrderDto> orders = orderService.listOrders(u1.getId());
        assertThat(orders).isNotEmpty();
        // newest first — second order created after first
        assertThat(orders.get(0).getCreatedAt())
                .isAfterOrEqualTo(orders.get(orders.size() - 1).getCreatedAt());

        assertThat(orders).extracting(OrderDto::getId).contains(first.getId());
    }

    /** ORD-03 – changing catalogue price after purchase does NOT alter order stored price */
    @Test
    void ord03_priceChangeAfterPurchase_doesNotAlterOrderSnapshot() {
        OrderDto paid = placePaidOrder(0);
        BigDecimal originalPrice = paid.getItems().get(0).getUnitPrice();

        // Change catalogue price
        b1.setPrice(new BigDecimal("999.00"));
        bookRepository.save(b1);

        OrderDto fetched = orderService.getOrder(u1.getId(), paid.getId());
        assertThat(fetched.getItems().get(0).getUnitPrice())
                .isEqualByComparingTo(originalPrice);
    }

    /** AGAIN-01 – Buy Again when B1 has stock adds B1 to cart at current price */
    @Test
    void again01_buyAgain_addsAvailableItemsToCart() {
        Order paidOrder = factory.paidOrderO1(u1, b1, a1);
        factory.emptyCart(u1);  // ensure clean cart

        CartDto cart = orderService.buyAgain(u1.getId(), paidOrder.getId());
        assertThat(cart.getItems()).isNotEmpty();
        assertThat(cart.getItems()).extracting(ci -> ci.getBook().getId())
                .contains(b1.getId());
        // Price in cart should reflect current catalogue price
        assertThat(cart.getItems().get(0).getBook().getPrice())
                .isEqualByComparingTo(b1.getPrice());
    }

    /** AGAIN-02 – Buy Again when B1 has stock 0 → cart unchanged */
    @Test
    void again02_buyAgain_outOfStockItem_cartUnchanged() {
        Order paidOrder = factory.paidOrderO1(u1, b1, a1);
        factory.emptyCart(u1);

        // Set B1 out of stock
        b1.setAvailableStock(0);
        bookRepository.save(b1);

        // buyAgain skips out-of-stock items silently — cart should be empty
        CartDto cart = orderService.buyAgain(u1.getId(), paidOrder.getId());
        assertThat(cart.getItems()).isEmpty();
    }

    /** AGAIN-03 – Buy Again for non-existent order → ResourceNotFoundException */
    @Test
    void again03_unknownOrderId_throwsNotFound() {
        assertThatThrownBy(() -> orderService.buyAgain(u1.getId(), 999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Cancel PENDING_PAYMENT order → CANCELLED, stock restored, points restored */
    @Test
    void cancel_pendingOrder_restoresStockAndPoints() {
        cartService.addItem(u1.getId(), b1.getId(), 2);
        CheckoutSelection sel = new CheckoutSelection();
        sel.setAddressId(a1.getId());
        sel.setPointsToRedeem(50);
        OrderDto pending = checkoutService.placeOrder(
                u1.getId(), sel, UUID.randomUUID().toString());

        // Stock should have been decremented by 2
        assertThat(bookRepository.findById(b1.getId()).orElseThrow().getAvailableStock())
                .isEqualTo(3);

        OrderDto cancelled = orderService.cancelOrder(u1.getId(), pending.getId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");

        // Stock restored
        assertThat(bookRepository.findById(b1.getId()).orElseThrow().getAvailableStock())
                .isEqualTo(5);

        // Points restored
        User fresh = userRepository.findById(u1.getId()).orElseThrow();
        assertThat(fresh.getGiftPointsBalance()).isEqualTo(100);
    }

    /** Cancel PAID order → ConflictException */
    @Test
    void cancel_paidOrder_throwsConflict() {
        OrderDto paid = placePaidOrder(0);
        assertThatThrownBy(() -> orderService.cancelOrder(u1.getId(), paid.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("paid");
    }
}
