package com.rai.stockflow.service;

import com.rai.stockflow.dto.CreateProductRequest;
import com.rai.stockflow.dto.ProductResponse;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.exception.DuplicateSkuException;
import com.rai.stockflow.exception.ProductLifecycleConflictException;
import com.rai.stockflow.exception.ProductNotFoundException;
import com.rai.stockflow.repository.ProductRepository;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Aplica as regras de cadastro, consulta e ciclo de vida dos produtos. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;

    public ProductService(ProductRepository productRepository, StockBalanceRepository balanceRepository,
                          StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
    }

    public Product create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Product product = new Product(sku, request.name().trim(), request.category().trim());
        return productRepository.save(product);
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse deactivate(Long id) {
        Product product = lockedProduct(id);
        if (balanceRepository.existsByProductIdAndQuantityGreaterThan(id, 0)) {
            throw new ProductLifecycleConflictException(
                    "Produto com saldo em depósito não pode ser desativado.");
        }
        // O saldo já é zero; desativar não cria uma movimentação artificial.
        product.deactivate();
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse reactivate(Long id) {
        Product product = lockedProduct(id);
        product.reactivate();
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = lockedProduct(id);
        if (balanceRepository.existsByProductId(id) || movementRepository.existsForProduct(id)) {
            throw new ProductLifecycleConflictException(
                    "Produto com vínculo a depósito ou histórico não pode ser excluído definitivamente.");
        }
        productRepository.delete(product);
        productRepository.flush();
    }

    private Product lockedProduct(Long id) {
        return productRepository.findForUpdate(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
