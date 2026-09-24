package com.example.ebookstore;

import com.example.ebookstore.dto.BookDto;
import com.example.ebookstore.dto.BookPageDto;
import com.example.ebookstore.dto.CategoryDto;
import com.example.ebookstore.entity.Book;
import com.example.ebookstore.entity.Category;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.BookRepository;
import com.example.ebookstore.service.CatalogueService;
import com.example.ebookstore.util.BaseIntegrationTest;
import com.example.ebookstore.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * CAT-01 through CAT-10
 */
class CatalogueServiceTest extends BaseIntegrationTest {

    @Autowired CatalogueService  catalogueService;
    @Autowired TestDataFactory   factory;
    @Autowired BookRepository    bookRepository;

    Category fiction;
    Category science;
    Book     b1;   // Fiction, P1, ₹400, stock 5
    Book     b2;   // Fiction, P2, ₹250, stock 2
    Book     b3;   // Science, P1, stock 0

    @BeforeEach
    void setUp() {
        fiction = factory.fiction();
        science = factory.science();
        b1 = factory.bookB1(fiction);
        b2 = factory.bookB2(fiction);
        b3 = factory.bookB3OutOfStock(science);
    }

    /** CAT-01 – list categories returns both seeded categories */
    @Test
    void cat01_listCategories_returnsBothCategories() {
        List<CategoryDto> cats = catalogueService.listCategories();
        assertThat(cats).extracting(CategoryDto::getName)
                .contains("Fiction", "Science");
    }

    /** CAT-02 – list publishers returns P1 and P2 */
    @Test
    void cat02_listPublishers_returnsBothPublishers() {
        // B3 has stock 0 — publishers list is not filtered by stock
        List<String> pubs = catalogueService.listPublishers();
        assertThat(pubs).contains("P1", "P2");
    }

    /** CAT-03 – GET /books returns page object with required fields */
    @Test
    void cat03_listBooks_returnsPageWithRequiredFields() {
        BookPageDto page = catalogueService.listBooks(null, null, null, 0, 20);
        // B3 has stock 0 so excluded from catalogue
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getPage()).isZero();
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotalElements()).isGreaterThan(0);
        BookDto first = page.getContent().get(0);
        assertThat(first.getTitle()).isNotBlank();
        assertThat(first.getAuthor()).isNotBlank();
        assertThat(first.getPrice()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(first.getAvailableStock()).isGreaterThanOrEqualTo(0);
    }

    /** CAT-04 – search is case-insensitive and matches B1 by partial title */
    @Test
    void cat04_searchByTitle_caseInsensitive_findsB1() {
        // B1 title contains "clean code" — search lowercase
        BookPageDto page = catalogueService.listBooks("clean", null, null, 0, 20);
        assertThat(page.getContent())
                .extracting(BookDto::getId)
                .contains(b1.getId())
                .doesNotContain(b2.getId());
    }

    /** CAT-05 – filter by categoryId AND publisher returns B1 only */
    @Test
    void cat05_filterByCategoryAndPublisher_returnsOnlyB1() {
        BookPageDto page = catalogueService.listBooks(null, fiction.getId(), "P1", 0, 20);
        assertThat(page.getContent())
                .extracting(BookDto::getId)
                .contains(b1.getId())
                .doesNotContain(b2.getId());
    }

    /** CAT-06 – page size 1 returns at most one item and pagination metadata is correct */
    @Test
    void cat06_pagination_sizeOne_returnsAtMostOneItem() {
        BookPageDto page = catalogueService.listBooks(null, null, null, 0, 1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getSize()).isEqualTo(1);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    /** CAT-07 – size=0 is invalid — Spring @Min(1) validation throws */
    @Test
    void cat07_invalidPageSize_throwsConstraintViolation() {
        // size minimum is validated at controller layer via @RequestParam default
        // At service layer we pass raw values — negative/zero page size causes
        // an IllegalArgumentException from Spring Data PageRequest
        assertThatThrownBy(() ->
                catalogueService.listBooks(null, null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** CAT-08 – get book by id returns correct id and price */
    @Test
    void cat08_getBook_returnsCorrectBookDetails() {
        BookDto dto = catalogueService.getBook(b1.getId());
        assertThat(dto.getId()).isEqualTo(b1.getId());
        assertThat(dto.getPrice()).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    /** CAT-09 – unknown book id throws ResourceNotFoundException */
    @Test
    void cat09_unknownBookId_throwsNotFound() {
        assertThatThrownBy(() -> catalogueService.getBook(999_999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999999");
    }

    /** CAT-10 – related books excludes B1 itself; B2 is eligible (same category) */
    @Test
    void cat10_relatedBooks_excludesSelf_includesSameCategoryBook() {
        List<BookDto> related = catalogueService.listRelatedBooks(b1.getId());
        assertThat(related)
                .extracting(BookDto::getId)
                .doesNotContain(b1.getId())
                .contains(b2.getId());
    }
}
