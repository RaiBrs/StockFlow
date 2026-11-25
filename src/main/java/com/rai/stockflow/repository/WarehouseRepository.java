package com.rai.stockflow.repository;

import com.rai.stockflow.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

/** A unicidade do código é garantida pela restrição no banco de dados. */
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}
