package com.ameya.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "manufacturers")
public class Manufacturer extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
