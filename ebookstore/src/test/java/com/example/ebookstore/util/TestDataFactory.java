package com.example.ebookstore.util;

import com.example.ebookstore.entity.*;
import com.example.ebookstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Shared fixture builder used by all integration tests.
 * Each test method should call reset() via @BeforeEach or @Transactional rollback.
 */
@Component
@RequiredArgsConstructor
public class TestDataFactory {

    private final CategoryRepository             categoryRepository;
    private final BookRepository                 bookRepository;
    private final UserRepository                 userRepository;
    private final AddressRepository              addressRepository;
    private final CartRepository                 cartRepository;
    private final OrderRepository                orderRepository;
    private final PaymentRepository              paymentRepository;
    private final GiftPointsTransactionRepository giftPointsTransactionRepository;
    private final PasswordEncoder                passwordEncoder;

    // ── Categories ────────────────────────────────────────────────────────────

    public Category fiction() {
        return categoryRepository.save(Category.builder().name("Fiction").build());
    }

    public Category science() {
        return categoryRepository.save(Category.builder().name("Science").build());
    }

    // ── Books ─────────────────────────────────────────────────────────────────

    /** B1: Fiction, publisher P1, ₹400, stock 5 */
    public Book bookB1(Category category) {
        return bookRepository.save(Book.builder()
                .title("Clean Code")
                .author("Robert Martin")
                .publisher("P1")
                .description("A handbook of agile craftsmanship.")
                .price(new BigDecimal("400.00"))
                .availableStock(5)
                .category(category)
                .build());
    }

    /** B2: Fiction, publisher P2, ₹250, stock 2 */
    public Book bookB2(Category category) {
        return bookRepository.save(Book.builder()
                .title("The Pragmatic Programmer")
                .author("Andrew Hunt")
                .publisher("P2")
                .description("Your journey to mastery.")
                .price(new BigDecimal("250.00"))
                .availableStock(2)
                .category(category)
                .build());
    }

    /** B3: Science, publisher P1, stock 0 (out of stock) */
    public Book bookB3OutOfStock(Category category) {
        return bookRepository.save(Book.builder()
                .title("A Brief History of Time")
                .author("Stephen Hawking")
                .publisher("P1")
                .description("The universe explained.")
                .price(new BigDecimal("300.00"))
                .availableStock(0)
                .category(category)
                .build());
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    /** U1: demo user with 100 gift points */
    public User userU1() {
        return userRepository.save(User.builder()
                .name("Test User")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .giftPointsBalance(100)
                .build());
    }

    /** A second user for ownership tests */
    public User userU2() {
        return userRepository.save(User.builder()
                .name("Other User")
                .email("other@example.com")
                .password(passwordEncoder.encode("password123"))
                .giftPointsBalance(0)
                .build());
    }

    // ── Addresses ─────────────────────────────────────────────────────────────

    /** A1: address belonging to U1 */
    public Address addressA1(User user) {
        return addressRepository.save(Address.builder()
                .user(user)
                .recipient("Test User")
                .line1("1 Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .build());
    }

    /** A2: address belonging to U2 (for ownership test) */
    public Address addressA2(User otherUser) {
        return addressRepository.save(Address.builder()
                .user(otherUser)
                .recipient("Other User")
                .line1("99 Other Road")
                .city("Delhi")
                .state("Delhi")
                .postalCode("110001")
                .build());
    }

    // ── Cart ──────────────────────────────────────────────────────────────────

   public Cart emptyCart(User user) {
    return cartRepository.findByUserId(user.getId())
            .orElseGet(() -> cartRepository.save(
                    Cart.builder().user(user).build()
            ));
}

    public CartItem addToCart(Cart cart, Book book, int quantity) {
        CartItem item = CartItem.builder()
                .cart(cart)
                .book(book)
                .quantity(quantity)
                .build();
        cart.getItems().add(item);
        cartRepository.save(cart);
        return item;
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    /** Creates a PAID order for U1 containing B1 (for Buy Again / recommendation tests) */
    public Order paidOrderO1(User user, Book book, Address address) {
        OrderItem oi = OrderItem.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .unitPrice(book.getPrice())
                .quantity(1)
                .lineTotal(book.getPrice())
                .build();

        Order order = Order.builder()
                .user(user)
                .deliveryAddress(address)
                .subtotal(book.getPrice())
                .pointsRedeemed(0)
                .finalTotal(book.getPrice())
                .status(Order.Status.PAID)
                .idempotencyKey("seed-paid-order-key")
                .build();

        order = orderRepository.save(order);
        oi.setOrder(order);
        order.getItems().add(oi);
        return orderRepository.save(order);
    }
}
