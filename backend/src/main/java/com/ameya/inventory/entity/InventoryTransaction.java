package com.ameya.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single immutable ledger row. Stock is never stored redundantly on
 * Item - it is always the signed sum of these rows for that item.
 * Never update or delete a posted row; corrections are a new REVERSAL
 * transaction (see InventoryTransactionService.reverse).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_no", nullable = false, unique = true, length = 50)
    private String txnNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 30)
    private TransactionType txnType;

    /** Signed: positive adds to stock, negative removes from it. */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost_at_txn", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCostAtTxn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedBy;

    @Column(length = 255)
    private String purpose;

    @Column(length = 500)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", length = 20)
    private ItemCondition itemCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_txn_id")
    private InventoryTransaction reversalOfTxn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionSource source = TransactionSource.APP;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
