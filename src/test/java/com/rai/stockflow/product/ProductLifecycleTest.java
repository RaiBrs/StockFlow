package com.rai.stockflow.product;

import com.rai.stockflow.entity.Product;
import com.rai.stockflow.exception.ProductLifecycleConflictException;
import com.rai.stockflow.exception.ProductNotFoundException;
import com.rai.stockflow.repository.ProductRepository;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.StockMovementRepository;
import com.rai.stockflow.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductLifecycleTest {

    private ProductRepository products;
    private StockBalanceRepository balances;
    private StockMovementRepository movements;
    private ProductService service;
    private Product product;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        balances = mock(StockBalanceRepository.class);
        movements = mock(StockMovementRepository.class);
        service = new ProductService(products, balances, movements);
        product = new Product("SKU-1", "Monitor", "Displays");
        when(products.findForUpdate(1L)).thenReturn(Optional.of(product));
    }

    @Test
    void deactivationRequiresZeroBalanceInEveryWarehouse() {
        when(balances.existsByProductIdAndQuantityGreaterThan(1L, 0)).thenReturn(true);

        assertThrows(ProductLifecycleConflictException.class, () -> service.deactivate(1L));
        assertTrue(product.isActive());
    }

    @Test
    void deactivationPreservesZeroBalanceAndHistory() {
        assertFalse(service.deactivate(1L).active());
        verify(balances).existsByProductIdAndQuantityGreaterThan(1L, 0);
        verify(movements, never()).deleteAll();
    }

    @Test
    void reactivationAllowsProductToReceiveEntriesAgain() {
        product.deactivate();

        assertTrue(service.reactivate(1L).active());
    }

    @Test
    void deletionRequiresNoWarehouseAssociation() {
        when(balances.existsByProductId(1L)).thenReturn(true);

        assertThrows(ProductLifecycleConflictException.class, () -> service.delete(1L));
        verify(products, never()).delete(product);
    }

    @Test
    void deletionRequiresNoMovementHistory() {
        when(movements.existsForProduct(1L)).thenReturn(true);

        assertThrows(ProductLifecycleConflictException.class, () -> service.delete(1L));
        verify(products, never()).delete(product);
    }

    @Test
    void unusedProductCanBeDeleted() {
        service.delete(1L);

        verify(products).delete(product);
        verify(products).flush();
    }

    @Test
    void missingProductCannotBeChanged() {
        when(products.findForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.deactivate(99L));
    }
}
