package com.rai.stockflow.stock;

import com.rai.stockflow.dto.StockChangeRequest;
import com.rai.stockflow.dto.StockAdjustmentRequest;
import com.rai.stockflow.dto.StockAdjustmentDirection;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.entity.StockBalance;
import com.rai.stockflow.entity.User;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.StockOperationConflictException;
import com.rai.stockflow.repository.ProductRepository;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.StockMovementRepository;
import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.repository.WarehouseRepository;
import com.rai.stockflow.security.Role;
import com.rai.stockflow.service.StockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Usa transações separadas no PostgreSQL para verificar a trava do saldo. */
@SpringBootTest(properties = "JWT_SECRET_BASE64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@EnabledIfEnvironmentVariable(named = "STOCKFLOW_RUN_DB_TESTS", matches = "true")
class StockConcurrencyPostgresTest {

    @Autowired private StockService service;
    @Autowired private ProductRepository products;
    @Autowired private WarehouseRepository warehouses;
    @Autowired private UserRepository users;
    @Autowired private StockBalanceRepository balances;
    @Autowired private StockMovementRepository movements;

    @Test
    void twoSimultaneousExitsCannotOverdrawBalance() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Product product = products.save(new Product("TEST-" + suffix, "Produto de teste", "Teste"));
        Warehouse warehouse = warehouses.save(new Warehouse("TEST-" + suffix, "Depósito de teste"));
        User actor = users.save(new User("test-" + suffix + "@example.com", "unused-hash", Role.ADMIN));

        try {
            service.initialize(new StockChangeRequest(product.getId(), warehouse.getId(), 5L), actor.getEmail());
            CountDownLatch start = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                java.util.concurrent.Callable<Boolean> withdraw = () -> {
                    start.await();
                    try {
                        service.exit(new StockChangeRequest(product.getId(), warehouse.getId(), 4L), actor.getEmail());
                        return true;
                    } catch (StockOperationConflictException exception) {
                        return false;
                    }
                };
                Future<Boolean> first = executor.submit(withdraw);
                Future<Boolean> second = executor.submit(withdraw);
                start.countDown();

                int successes = (first.get(30, TimeUnit.SECONDS) ? 1 : 0)
                        + (second.get(30, TimeUnit.SECONDS) ? 1 : 0);
                assertEquals(1, successes);
            }

            assertEquals(1, service.balance(product.getId(), warehouse.getId()).quantity());
            StockBalance balance = balances.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseThrow();
            assertEquals(2, movements.findByBalanceId(balance.getId(), PageRequest.of(0, 20)).getTotalElements());
        } finally {
            cleanUp(product, warehouse, actor);
        }
    }

    @Test
    void failedMovementInsertRollsBackBalanceChange() {
        String suffix = UUID.randomUUID().toString();
        Product product = products.save(new Product("TEST-" + suffix, "Produto de teste", "Teste"));
        Warehouse warehouse = warehouses.save(new Warehouse("TEST-" + suffix, "Depósito de teste"));
        User actor = users.save(new User("test-" + suffix + "@example.com", "unused-hash", Role.ADMIN));

        try {
            service.initialize(new StockChangeRequest(product.getId(), warehouse.getId(), 5L), actor.getEmail());

            // Chamada direta contorna a validação HTTP; a restrição SQL rejeita justificativa vazia.
            assertThrows(DataIntegrityViolationException.class, () -> service.adjust(
                    new StockAdjustmentRequest(product.getId(), warehouse.getId(), 3L,
                            StockAdjustmentDirection.IN, " "), actor.getEmail()));

            assertEquals(5, service.balance(product.getId(), warehouse.getId()).quantity());
            StockBalance balance = balances.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseThrow();
            assertEquals(1, movements.findByBalanceId(balance.getId(), PageRequest.of(0, 20)).getTotalElements());
        } finally {
            cleanUp(product, warehouse, actor);
        }
    }

    private void cleanUp(Product product, Warehouse warehouse, User actor) {
        balances.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()).ifPresent(balance -> {
            movements.deleteAll(movements.findByBalanceId(balance.getId(), PageRequest.of(0, 20)).getContent());
            balances.delete(balance);
        });
        products.delete(product);
        warehouses.delete(warehouse);
        users.delete(actor);
    }
}
