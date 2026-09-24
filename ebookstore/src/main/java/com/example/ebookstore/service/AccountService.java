package com.example.ebookstore.service;

import com.example.ebookstore.dto.AddressDto;
import com.example.ebookstore.dto.AddressInput;
import com.example.ebookstore.dto.DtoMapper;
import com.example.ebookstore.dto.UserDto;
import com.example.ebookstore.entity.Address;
import com.example.ebookstore.entity.User;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.AddressRepository;
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
public class AccountService {

    private final UserRepository    userRepository;
    private final AddressRepository addressRepository;

    // ── GET /me ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return DtoMapper.toDto(user);
    }

    // ── GET /me/addresses ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AddressDto> listAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    // ── POST /me/addresses ────────────────────────────────────────────────────

    @Transactional
    public AddressDto createAddress(Long userId, AddressInput input) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Address address = Address.builder()
                .user(user)
                .recipient(input.getRecipient())
                .line1(input.getLine1())
                .line2(input.getLine2())
                .city(input.getCity())
                .state(input.getState())
                .postalCode(input.getPostalCode())
                .build();

        Address saved = addressRepository.save(address);
        log.debug("Created address id={} for user={}", saved.getId(), userId);
        return DtoMapper.toDto(saved);
    }

    // ── PUT /me/addresses/{addressId} ────────────────────────────────────────

    @Transactional
    public AddressDto updateAddress(Long userId, Long addressId, AddressInput input) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId));

        address.setRecipient(input.getRecipient());
        address.setLine1(input.getLine1());
        address.setLine2(input.getLine2());
        address.setCity(input.getCity());
        address.setState(input.getState());
        address.setPostalCode(input.getPostalCode());

        Address saved = addressRepository.save(address);
        log.debug("Updated address id={} for user={}", addressId, userId);
        return DtoMapper.toDto(saved);
    }

    // ── DELETE /me/addresses/{addressId} ──────────────────────────────────────

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId));
        addressRepository.delete(address);
        log.debug("Deleted address id={} for user={}", addressId, userId);
    }

    // ── Helper: load & verify address ownership (used by CheckoutService) ─────

    @Transactional(readOnly = true)
    public Address getAddressForUser(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId));
    }
}
