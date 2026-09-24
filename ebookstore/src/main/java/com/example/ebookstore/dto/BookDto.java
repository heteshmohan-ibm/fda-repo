package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class BookDto {
    Long id;
    String title;
    String author;
    String publisher;
    String description;
    Long categoryId;
    BigDecimal price;
    String coverUrl;
    Integer availableStock;
}
