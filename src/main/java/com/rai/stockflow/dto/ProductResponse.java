package com.rai.stockflow.dto;

import com.rai.stockflow.entity.Product;

/** Define os dados do produto que poderão ser enviados pela API. */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String category,
        boolean active
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.isActive()
        );
    }
}
