package com.rai.stockflow.product;

import com.rai.stockflow.dto.CreateProductRequest;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.exception.DuplicateSkuException;
import com.rai.stockflow.repository.ProductRepository;
import com.rai.stockflow.service.ProductService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductRegistrationTest {

    private ProductRepository productRepository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        service = new ProductService(productRepository);
    }

    @Test
    @DisplayName("RN-001: cadastra produto com SKU padronizado")
    void createsProductWithCanonicalSku() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(call -> call.getArgument(0));

        Product product = service.create(
                new CreateProductRequest(" ab-12 ", " Monitor ", " Displays ")
        );

        assertEquals("AB-12", product.getSku());
        assertEquals("Monitor", product.getName());
        assertEquals("Displays", product.getCategory());
        verify(productRepository).existsBySkuIgnoreCase("AB-12");
    }

    @Test
    @DisplayName("RN-001: recusa SKU já cadastrado")
    void rejectsDuplicateSkuBeforeSaving() {
        when(productRepository.existsBySkuIgnoreCase("AB-12")).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> service.create(
                        new CreateProductRequest("ab-12", "Monitor", "Displays")
                ));

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-002: recusa nome em branco")
    void rejectsBlankName() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            var violations = validator.validate(
                    new CreateProductRequest("AB-12", "  ", "Displays")
            );

            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        }
    }

    @Test
    @DisplayName("RN-002: recusa categoria em branco")
    void rejectsBlankCategory() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            var violations = validator.validate(
                    new CreateProductRequest("AB-12", "Monitor", "")
            );

            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("category")));
        }
    }
}
