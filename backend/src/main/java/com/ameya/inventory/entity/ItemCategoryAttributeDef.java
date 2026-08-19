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

/**
 * Admin-configurable "shape" of the extra fields a tool category needs
 * (e.g. Diameter for DRILLS, Grade for INSERTS) without a Java code change.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "item_category_attribute_defs")
public class ItemCategoryAttributeDef extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private AttributeDataType dataType;

    @Column(name = "is_required", nullable = false)
    private boolean required = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}
