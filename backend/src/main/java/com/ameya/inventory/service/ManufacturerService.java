package com.ameya.inventory.service;

import com.ameya.inventory.dto.manufacturer.ManufacturerDtos;
import com.ameya.inventory.entity.Manufacturer;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManufacturerService {

    private final ManufacturerRepository repository;

    @Transactional(readOnly = true)
    public List<ManufacturerDtos.Response> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ManufacturerDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ManufacturerDtos.Response create(ManufacturerDtos.Request request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Manufacturer '" + request.name() + "' already exists.");
        }
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(request.name());
        manufacturer.setActive(request.active());
        return toResponse(repository.save(manufacturer));
    }

    @Transactional
    public ManufacturerDtos.Response update(Long id, ManufacturerDtos.Request request) {
        Manufacturer manufacturer = findOrThrow(id);
        manufacturer.setName(request.name());
        manufacturer.setActive(request.active());
        return toResponse(repository.save(manufacturer));
    }

    private Manufacturer findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Manufacturer", id));
    }

    private ManufacturerDtos.Response toResponse(Manufacturer m) {
        return new ManufacturerDtos.Response(m.getId(), m.getName(), m.isActive());
    }
}
