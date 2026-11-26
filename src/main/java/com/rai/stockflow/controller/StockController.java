package com.rai.stockflow.controller;

import com.rai.stockflow.dto.StockAdjustmentRequest;
import com.rai.stockflow.dto.StockBalanceResponse;
import com.rai.stockflow.dto.StockChangeRequest;
import com.rai.stockflow.dto.StockMovementPageResponse;
import com.rai.stockflow.dto.StockMovementResponse;
import com.rai.stockflow.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recebe operações de estoque; a regra e a transação ficam no serviço. */
@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/initial-entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public StockMovementResponse initialize(@Valid @RequestBody StockChangeRequest request,
                                            Authentication authentication) {
        return stockService.initialize(request, authentication.getName());
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public StockMovementResponse enter(@Valid @RequestBody StockChangeRequest request,
                                       Authentication authentication) {
        return stockService.enter(request, authentication.getName());
    }

    @PostMapping("/exits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public StockMovementResponse exit(@Valid @RequestBody StockChangeRequest request,
                                      Authentication authentication) {
        return stockService.exit(request, authentication.getName());
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public StockMovementResponse adjust(@Valid @RequestBody StockAdjustmentRequest request,
                                        Authentication authentication) {
        return stockService.adjust(request, authentication.getName());
    }

    @GetMapping("/balances/{productId}/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public StockBalanceResponse balance(@PathVariable Long productId, @PathVariable Long warehouseId) {
        return stockService.balance(productId, warehouseId);
    }

    @GetMapping("/movements/{productId}/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public StockMovementPageResponse movements(@PathVariable Long productId,
                                               @PathVariable Long warehouseId,
                                               @RequestParam(defaultValue = "0") int page) {
        return stockService.movements(productId, warehouseId, page);
    }
}
