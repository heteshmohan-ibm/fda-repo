package com.example.ebookstore;

import com.example.ebookstore.dto.BookDto;
import com.example.ebookstore.dto.CartDto;
import com.example.ebookstore.entity.*;
import com.example.ebookstore.exception.ConflictException;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.UserRepository;
import com.example.ebookstore.service.CartService;
import com.example.ebookstore.service.CatalogueService;
import com.example.ebookstore.util.BaseIntegrationTest;
import com.example.ebookstore.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * CART-01 through CART-09, REC-01 through REC-02
 */
class CartServiceTest extends BaseIntegrationTest {

    @Autowired CartService      cartService;
    @Autowired CatalogueService catalogueService;
    @Autowired TestDataFactory  factory;
    @Autowired UserRepository   userRepository;

    User     u1;
    Category fiction;
    Category science;
    Book     b1;   // Fiction, P1, ₹400, stock 5
    Book     b2;   // Fiction, P2, ₹250, stock 2
    Book     b3;   // Science, P1, stock 0

    @BeforeEach
    void setUp() {
        u1      = factory.userU1();
        fiction = factory.fiction();
        science = factory.science();
        b1      = factory.bookB1(fiction);
        b2      = factory.bookB2(fiction);
        b3      = factory.bookB3OutOfStock(science);
        factory.emptyCart(u1);
    }

    /** CART-01 – empty cart returns items=[] and subtotal 0.00 */
    @Test
    void cart01_emptyCart_returnsZeroSubtotal() {
        CartDto cart = cartService.getCart(u1.getId());
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /** CART-02 – add B1 qty 2; subtotal ₹800 */
    @Test
    void cart02_addB1Qty2_subtotalIs800() {
        CartDto cart = cartService.addItem(u1.getId(), b1.getId(), 2);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems().get(0).getLineTotal())
                .isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(cart.getSubtotal()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    /** CART-03 – adding B1 again increments quantity; subtotal ₹1200 */
    @Test
    void cart03_addB1Again_incrementsQuantity() {
        cartService.addItem(u1.getId(), b1.getId(), 2);
        CartDto cart = cartService.addItem(u1.getId(), b1.getId(), 1);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(cart.getSubtotal()).isEqualByComparingTo(new BigDecimal("1200.00"));
    }

    /** CART-04 – PUT sets absolute quantity; subtotal ₹400 */
    @Test
    void cart04_updateItem_setsAbsoluteQuantity() {
        cartService.addItem(u1.getId(), b1.getId(), 2);
        CartDto cart = cartService.updateItem(u1.getId(), b1.getId(), 1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(1);
        assertThat(cart.getSubtotal()).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    /** CART-05 – DELETE item; subsequent cart omits B1 */
    @Test
    void cart05_removeItem_notInCart() {
        cartService.addItem(u1.getId(), b1.getId(), 1);
        cartService.removeItem(u1.getId(), b1.getId());
        CartDto cart = cartService.getCart(u1.getId());
        assertThat(cart.getItems()).isEmpty();
    }

    /** CART-06 – add quantity 0 is rejected (validation, min=1) */
    @Test
    void cart06_addQuantityZero_rejected() {
        // quantity=0 violates @Min(1) — service throws or Spring validation rejects
        assertThatThrownBy(() -> cartService.addItem(u1.getId(), b1.getId(), 0))
                .isInstanceOfAny(
                        ConflictException.class,
                        IllegalArgumentException.class,
                        jakarta.validation.ConstraintViolationException.class);

        // Cart must remain unchanged
        CartDto cart = cartService.getCart(u1.getId());
        assertThat(cart.getItems()).isEmpty();
    }

    /** CART-07 – add qty 6 when stock is 5 → ConflictException; cart unchanged */
    @Test
    void cart07_exceedStock_throwsConflict() {
        assertThatThrownBy(() -> cartService.addItem(u1.getId(), b1.getId(), 6))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("stock");
        assertThat(cartService.getCart(u1.getId()).getItems()).isEmpty();
    }

    /** CART-08 – add non-existent book → ResourceNotFoundException; cart unchanged */
    @Test
    void cart08_unknownBook_throwsNotFound() {
        assertThatThrownBy(() -> cartService.addItem(u1.getId(), 999_999L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(cartService.getCart(u1.getId()).getItems()).isEmpty();
    }

    /** CART-09 – add B3 (stock 0) → ConflictException */
    @Test
    void cart09_outOfStockBook_throwsConflict() {
        assertThatThrownBy(() -> cartService.addItem(u1.getId(), b3.getId(), 1))
                .isInstanceOf(ConflictException.class);
    }

    /** REC-01 – after a PAID order containing B1, recommendations include B2 but not B1 */
    @Test
    void rec01_withPaidHistory_recommendsRelatedExcludesPurchased() {
        Address a1 = factory.addressA1(u1);
        factory.paidOrderO1(u1, b1, a1);

        List<BookDto> recs = catalogueService.getRecommendations(u1.getId(), 10);
        List<Long> ids = recs.stream().map(BookDto::getId).toList();

        assertThat(ids).doesNotContain(b1.getId());
        // B2 is same-category Fiction — eligible
        assertThat(ids).contains(b2.getId());
    }

    /** REC-02 – no paid order history → empty recommendations */
    @Test
    void rec02_noPaidHistory_returnsEmpty() {
        List<BookDto> recs = catalogueService.getRecommendations(u1.getId(), 10);
        assertThat(recs).isEmpty();
    }
}
