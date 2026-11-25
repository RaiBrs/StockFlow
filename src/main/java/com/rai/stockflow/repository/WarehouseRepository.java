package com.rai.stockflow.repository;

import com.rai.stockflow.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consulta códigos sem diferenciar maiúsculas de minúsculas. */
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByCodeIgnoreCase(String code);
}
