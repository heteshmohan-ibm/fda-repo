package com.example.ebookstore.service;

import com.example.ebookstore.dto.*;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.BookRepository;
import com.example.ebookstore.repository.CategoryRepository;
import com.example.ebookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogueService {

    private static final int RELATED_BOOKS_LIMIT = 6;

    private final BookRepository     bookRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository    orderRepository;

    // ── Categories ────────────────────────────────────────────────────────────

    public List<CategoryDto> listCategories() {
        return categoryRepository.findAll().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    // ── Publishers ────────────────────────────────────────────────────────────

    public List<String> listPublishers() {
        return bookRepository.findDistinctPublishers();
    }

    // ── Book search / filter ──────────────────────────────────────────────────

    public BookPageDto listBooks(String q, Long categoryId, String publisher,
                                 int page, int size) {
        // Treat blank strings as null so JPQL IS NULL checks work correctly
        String qParam   = (q         != null && !q.isBlank())         ? q         : null;
        String pubParam = (publisher != null && !publisher.isBlank()) ? publisher : null;

        Page<com.example.ebookstore.entity.Book> result =
                bookRepository.search(qParam, categoryId, pubParam,
                                      PageRequest.of(page, size));

        List<BookDto> content = result.getContent().stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());

        return BookPageDto.builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // ── Single book ───────────────────────────────────────────────────────────

    public BookDto getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .map(DtoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
    }

    // ── Related books (same category, capped at RELATED_BOOKS_LIMIT) ──────────

    public List<BookDto> listRelatedBooks(Long bookId) {
        com.example.ebookstore.entity.Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        return bookRepository
                .findRelated(book.getCategory().getId(), bookId,
                             PageRequest.of(0, RELATED_BOOKS_LIMIT))
                .stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }

    // ── Recommendations (based on purchase history) ───────────────────────────

    public List<BookDto> getRecommendations(Long userId, int size) {
        // Clamp size to a safe range
        int limit = Math.min(Math.max(size, 1), 50);

        List<Long> purchasedCategoryIds = orderRepository.findPurchasedCategoryIdsByUser(userId);
        if (purchasedCategoryIds.isEmpty()) {
            log.debug("No purchase history for user={}, returning empty recommendations", userId);
            return Collections.emptyList();
        }

        List<Long> alreadyPurchasedBookIds = orderRepository.findPurchasedBookIdsByUser(userId);
        if (alreadyPurchasedBookIds.isEmpty()) {
            alreadyPurchasedBookIds = List.of(-1L);
        }

        return bookRepository
                .findRecommendations(purchasedCategoryIds, alreadyPurchasedBookIds,
                                     PageRequest.of(0, limit))
                .stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
    }
}
