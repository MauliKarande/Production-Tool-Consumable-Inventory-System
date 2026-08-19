package com.ameya.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "machines")
public class Machine extends BaseEntity {

    @Column(name = "machine_code", nullable = false, unique = true, length = 50)
    private String machineCode;

    @Column(name = "machine_name", nullable = false, length = 150)
    private String machineName;

    @Column(name = "machine_type", length = 50)
    private String machineType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = 150)
    private String location;

    @Column(length = 150)
    private String manufacturer;

    @Column(length = 150)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MachineStatus status = MachineStatus.ACTIVE;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false)
    private boolean active = true;
}
