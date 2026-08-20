package com.ameya.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "purchase_requisition_items")
public class PurchaseRequisitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pr_id", nullable = false)
    private PurchaseRequisition pr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "estimated_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Set once goods are received against this line - links to the PURCHASE_INWARD ledger row. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_txn_id")
    private InventoryTransaction receivedTxn;

    /** Quantity actually received so far (line can be partially received before the PR is fully RECEIVED). */
    @Column(name = "received_qty", precision = 14, scale = 3)
    private BigDecimal receivedQty = BigDecimal.ZERO;
}
