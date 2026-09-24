package com.example.ebookstore.repository;

import com.example.ebookstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    /** Category ids of all books the user has successfully purchased. */
    @Query("""
            SELECT DISTINCT b.category.id
            FROM Order o
            JOIN o.items oi
            JOIN Book b ON b.id = oi.bookId
            WHERE o.user.id = :userId
              AND o.status  = 'PAID'
            """)
    List<Long> findPurchasedCategoryIdsByUser(@Param("userId") Long userId);

    /** Book ids the user has already purchased — excluded from recommendations. */
    @Query("""
            SELECT DISTINCT oi.bookId
            FROM Order o
            JOIN o.items oi
            WHERE o.user.id = :userId
              AND o.status  = 'PAID'
            """)
    List<Long> findPurchasedBookIdsByUser(@Param("userId") Long userId);
}
