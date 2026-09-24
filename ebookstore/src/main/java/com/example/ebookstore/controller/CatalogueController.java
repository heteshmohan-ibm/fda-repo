package com.example.ebookstore.controller;

import com.example.ebookstore.config.AuthUtil;
import com.example.ebookstore.dto.*;
import com.example.ebookstore.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogueController {

    private final CatalogueService catalogueService;
    private final AuthUtil         authUtil;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> listCategories() {
        return ResponseEntity.ok(catalogueService.listCategories());
    }

    @GetMapping("/publishers")
    public ResponseEntity<List<String>> listPublishers() {
        return ResponseEntity.ok(catalogueService.listPublishers());
    }

    @GetMapping("/books")
    public ResponseEntity<BookPageDto> listBooks(
            @RequestParam(required = false)              String q,
            @RequestParam(required = false)              Long   categoryId,
            @RequestParam(required = false)              String publisher,
            @RequestParam(defaultValue = "0")            int    page,
            @RequestParam(defaultValue = "20")           int    size) {
        return ResponseEntity.ok(
                catalogueService.listBooks(q, categoryId, publisher, page, size));
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<BookDto> getBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(catalogueService.getBook(bookId));
    }

    @GetMapping("/books/{bookId}/related")
    public ResponseEntity<List<BookDto>> listRelatedBooks(@PathVariable Long bookId) {
        return ResponseEntity.ok(catalogueService.listRelatedBooks(bookId));
    }

    @GetMapping("/me/recommendations")
    public ResponseEntity<List<BookDto>> getRecommendations(
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                catalogueService.getRecommendations(authUtil.currentUserId(), size));
    }
}
