package com.ameya.inventory.service;

import com.ameya.inventory.dto.uom.UnitOfMeasureDtos;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitOfMeasureService {

    private final UnitOfMeasureRepository repository;

    @Transactional(readOnly = true)
    public List<UnitOfMeasureDtos.Response> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UnitOfMeasureDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public UnitOfMeasureDtos.Response create(UnitOfMeasureDtos.Request request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("Unit of measure '" + request.code() + "' already exists.");
        }
        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setCode(request.code());
        uom.setName(request.name());
        uom.setActive(request.active());
        return toResponse(repository.save(uom));
    }

    @Transactional
    public UnitOfMeasureDtos.Response update(Long id, UnitOfMeasureDtos.Request request) {
        UnitOfMeasure uom = findOrThrow(id);
        uom.setCode(request.code());
        uom.setName(request.name());
        uom.setActive(request.active());
        return toResponse(repository.save(uom));
    }

    private UnitOfMeasure findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Unit of measure", id));
    }

    private UnitOfMeasureDtos.Response toResponse(UnitOfMeasure u) {
        return new UnitOfMeasureDtos.Response(u.getId(), u.getCode(), u.getName(), u.isActive());
    }
}
