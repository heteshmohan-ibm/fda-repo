package com.example.ebookstore.repository;

import com.example.ebookstore.entity.GiftPointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftPointsTransactionRepository extends JpaRepository<GiftPointsTransaction, Long> {

    List<GiftPointsTransaction> findByUserIdOrderByTimestampDesc(Long userId);
}
