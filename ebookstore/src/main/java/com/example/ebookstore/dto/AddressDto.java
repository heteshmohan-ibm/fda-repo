package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddressDto {
    Long id;
    String recipient;
    String line1;
    String line2;
    String city;
    String state;
    String postalCode;
}
