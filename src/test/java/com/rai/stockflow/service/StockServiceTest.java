package com.rai.stockflow.service;

import com.rai.stockflow.dto.StockAdjustmentDirection;
import com.rai.stockflow.dto.StockAdjustmentRequest;
import com.rai.stockflow.dto.StockChangeRequest;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.entity.StockBalance;
import com.rai.stockflow.entity.StockMovement;
import com.rai.stockflow.entity.StockMovementType;
import com.rai.stockflow.entity.User;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.StockOperationConflictException;
import com.rai.stockflow.exception.InvalidPageException;
import com.rai.stockflow.repository.ProductRepository;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.StockMovementRepository;
import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    private ProductRepository products;
    private WarehouseRepository warehouses;
    private UserRepository users;
    private StockBalanceRepository balances;
    private StockMovementRepository movements;
    private StockService service;
    private Product product;
    private Warehouse warehouse;
    private User actor;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        warehouses = mock(WarehouseRepository.class);
        users = mock(UserRepository.class);
        balances = mock(StockBalanceRepository.class);
        movements = mock(StockMovementRepository.class);
        service = new StockService(products, warehouses, users, balances, movements);
        product = mock(Product.class);
        warehouse = mock(Warehouse.class);
        actor = mock(User.class);
        when(product.getId()).thenReturn(1L);
        when(product.isActive()).thenReturn(true);
        when(warehouse.getId()).thenReturn(2L);
        when(warehouse.isActive()).thenReturn(true);
        when(actor.isActive()).thenReturn(true);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(actor));
        when(products.findForUpdate(1L)).thenReturn(Optional.of(product));
        when(warehouses.findForUpdate(2L)).thenReturn(Optional.of(warehouse));
        when(movements.save(any(StockMovement.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void initialEntryCreatesBalanceAndMovement() {
        when(balances.saveAndFlush(any(StockBalance.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.initialize(new StockChangeRequest(1L, 2L, 5L), "admin@example.com");

        assertEquals(StockMovementType.INITIAL_ENTRY, result.type());
        assertEquals(0, result.previousBalance());
        assertEquals(5, result.newBalance());
        verify(balances).saveAndFlush(any(StockBalance.class));
        verify(movements).save(any(StockMovement.class));
    }

    @Test
    void secondInitialEntryIsRejected() {
        when(balances.existsByProductIdAndWarehouseId(1L, 2L)).thenReturn(true);

        assertThrows(StockOperationConflictException.class,
                () -> service.initialize(new StockChangeRequest(1L, 2L, 5L), "admin@example.com"));
        verify(movements, never()).save(any());
    }

    @Test
    void inactiveProductCannotReceiveInitialOrCommonEntry() {
        when(product.isActive()).thenReturn(false);

        assertThrows(StockOperationConflictException.class,
                () -> service.initialize(new StockChangeRequest(1L, 2L, 5L), "admin@example.com"));
        assertThrows(StockOperationConflictException.class,
                () -> service.enter(new StockChangeRequest(1L, 2L, 5L), "admin@example.com"));
        verify(movements, never()).save(any());
    }

    @Test
    void inactiveProductCannotReceivePositiveAdjustment() {
        when(product.isActive()).thenReturn(false);
        when(balances.findForUpdate(1L, 2L)).thenReturn(Optional.of(new StockBalance(product, warehouse, 0)));

        assertThrows(StockOperationConflictException.class, () -> service.adjust(
                new StockAdjustmentRequest(1L, 2L, 1L, StockAdjustmentDirection.IN, "Contagem"),
                "admin@example.com"));
        verify(movements, never()).save(any());
    }

    @Test
    void inactiveWarehouseCannotReceiveCommonEntry() {
        when(warehouse.isActive()).thenReturn(false);

        assertThrows(StockOperationConflictException.class,
                () -> service.enter(new StockChangeRequest(1L, 2L, 1L), "admin@example.com"));
        verify(movements, never()).save(any());
    }

    @Test
    void inactiveWarehouseCannotReceiveInitialEntryOrPositiveAdjustment() {
        when(warehouse.isActive()).thenReturn(false);
        when(balances.findForUpdate(1L, 2L)).thenReturn(Optional.of(new StockBalance(product, warehouse, 0)));

        assertThrows(StockOperationConflictException.class,
                () -> service.initialize(new StockChangeRequest(1L, 2L, 1L), "admin@example.com"));
        assertThrows(StockOperationConflictException.class, () -> service.adjust(
                new StockAdjustmentRequest(1L, 2L, 1L, StockAdjustmentDirection.IN, "Contagem"),
                "admin@example.com"));
        verify(movements, never()).save(any());
    }

    @Test
    void entryIncreasesBalanceAndRecordsPreviousValue() {
        StockBalance balance = new StockBalance(product, warehouse, 5);
        when(balances.findForUpdate(1L, 2L)).thenReturn(Optional.of(balance));

        var result = service.enter(new StockChangeRequest(1L, 2L, 3L), "admin@example.com");

        assertEquals(8, balance.getQuantity());
        assertEquals(5, result.previousBalance());
        assertEquals(8, result.newBalance());
        assertEquals(StockMovementType.ENTRY, result.type());
    }

    @Test
    void insufficientExitDoesNotChangeBalanceOrRecordMovement() {
        StockBalance balance = new StockBalance(product, warehouse, 5);
        when(balances.findForUpdate(1L, 2L)).thenReturn(Optional.of(balance));

        assertThrows(StockOperationConflictException.class,
                () -> service.exit(new StockChangeRequest(1L, 2L, 6L), "admin@example.com"));

        assertEquals(5, balance.getQuantity());
        verify(movements, never()).save(any());
    }

    @Test
    void adjustmentOutRecordsReasonAndNewBalance() {
        StockBalance balance = new StockBalance(product, warehouse, 5);
        when(balances.findForUpdate(1L, 2L)).thenReturn(Optional.of(balance));

        var result = service.adjust(new StockAdjustmentRequest(
                1L, 2L, 2L, StockAdjustmentDirection.OUT, " Contagem física "), "admin@example.com");

        assertEquals(3, balance.getQuantity());
        assertEquals("Contagem física", result.reason());
        assertEquals(StockMovementType.ADJUSTMENT_OUT, result.type());
    }

    @Test
    void negativeHistoryPageIsRejected() {
        assertThrows(InvalidPageException.class, () -> service.movements(1L, 2L, -1));
    }
}
