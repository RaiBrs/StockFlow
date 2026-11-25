package com.rai.stockflow.service;

import com.rai.stockflow.dto.CreateWarehouseRequest;
import com.rai.stockflow.dto.WarehouseResponse;
import com.rai.stockflow.entity.Warehouse;
import com.rai.stockflow.exception.DuplicateWarehouseCodeException;
import com.rai.stockflow.exception.WarehouseNotFoundException;
import com.rai.stockflow.repository.WarehouseRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Aplica as regras de cadastro e consulta de depósitos. */
@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
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
}
