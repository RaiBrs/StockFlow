package com.rai.stockflow.service;

import com.rai.stockflow.dto.StockAdjustmentDirection;
import com.rai.stockflow.dto.StockAdjustmentRequest;
import com.rai.stockflow.dto.StockBalanceResponse;
import com.rai.stockflow.dto.StockChangeRequest;
import com.rai.stockflow.dto.StockMovementPageResponse;
import com.rai.stockflow.dto.StockMovementResponse;
import com.rai.stockflow.entity.Product;
import com.rai.stockflow.entity.StockBalance;
import com.rai.stockflow.entity.StockMovement;
import com.rai.stockflow.entity.StockMovementType;
import com.rai.stockflow.entity.User;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.InvalidPageException;
import com.rai.stockflow.exception.ProductNotFoundException;
import com.rai.stockflow.exception.StockBalanceNotFoundException;
import com.rai.stockflow.exception.StockOperationConflictException;
import com.rai.stockflow.exception.WarehouseNotFoundException;
import com.rai.stockflow.repository.ProductRepository;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.StockMovementRepository;
import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Mantém saldo e movimento na mesma transação. */
@Service
public class StockService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;

    public StockService(ProductRepository productRepository, WarehouseRepository warehouseRepository,
                        UserRepository userRepository, StockBalanceRepository balanceRepository,
                        StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public StockMovementResponse initialize(StockChangeRequest request, String actorEmail) {
        Product product = product(request.productId());
        Warehouse warehouse = warehouse(request.warehouseId());
        requireActiveWarehouse(warehouse);
        User actor = actor(actorEmail);

        if (balanceRepository.existsByProductIdAndWarehouseId(product.getId(), warehouse.getId())) {
            throw new StockOperationConflictException("O saldo inicial deste produto no depósito já foi registrado.");
        }

        // O UNIQUE no banco resolve a corrida entre duas entradas iniciais simultâneas.
        StockBalance balance = balanceRepository.saveAndFlush(
                new StockBalance(product, warehouse, request.quantity()));
        return record(balance, actor, StockMovementType.INITIAL_ENTRY,
                request.quantity(), 0, request.quantity(), null);
    }

    @Transactional
    public StockMovementResponse enter(StockChangeRequest request, String actorEmail) {
        StockBalance balance = lockedBalance(request.productId(), request.warehouseId());
        requireActiveWarehouse(balance.getWarehouse());
        long previous = balance.getQuantity();
        long updated = add(previous, request.quantity());
        balance.changeQuantity(updated);
        return record(balance, actor(actorEmail), StockMovementType.ENTRY,
                request.quantity(), previous, updated, null);
    }

    @Transactional
    public StockMovementResponse exit(StockChangeRequest request, String actorEmail) {
        StockBalance balance = lockedBalance(request.productId(), request.warehouseId());
        long previous = balance.getQuantity();
        long updated = subtract(previous, request.quantity());
        balance.changeQuantity(updated);
        return record(balance, actor(actorEmail), StockMovementType.EXIT,
                request.quantity(), previous, updated, null);
    }

    @Transactional
    public StockMovementResponse adjust(StockAdjustmentRequest request, String actorEmail) {
        StockBalance balance = lockedBalance(request.productId(), request.warehouseId());
        long previous = balance.getQuantity();
        boolean increase = request.direction() == StockAdjustmentDirection.IN;
        if (increase) {
            requireActiveWarehouse(balance.getWarehouse());
        }
        long updated = increase ? add(previous, request.quantity()) : subtract(previous, request.quantity());
        balance.changeQuantity(updated);
        return record(balance, actor(actorEmail),
                increase ? StockMovementType.ADJUSTMENT_IN : StockMovementType.ADJUSTMENT_OUT,
                request.quantity(), previous, updated, request.reason().trim());
    }

    @Transactional(readOnly = true)
    public StockBalanceResponse balance(Long productId, Long warehouseId) {
        return StockBalanceResponse.from(balanceRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new StockBalanceNotFoundException(productId, warehouseId)));
    }

    @Transactional(readOnly = true)
    public StockMovementPageResponse movements(Long productId, Long warehouseId, int page) {
        if (page < 0) {
            throw new InvalidPageException();
        }
        StockBalance balance = balanceRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new StockBalanceNotFoundException(productId, warehouseId));
        Page<StockMovementResponse> result = movementRepository.findByBalanceId(balance.getId(),
                        PageRequest.of(page, 20, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))
                .map(StockMovementResponse::from);
        return StockMovementPageResponse.from(result);
    }

    private StockBalance lockedBalance(Long productId, Long warehouseId) {
        return balanceRepository.findForUpdate(productId, warehouseId)
                .orElseThrow(() -> new StockBalanceNotFoundException(productId, warehouseId));
    }

    private Product product(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Warehouse warehouse(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    private User actor(String email) {
        return userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new StockOperationConflictException("Usuário responsável indisponível."));
    }

    private void requireActiveWarehouse(Warehouse warehouse) {
        if (!warehouse.isActive()) {
            throw new StockOperationConflictException("Depósito inativo não pode receber mercadorias.");
        }
    }

    private long add(long current, long quantity) {
        try {
            return Math.addExact(current, quantity);
        } catch (ArithmeticException exception) {
            throw new StockOperationConflictException("Quantidade excede o limite de saldo permitido.");
        }
    }

    private long subtract(long current, long quantity) {
        if (quantity > current) {
            throw new StockOperationConflictException("Estoque insuficiente para esta operação.");
        }
        return current - quantity;
    }

    private StockMovementResponse record(StockBalance balance, User actor, StockMovementType type,
                                         long quantity, long previous, long updated, String reason) {
        StockMovement movement = movementRepository.save(new StockMovement(
                balance, actor, type, quantity, previous, updated, reason, Instant.now()));
        return StockMovementResponse.from(movement);
    }
}
