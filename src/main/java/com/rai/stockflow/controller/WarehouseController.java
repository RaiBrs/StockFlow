package com.rai.stockflow.controller;

import com.rai.stockflow.dto.CreateWarehouseRequest;
import com.rai.stockflow.dto.UpdateWarehouseRequest;
import com.rai.stockflow.dto.WarehousePageResponse;
import com.rai.stockflow.dto.WarehouseResponse;
import com.rai.stockflow.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Traduz requisições HTTP de depósitos para operações do serviço. */
@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public WarehouseResponse create(@Valid @RequestBody CreateWarehouseRequest request) {
        return WarehouseResponse.from(warehouseService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public WarehouseResponse findById(@PathVariable Long id) {
        return warehouseService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public WarehousePageResponse list(@RequestParam(defaultValue = "0") int page) {
        return warehouseService.list(page);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public WarehouseResponse update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateWarehouseRequest request) {
        return warehouseService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public WarehouseResponse deactivate(@PathVariable Long id) {
        return warehouseService.deactivate(id);
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public WarehouseResponse reactivate(@PathVariable Long id) {
        return warehouseService.reactivate(id);
    }
}
