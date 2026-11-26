package com.rai.stockflow.product;

import com.rai.stockflow.dto.ProductResponse;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.exception.ProductNotFoundException;
import com.rai.stockflow.repository.ProductRepository;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.StockMovementRepository;
import com.rai.stockflow.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductQueryTest {

    private ProductRepository productRepository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        service = new ProductService(productRepository, mock(StockBalanceRepository.class),
                mock(StockMovementRepository.class));
    }

    @Test
    @DisplayName("Consulta: devolve os dados do produto sem expor a entidade")
    void returnsProductResponse() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(42L);
        when(product.getSku()).thenReturn("AB-12");
        when(product.getName()).thenReturn("Monitor");
        when(product.getCategory()).thenReturn("Displays");
        when(product.isActive()).thenReturn(true);
        when(productRepository.findById(42L)).thenReturn(Optional.of(product));

        ProductResponse response = service.findById(42L);

        assertEquals(42L, response.id());
        assertEquals("AB-12", response.sku());
        assertEquals("Monitor", response.name());
        assertEquals("Displays", response.category());
        assertTrue(response.active());
    }

    @Test
    @DisplayName("Consulta: produto inexistente produz erro específico")
    void rejectsMissingProduct() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        ProductNotFoundException error = assertThrows(ProductNotFoundException.class,
                () -> service.findById(42L));

        assertEquals("Produto não encontrado: 42", error.getMessage());
        verify(productRepository).findById(42L);
    }
}
