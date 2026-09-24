package com.example.ebookstore.controller;

import com.example.ebookstore.config.AuthUtil;
import com.example.ebookstore.dto.AddressDto;
import com.example.ebookstore.dto.AddressInput;
import com.example.ebookstore.dto.UserDto;
import com.example.ebookstore.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AuthUtil       authUtil;

    @GetMapping
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(accountService.getCurrentUser(authUtil.currentUserId()));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDto>> listAddresses() {
        return ResponseEntity.ok(accountService.listAddresses(authUtil.currentUserId()));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressDto> createAddress(@Valid @RequestBody AddressInput input) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAddress(authUtil.currentUserId(), input));
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDto> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressInput input) {
        return ResponseEntity.ok(
                accountService.updateAddress(authUtil.currentUserId(), addressId, input));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        accountService.deleteAddress(authUtil.currentUserId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
