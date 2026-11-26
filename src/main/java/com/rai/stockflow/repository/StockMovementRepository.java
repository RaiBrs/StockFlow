package com.rai.stockflow.repository;

import com.rai.stockflow.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByBalanceId(Long balanceId, Pageable pageable);

    @Query("select (count(m) > 0) from StockMovement m where m.balance.product.id = :productId")
    boolean existsForProduct(@Param("productId") Long productId);
}
