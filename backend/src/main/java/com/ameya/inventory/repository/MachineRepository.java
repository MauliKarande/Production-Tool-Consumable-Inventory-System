package com.ameya.inventory.repository;

import com.ameya.inventory.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MachineRepository extends JpaRepository<Machine, Long>, JpaSpecificationExecutor<Machine> {
    boolean existsByMachineCodeIgnoreCase(String machineCode);
}
