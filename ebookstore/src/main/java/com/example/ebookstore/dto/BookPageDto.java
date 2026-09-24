package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BookPageDto {
    List<BookDto> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
