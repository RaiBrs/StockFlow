package com.rai.stockflow.controller;

import com.rai.stockflow.dto.CreateProductRequest;
import com.rai.stockflow.dto.ProductResponse;
import com.rai.stockflow.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Traduz requisições HTTP de produtos para operações do serviço. */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return ProductResponse.from(productService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductResponse deactivate(@PathVariable Long id) {
        return productService.deactivate(id);
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductResponse reactivate(@PathVariable Long id) {
        return productService.reactivate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
