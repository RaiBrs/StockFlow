package com.rai.stockflow.repository;

import com.rai.stockflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consulta SKUs sem diferenciar maiúsculas de minúsculas. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);
}
