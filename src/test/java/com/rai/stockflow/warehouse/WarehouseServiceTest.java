package com.rai.stockflow.warehouse;

import com.rai.stockflow.dto.CreateWarehouseRequest;
import com.rai.stockflow.dto.WarehouseResponse;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.DuplicateWarehouseCodeException;
import com.rai.stockflow.exception.WarehouseNotFoundException;
import com.rai.stockflow.repository.WarehouseRepository;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.service.WarehouseService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehouseServiceTest {

    private WarehouseRepository repository;
    private WarehouseService service;

    @BeforeEach
    void setUp() {
        repository = mock(WarehouseRepository.class);
        service = new WarehouseService(repository, mock(StockBalanceRepository.class));
    }

    @Test
    @DisplayName("RN-007: cadastra depósito com código padronizado")
    void createsWarehouseWithCanonicalCode() {
        when(repository.save(any(Warehouse.class))).thenAnswer(call -> call.getArgument(0));

        Warehouse warehouse = service.create(new CreateWarehouseRequest(" dep-01 ", " Central "));

        assertEquals("DEP-01", warehouse.getCode());
        assertEquals("Central", warehouse.getName());
        verify(repository).existsByCodeIgnoreCase("DEP-01");
    }

    @Test
    @DisplayName("RN-007: recusa código já cadastrado")
    void rejectsDuplicateCode() {
        when(repository.existsByCodeIgnoreCase("DEP-01")).thenReturn(true);

        assertThrows(DuplicateWarehouseCodeException.class,
                () -> service.create(new CreateWarehouseRequest("dep-01", "Central")));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Cadastro: exige código e nome preenchidos")
    void rejectsBlankFields() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new CreateWarehouseRequest(" ", ""));

            assertEquals(2, violations.size());
        }
    }

    @Test
    @DisplayName("Consulta: devolve os dados do depósito")
    void returnsWarehouseResponse() {
        Warehouse warehouse = mock(Warehouse.class);
        when(warehouse.getId()).thenReturn(7L);
        when(warehouse.getCode()).thenReturn("DEP-01");
        when(warehouse.getName()).thenReturn("Central");
        when(warehouse.isActive()).thenReturn(true);
        when(repository.findById(7L)).thenReturn(Optional.of(warehouse));

        WarehouseResponse response = service.findById(7L);

        assertEquals(7L, response.id());
        assertEquals("DEP-01", response.code());
        assertEquals("Central", response.name());
        assertTrue(response.active());
    }

    @Test
    @DisplayName("Consulta: depósito inexistente produz erro específico")
    void rejectsMissingWarehouse() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class, () -> service.findById(7L));
    }
}
