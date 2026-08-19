package com.ameya.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "items")
public class Item extends BaseEntity {

    @Column(name = "item_code", nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_supplier_id")
    private Supplier preferredSupplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UnitOfMeasure uom;

    @Column(name = "part_number", length = 100)
    private String partNumber;

    @Column(length = 500)
    private String specification;

    @Column(name = "safe_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal safeStock = BigDecimal.ZERO;

    @Column(name = "max_stock", precision = 14, scale = 3)
    private BigDecimal maxStock;

    @Column(name = "current_unit_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentUnitCost = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "legacy_description", length = 500)
    private String legacyDescription;

    @Column(name = "barcode_value", unique = true, length = 100)
    private String barcodeValue;

    @Column(nullable = false)
    private boolean active = true;
}
