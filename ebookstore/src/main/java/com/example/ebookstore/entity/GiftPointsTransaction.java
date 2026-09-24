package com.example.ebookstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gift_points_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GiftPointsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * Positive = points earned, negative = points redeemed.
     * One gift point = 1 INR.
     * Earning rule: 1 point per 100 INR of finalTotal (rounded down).
     */
    @Column(nullable = false)
    private Integer pointsDelta;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
