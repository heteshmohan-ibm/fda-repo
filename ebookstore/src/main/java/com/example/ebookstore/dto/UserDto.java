package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserDto {
    Long id;
    String name;
    String email;
    Integer giftPointsBalance;
}
