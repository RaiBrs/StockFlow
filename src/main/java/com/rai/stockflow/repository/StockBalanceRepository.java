package com.rai.stockflow.repository;

import com.rai.stockflow.entity.StockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    boolean existsByProductIdAndWarehouseId(Long productId, Long warehouseId);

    Optional<StockBalance> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    // Bloqueia a linha até o fim da transação para serializar alterações do mesmo saldo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalance b where b.product.id = :productId and b.warehouse.id = :warehouseId")
    Optional<StockBalance> findForUpdate(@Param("productId") Long productId,
                                         @Param("warehouseId") Long warehouseId);
}
