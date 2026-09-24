package com.example.ebookstore;

import com.example.ebookstore.dto.AddressDto;
import com.example.ebookstore.dto.AddressInput;
import com.example.ebookstore.dto.UserDto;
import com.example.ebookstore.entity.Address;
import com.example.ebookstore.entity.User;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.AddressRepository;
import com.example.ebookstore.service.AccountService;
import com.example.ebookstore.util.BaseIntegrationTest;
import com.example.ebookstore.util.TestDataFactory;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ACC-01 through ACC-04
 */
class AccountServiceTest extends BaseIntegrationTest {

    @Autowired AccountService    accountService;
    @Autowired TestDataFactory   factory;
    @Autowired AddressRepository addressRepository;

    User    u1;
    User    u2;
    Address a1;
    Address a2;

    @BeforeEach
    void setUp() {
        u1 = factory.userU1();
        u2 = factory.userU2();
        a1 = factory.addressA1(u1);
        a2 = factory.addressA2(u2);
    }

    /** ACC-01 – GET /me returns demo user's id, email, and point balance */
    @Test
    void acc01_getCurrentUser_returnsCorrectFields() {
        UserDto dto = accountService.getCurrentUser(u1.getId());
        assertThat(dto.getId()).isEqualTo(u1.getId());
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getGiftPointsBalance()).isEqualTo(100);
    }

    /** ACC-02 – GET /me/addresses returns only U1's addresses */
    @Test
    void acc02_listAddresses_returnsOnlyOwnAddresses() {
        List<AddressDto> addresses = accountService.listAddresses(u1.getId());
        assertThat(addresses).extracting(AddressDto::getId)
                .contains(a1.getId())
                .doesNotContain(a2.getId());
    }

    /** ACC-03 – POST /me/addresses creates address and it appears in list */
    @Test
    void acc03_createAddress_appearsInSubsequentList() {
        AddressInput input = new AddressInput();
        input.setRecipient("Alice");
        input.setLine1("5 Park Lane");
        input.setCity("Kolkata");
        input.setState("West Bengal");
        input.setPostalCode("700001");

        AddressDto created = accountService.createAddress(u1.getId(), input);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRecipient()).isEqualTo("Alice");

        List<AddressDto> list = accountService.listAddresses(u1.getId());
        assertThat(list).extracting(AddressDto::getId).contains(created.getId());
    }

    /** ACC-04 – createAddress without postalCode throws validation error */
    @Test
    void acc04_createAddress_missingPostalCode_throwsValidation() {
        AddressInput input = new AddressInput();
        input.setRecipient("Bob");
        input.setLine1("1 Test St");
        input.setCity("Chennai");
        input.setState("Tamil Nadu");
        // postalCode deliberately omitted

        assertThatThrownBy(() -> accountService.createAddress(u1.getId(), input))
                .satisfies(ex -> assertThat(ex)
                        .isInstanceOfAny(
                                ConstraintViolationException.class,
                                jakarta.validation.ConstraintViolationException.class,
                                com.example.ebookstore.exception.BadRequestException.class));
    }

    /** Update address — updated fields persisted */
    @Test
    void acc_updateAddress_updatesFields() {
        AddressInput update = new AddressInput();
        update.setRecipient("Updated Name");
        update.setLine1("New Line 1");
        update.setCity("Pune");
        update.setState("Maharashtra");
        update.setPostalCode("411001");

        AddressDto updated = accountService.updateAddress(u1.getId(), a1.getId(), update);
        assertThat(updated.getRecipient()).isEqualTo("Updated Name");
        assertThat(updated.getCity()).isEqualTo("Pune");
    }

    /** Delete address — no longer in list */
    @Test
    void acc_deleteAddress_removedFromList() {
        accountService.deleteAddress(u1.getId(), a1.getId());
        List<AddressDto> list = accountService.listAddresses(u1.getId());
        assertThat(list).extracting(AddressDto::getId).doesNotContain(a1.getId());
    }

    /** Update address owned by other user throws 404 */
    @Test
    void acc_updateAddress_otherUsersAddress_throws404() {
        AddressInput input = new AddressInput();
        input.setRecipient("X"); input.setLine1("X"); input.setCity("X");
        input.setState("X"); input.setPostalCode("X");

        assertThatThrownBy(() -> accountService.updateAddress(u1.getId(), a2.getId(), input))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
