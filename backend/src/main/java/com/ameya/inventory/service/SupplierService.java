package com.ameya.inventory.service;

import com.ameya.inventory.dto.supplier.SupplierDtos;
import com.ameya.inventory.entity.Supplier;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;

    @Transactional(readOnly = true)
    public Page<SupplierDtos.Response> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public SupplierDtos.Response create(SupplierDtos.Request request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Supplier '" + request.name() + "' already exists.");
        }
        Supplier supplier = apply(new Supplier(), request);
        return toResponse(repository.save(supplier));
    }

    @Transactional
    public SupplierDtos.Response update(Long id, SupplierDtos.Request request) {
        Supplier supplier = apply(findOrThrow(id), request);
        return toResponse(repository.save(supplier));
    }

    private Supplier apply(Supplier supplier, SupplierDtos.Request request) {
        supplier.setName(request.name());
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setGstNumber(request.gstNumber());
        supplier.setActive(request.active());
        return supplier;
    }

    private Supplier findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    private SupplierDtos.Response toResponse(Supplier s) {
        return new SupplierDtos.Response(s.getId(), s.getName(), s.getContactPerson(), s.getPhone(),
                s.getEmail(), s.getAddress(), s.getGstNumber(), s.isActive());
    }
}
