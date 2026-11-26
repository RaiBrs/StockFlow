package com.rai.stockflow.repository;

import com.rai.stockflow.entity.Warehouse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Consulta códigos sem diferenciar maiúsculas de minúsculas. */
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    // A mesma trava é usada ao desativar o depósito e ao movimentar seu estoque.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Warehouse w where w.id = :id")
    Optional<Warehouse> findForUpdate(@Param("id") Long id);
}
