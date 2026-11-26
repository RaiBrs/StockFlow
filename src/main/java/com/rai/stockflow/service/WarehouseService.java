package com.rai.stockflow.service;

import com.rai.stockflow.dto.CreateWarehouseRequest;
import com.rai.stockflow.dto.UpdateWarehouseRequest;
import com.rai.stockflow.dto.WarehousePageResponse;
import com.rai.stockflow.dto.WarehouseResponse;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.DuplicateWarehouseCodeException;
import com.rai.stockflow.exception.InvalidPageException;
import com.rai.stockflow.exception.WarehouseLifecycleConflictException;
import com.rai.stockflow.exception.WarehouseNotFoundException;
import com.rai.stockflow.repository.StockBalanceRepository;
import com.rai.stockflow.repository.WarehouseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Aplica as regras de cadastro, consulta e ciclo de vida dos depósitos. */
@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository balanceRepository;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            StockBalanceRepository balanceRepository) {
        this.warehouseRepository = warehouseRepository;
        this.balanceRepository = balanceRepository;
    }

    public Warehouse create(CreateWarehouseRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);

        if (warehouseRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateWarehouseCodeException(code);
        }

        Warehouse warehouse = new Warehouse(code, request.name().trim());
        return warehouseRepository.save(warehouse);
    }

    public WarehouseResponse findById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));

        return WarehouseResponse.from(warehouse);
    }

    @Transactional(readOnly = true)
    public WarehousePageResponse list(int page) {
        if (page < 0) {
            throw new InvalidPageException();
        }
        return WarehousePageResponse.from(warehouseRepository.findAll(
                PageRequest.of(page, 20, Sort.by("id"))).map(WarehouseResponse::from));
    }

    @Transactional
    public WarehouseResponse update(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = lockedWarehouse(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!warehouse.getCode().equals(code)
                && warehouseRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateWarehouseCodeException(code);
        }
        warehouse.updateDetails(code, request.name().trim());
        return WarehouseResponse.from(warehouse);
    }

    @Transactional
    public WarehouseResponse deactivate(Long id) {
        Warehouse warehouse = lockedWarehouse(id);
        if (balanceRepository.existsByWarehouseIdAndQuantityGreaterThan(id, 0)) {
            throw new WarehouseLifecycleConflictException(
                    "Depósito com mercadorias não pode ser desativado.");
        }
        warehouse.deactivate();
        return WarehouseResponse.from(warehouse);
    }

    @Transactional
    public WarehouseResponse reactivate(Long id) {
        Warehouse warehouse = lockedWarehouse(id);
        warehouse.reactivate();
        return WarehouseResponse.from(warehouse);
    }

    private Warehouse lockedWarehouse(Long id) {
        return warehouseRepository.findForUpdate(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));
    }
}
