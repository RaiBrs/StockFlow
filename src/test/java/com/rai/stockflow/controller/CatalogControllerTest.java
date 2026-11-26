package com.rai.stockflow.controller;

import com.rai.stockflow.dto.ProductResponse;
import com.rai.stockflow.dto.WarehouseResponse;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.ApiExceptionHandler;
import com.rai.stockflow.exception.DuplicateWarehouseCodeException;
import com.rai.stockflow.exception.ProductNotFoundException;
import com.rai.stockflow.exception.WarehouseNotFoundException;
import com.rai.stockflow.security.MethodSecurityConfig;
import com.rai.stockflow.security.ActiveUserJwtConverter;
import com.rai.stockflow.service.ProductService;
import com.rai.stockflow.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ProductController.class, WarehouseController.class})
@Import({MethodSecurityConfig.class, ApiExceptionHandler.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private WarehouseService warehouseService;

    @MockitoBean
    private ActiveUserJwtConverter activeUserJwtConverter;

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCreatesProduct() throws Exception {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getSku()).thenReturn("AB-12");
        when(product.isActive()).thenReturn(true);
        when(productService.create(any())).thenReturn(product);

        mvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"AB-12","name":"Monitor","category":"Displays"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("AB-12"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotCreateProduct() throws Exception {
        mvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"AB-12","name":"Monitor","category":"Displays"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorQueriesProduct() throws Exception {
        when(productService.findById(1L))
                .thenReturn(new ProductResponse(1L, "AB-12", "Monitor", "Displays", true));

        mvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Monitor"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void missingProductReturnsNotFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produto não encontrado: 99"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void invalidProductReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":" ","name":"Monitor","category":"Displays"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0]").value("sku: SKU é obrigatório"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesWarehouse() throws Exception {
        Warehouse warehouse = mock(Warehouse.class);
        when(warehouse.getId()).thenReturn(2L);
        when(warehouse.getCode()).thenReturn("DEP-01");
        when(warehouse.isActive()).thenReturn(true);
        when(warehouseService.create(any())).thenReturn(warehouse);

        mvc.perform(post("/api/v1/warehouses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEP-01","name":"Central"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DEP-01"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void blankWarehouseCodeReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/warehouses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":" ","name":"Central"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0]").value("code: Código é obrigatório"));

        verifyNoInteractions(warehouseService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void blankWarehouseNameReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/warehouses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEP-01","name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("name: Nome é obrigatório"));

        verifyNoInteractions(warehouseService);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotCreateWarehouse() throws Exception {
        mvc.perform(post("/api/v1/warehouses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEP-01","name":"Central"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(warehouseService);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorQueriesWarehouse() throws Exception {
        when(warehouseService.findById(2L))
                .thenReturn(new WarehouseResponse(2L, "DEP-01", "Central", true));

        mvc.perform(get("/api/v1/warehouses/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Central"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void duplicateWarehouseReturnsConflict() throws Exception {
        when(warehouseService.create(any())).thenThrow(new DuplicateWarehouseCodeException("DEP-01"));

        mvc.perform(post("/api/v1/warehouses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEP-01","name":"Central"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void anonymousUserCannotQueryProduct() throws Exception {
        mvc.perform(get("/api/v1/products/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void missingWarehouseReturnsNotFound() throws Exception {
        when(warehouseService.findById(99L)).thenThrow(new WarehouseNotFoundException(99L));

        mvc.perform(get("/api/v1/warehouses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Depósito não encontrado: 99"));
    }
}
