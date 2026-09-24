package com.example.ebookstore.repository;

import com.example.ebookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Search and filter books.
     * All params are optional — null values are ignored in the WHERE clause.
     */
    @Query("""
            SELECT b FROM Book b
            WHERE (:q       IS NULL OR LOWER(b.title)     LIKE LOWER(CONCAT('%', :q, '%'))
                                    OR LOWER(b.author)    LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:catId   IS NULL OR b.category.id      = :catId)
              AND (:pub     IS NULL OR LOWER(b.publisher) = LOWER(:pub))
              AND b.availableStock > 0
            """)
    Page<Book> search(@Param("q") String q,
                      @Param("catId") Long categoryId,
                      @Param("pub") String publisher,
                      Pageable pageable);

    /** Related books: same category, excluding the current book, in-stock only. */
    @Query("""
            SELECT b FROM Book b
            WHERE b.category.id = :catId
              AND b.id <> :bookId
              AND b.availableStock > 0
            """)
    List<Book> findRelated(@Param("catId") Long categoryId,
                           @Param("bookId") Long bookId,
                           Pageable pageable);

    /** Distinct publisher names for the catalogue brand filter. */
    @Query("SELECT DISTINCT b.publisher FROM Book b ORDER BY b.publisher")
    List<String> findDistinctPublishers();

    /** Books belonging to any of the given category ids — used for recommendations. */
    @Query("""
            SELECT b FROM Book b
            WHERE b.category.id IN :categoryIds
              AND b.id NOT IN :excludedBookIds
              AND b.availableStock > 0
            """)
    List<Book> findRecommendations(@Param("categoryIds") List<Long> categoryIds,
                                   @Param("excludedBookIds") List<Long> excludedBookIds,
                                   Pageable pageable);
}
