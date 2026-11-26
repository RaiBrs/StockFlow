package com.rai.stockflow.warehouse;

import com.rai.stockflow.dto.UpdateWarehouseRequest;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.DuplicateWarehouseCodeException;
import com.rai.stockflow.exception.InvalidPageException;
import com.rai.stockflow.exception.WarehouseLifecycleConflictException;
import com.rai.stockflow.exception.WarehouseNotFoundException;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.WarehouseRepository;
import com.rai.stockflow.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarehouseLifecycleTest {

    private WarehouseRepository warehouses;
    private StockBalanceRepository balances;
    private WarehouseService service;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouses = mock(WarehouseRepository.class);
        balances = mock(StockBalanceRepository.class);
        service = new WarehouseService(warehouses, balances);
        warehouse = new Warehouse("DEP-01", "Central");
        when(warehouses.findForUpdate(7L)).thenReturn(Optional.of(warehouse));
    }

    @Test
    void updateNormalizesCodeAndName() {
        var response = service.update(7L, new UpdateWarehouseRequest(" dep-02 ", " Anexo "));

        assertEquals("DEP-02", response.code());
        assertEquals("Anexo", response.name());
    }

    @Test
    void updateRejectsCodeAlreadyUsedByAnotherWarehouse() {
        when(warehouses.existsByCodeIgnoreCaseAndIdNot("DEP-02", 7L)).thenReturn(true);

        assertThrows(DuplicateWarehouseCodeException.class,
                () -> service.update(7L, new UpdateWarehouseRequest("dep-02", "Anexo")));
        assertEquals("DEP-01", warehouse.getCode());
    }

    @Test
    void warehouseWithGoodsCannotBeDeactivated() {
        when(balances.existsByWarehouseIdAndQuantityGreaterThan(7L, 0)).thenReturn(true);

        assertThrows(WarehouseLifecycleConflictException.class, () -> service.deactivate(7L));
        assertTrue(warehouse.isActive());
    }

    @Test
    void emptyWarehouseCanBeDeactivatedAndReactivated() {
        assertFalse(service.deactivate(7L).active());
        assertTrue(service.reactivate(7L).active());
    }

    @Test
    void listsWarehousesInPages() {
        when(warehouses.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(warehouse)));

        var result = service.list(0);

        assertEquals(1, result.totalElements());
        assertEquals("DEP-01", result.items().getFirst().code());
    }

    @Test
    void negativePageIsRejected() {
        assertThrows(InvalidPageException.class, () -> service.list(-1));
    }

    @Test
    void missingWarehouseCannotBeChanged() {
        when(warehouses.findForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class, () -> service.deactivate(99L));
    }
}
