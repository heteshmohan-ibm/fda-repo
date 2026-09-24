package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryDto {
    Long id;
    String name;
}
