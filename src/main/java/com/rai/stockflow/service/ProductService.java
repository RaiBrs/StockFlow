package com.rai.stockflow.service;

import com.rai.stockflow.dto.CreateProductRequest;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.exception.DuplicateSkuException;
import com.rai.stockflow.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Aplica as regras de cadastro antes de persistir um produto. */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Product product = new Product(sku, request.name().trim(), request.category().trim());
        return productRepository.save(product);
    }
}
