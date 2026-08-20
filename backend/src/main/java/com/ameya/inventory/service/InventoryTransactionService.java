package com.ameya.inventory.service;

import com.ameya.inventory.dto.assignment.AssignmentDtos;
import com.ameya.inventory.dto.inventory.InventoryDtos;
import com.ameya.inventory.dto.inventory.TransactionResponse;
import com.ameya.inventory.entity.AssignmentStatus;
import com.ameya.inventory.entity.Employee;
import com.ameya.inventory.entity.InventoryTransaction;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemCondition;
import com.ameya.inventory.entity.Machine;
import com.ameya.inventory.entity.StockAssignment;
import com.ameya.inventory.entity.TransactionType;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.EmployeeRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.MachineRepository;
import com.ameya.inventory.repository.StockAssignmentRepository;
import com.ameya.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The inventory ledger. Every method that mutates stock takes a
 * pessimistic write lock on the Item row first (see
 * ItemRepository.findByIdForUpdate) so two concurrent requests against
 * the same item are serialized rather than both reading the same
 * "available" figure and both succeeding - see Phase 1 doc G.2.
 *
 * current stock is never stored - it is always
 * sum(inventory_transactions.quantity) for that item (G.1). No method
 * here ever does item.setSomeStockField(...); every effect is a new
 * ledger row.
 */
@Service
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final ItemRepository itemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StockAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;

    @Transactional
    public InventoryDtos.IssueResponse issue(InventoryDtos.IssueRequest req, Long performedByUserId) {
        Item item = lockItem(req.itemId());
        if (!item.isActive()) {
            throw new BusinessRuleException("Item '" + item.getName() + "' is inactive and cannot be issued.");
        }
        Employee employee = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", req.employeeId()));
        if (!employee.isActive()) {
            throw new BusinessRuleException("Employee '" + employee.getName() + "' is inactive and cannot receive tools.");
        }
        Machine machine = findActiveMachineOrNull(req.machineId());

        BigDecimal available = transactionRepository.currentStock(item.getId());
        if (req.quantity().compareTo(available) > 0) {
            throw new BusinessRuleException("Cannot issue " + req.quantity() + " " + item.getUom().getCode() +
                    ". Only " + available + " " + item.getUom().getCode() + " currently available.");
        }

        User performedBy = getUser(performedByUserId);

        InventoryTransaction txn = newTxn(item, TransactionType.ISSUE_OUTWARD, req.quantity().negate(),
                item.getCurrentUnitCost(), performedBy);
        txn.setEmployee(employee);
        txn.setMachine(machine);
        txn.setPurpose(req.purpose());
        txn.setRemark(req.remark());
        transactionRepository.save(txn);

        StockAssignment assignment = new StockAssignment();
        assignment.setItem(item);
        assignment.setEmployee(employee);
        assignment.setMachine(machine);
        assignment.setAssignedQty(req.quantity());
        assignment.setReturnedQty(BigDecimal.ZERO);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setOpenedByTxn(txn);
        assignmentRepository.save(assignment);

        BigDecimal remaining = available.subtract(req.quantity());
        return new InventoryDtos.IssueResponse(toResponse(txn), toAssignmentResponse(assignment), remaining);
    }

    @Transactional
    public InventoryDtos.ReturnResponse returnStock(InventoryDtos.ReturnRequest req, Long performedByUserId) {
        StockAssignment assignment = assignmentRepository.findById(req.assignmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment", req.assignmentId()));
        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            throw new BusinessRuleException("Assignment " + assignment.getId() + " has already been fully returned.");
        }
        ItemCondition condition = parseCondition(req.condition());

        Item item = lockItem(assignment.getItem().getId());
        BigDecimal remaining = assignment.getAssignedQty().subtract(assignment.getReturnedQty());
        if (req.quantity().compareTo(remaining) > 0) {
            throw new BusinessRuleException("Cannot return " + req.quantity() + " " + item.getUom().getCode() +
                    ". Only " + remaining + " " + item.getUom().getCode() + " is currently assigned on this record.");
        }

        User performedBy = getUser(performedByUserId);

        // Goods physically come back into the store's custody first...
        InventoryTransaction returnTxn = newTxn(item, TransactionType.RETURN_FROM_USER, req.quantity(),
                item.getCurrentUnitCost(), performedBy);
        returnTxn.setEmployee(assignment.getEmployee());
        returnTxn.setMachine(assignment.getMachine());
        returnTxn.setItemCondition(condition);
        returnTxn.setRemark(req.remark());
        transactionRepository.save(returnTxn);
        List<TransactionResponse> responses = new ArrayList<>();
        responses.add(toResponse(returnTxn));

        // ...and if it isn't GOOD, it is immediately written off. Two
        // offsetting entries (not one zero-effect entry) so both "total
        // returned" and "total damaged/scrapped" reporting stay accurate.
        if (condition != ItemCondition.GOOD) {
            TransactionType writeOffType = condition == ItemCondition.DAMAGED ? TransactionType.DAMAGE : TransactionType.SCRAP;
            InventoryTransaction writeOffTxn = newTxn(item, writeOffType, req.quantity().negate(),
                    item.getCurrentUnitCost(), performedBy);
            writeOffTxn.setEmployee(assignment.getEmployee());
            writeOffTxn.setMachine(assignment.getMachine());
            writeOffTxn.setItemCondition(condition);
            writeOffTxn.setRemark(req.remark());
            transactionRepository.save(writeOffTxn);
            responses.add(toResponse(writeOffTxn));
        }

        assignment.setReturnedQty(assignment.getReturnedQty().add(req.quantity()));
        if (assignment.getReturnedQty().compareTo(assignment.getAssignedQty()) >= 0) {
            assignment.setStatus(AssignmentStatus.CLOSED);
            assignment.setClosedAt(Instant.now());
        } else {
            assignment.setStatus(AssignmentStatus.PARTIALLY_RETURNED);
        }
        assignmentRepository.save(assignment);

        return new InventoryDtos.ReturnResponse(responses, toAssignmentResponse(assignment));
    }

    @Transactional
    public InventoryDtos.StockMutationResponse openingBalance(InventoryDtos.OpeningBalanceRequest req, Long performedByUserId) {
        Item item = lockItem(req.itemId());
        if (transactionRepository.existsByItem_Id(item.getId())) {
            throw new BusinessRuleException("Item '" + item.getName() +
                    "' already has transaction history; opening balance can only be recorded once, before any other movement.");
        }

        item.setCurrentUnitCost(req.unitCost());
        itemRepository.save(item);

        User performedBy = getUser(performedByUserId);
        InventoryTransaction txn = newTxn(item, TransactionType.OPENING_BALANCE, req.quantity(), req.unitCost(), performedBy);
        txn.setRemark(req.remark());
        transactionRepository.save(txn);

        return new InventoryDtos.StockMutationResponse(toResponse(txn), req.quantity());
    }

    @Transactional
    public InventoryDtos.InwardResponse purchaseInward(InventoryDtos.PurchaseInwardRequest req, Long performedByUserId) {
        Item item = lockItem(req.itemId());
        BigDecimal currentQty = transactionRepository.currentStock(item.getId()).max(BigDecimal.ZERO);
        BigDecimal currentValue = currentQty.multiply(item.getCurrentUnitCost());
        BigDecimal incomingValue = req.quantity().multiply(req.unitCost());
        BigDecimal totalQty = currentQty.add(req.quantity());

        BigDecimal newAverage = totalQty.signum() == 0
                ? req.unitCost()
                : currentValue.add(incomingValue).divide(totalQty, 2, RoundingMode.HALF_UP);
        item.setCurrentUnitCost(newAverage);
        itemRepository.save(item);

        User performedBy = getUser(performedByUserId);
        InventoryTransaction txn = newTxn(item, TransactionType.PURCHASE_INWARD, req.quantity(), req.unitCost(), performedBy);
        txn.setRemark(req.remark());
        transactionRepository.save(txn);

        return new InventoryDtos.InwardResponse(toResponse(txn), newAverage, totalQty);
    }

    @Transactional
    public InventoryDtos.StockMutationResponse adjust(InventoryDtos.AdjustmentRequest req, Long performedByUserId) {
        Item item = lockItem(req.itemId());
        boolean isIn = "IN".equalsIgnoreCase(req.direction());
        boolean isOut = "OUT".equalsIgnoreCase(req.direction());
        if (!isIn && !isOut) {
            throw new BusinessRuleException("Direction must be IN or OUT.");
        }

        BigDecimal signedQty;
        TransactionType type;
        if (isIn) {
            type = TransactionType.STOCK_ADJUSTMENT_IN;
            signedQty = req.quantity();
        } else {
            BigDecimal available = transactionRepository.currentStock(item.getId());
            if (req.quantity().compareTo(available) > 0) {
                throw new BusinessRuleException("Cannot adjust out " + req.quantity() + " " + item.getUom().getCode() +
                        ". Only " + available + " " + item.getUom().getCode() + " currently available.");
            }
            type = TransactionType.STOCK_ADJUSTMENT_OUT;
            signedQty = req.quantity().negate();
        }

        User performedBy = getUser(performedByUserId);
        InventoryTransaction txn = newTxn(item, type, signedQty, item.getCurrentUnitCost(), performedBy);
        txn.setRemark(req.reason());
        transactionRepository.save(txn);

        return new InventoryDtos.StockMutationResponse(toResponse(txn), transactionRepository.currentStock(item.getId()));
    }

    @Transactional
    public InventoryDtos.StockMutationResponse damageOrScrap(InventoryDtos.DamageScrapRequest req, Long performedByUserId) {
        Item item = lockItem(req.itemId());
        TransactionType type = "DAMAGE".equalsIgnoreCase(req.type()) ? TransactionType.DAMAGE
                : "SCRAP".equalsIgnoreCase(req.type()) ? TransactionType.SCRAP
                : null;
        if (type == null) {
            throw new BusinessRuleException("Type must be DAMAGE or SCRAP.");
        }
        BigDecimal available = transactionRepository.currentStock(item.getId());
        if (req.quantity().compareTo(available) > 0) {
            throw new BusinessRuleException("Cannot write off " + req.quantity() + " " + item.getUom().getCode() +
                    ". Only " + available + " " + item.getUom().getCode() + " currently available.");
        }

        User performedBy = getUser(performedByUserId);
        InventoryTransaction txn = newTxn(item, type, req.quantity().negate(), item.getCurrentUnitCost(), performedBy);
        txn.setRemark(req.reason());
        transactionRepository.save(txn);

        return new InventoryDtos.StockMutationResponse(toResponse(txn), transactionRepository.currentStock(item.getId()));
    }

    @Transactional
    public InventoryDtos.StockMutationResponse reverse(InventoryDtos.ReversalRequest req, Long performedByUserId) {
        InventoryTransaction original = transactionRepository.findById(req.transactionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", req.transactionId()));
        if (original.getTxnType() == TransactionType.REVERSAL) {
            throw new BusinessRuleException("Transaction " + original.getTxnNo() + " is itself a reversal and cannot be reversed.");
        }
        if (transactionRepository.existsByReversalOfTxn_Id(original.getId())) {
            throw new BusinessRuleException("Transaction " + original.getTxnNo() + " has already been reversed.");
        }

        Item item = lockItem(original.getItem().getId());

        User performedBy = getUser(performedByUserId);
        InventoryTransaction reversal = newTxn(item, TransactionType.REVERSAL, original.getQuantity().negate(),
                original.getUnitCostAtTxn(), performedBy);
        reversal.setEmployee(original.getEmployee());
        reversal.setMachine(original.getMachine());
        reversal.setReversalOfTxn(original);
        reversal.setRemark(req.reason());
        transactionRepository.save(reversal);

        return new InventoryDtos.StockMutationResponse(toResponse(reversal), transactionRepository.currentStock(item.getId()));
    }

    @Transactional(readOnly = true)
    public InventoryDtos.CurrentStockResponse getCurrentStock(Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> ResourceNotFoundException.of("Item", itemId));
        BigDecimal stock = transactionRepository.currentStock(itemId);
        String status = stock.signum() <= 0 ? "OUT_OF_STOCK"
                : stock.compareTo(item.getSafeStock()) <= 0 ? "LOW_STOCK"
                : "NORMAL";
        return new InventoryDtos.CurrentStockResponse(
                item.getId(), item.getItemCode(), item.getName(), stock, item.getSafeStock(), item.getMaxStock(),
                item.getCurrentUnitCost(), stock.multiply(item.getCurrentUnitCost()), status);
    }

    @Transactional(readOnly = true)
    public InventoryDtos.StockSummaryResponse getStockSummary(Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> ResourceNotFoundException.of("Item", itemId));
        Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
        for (TransactionType t : TransactionType.values()) {
            totals.put(t, BigDecimal.ZERO);
        }
        for (InventoryTransactionRepository.TxnTypeTotal row : transactionRepository.sumByType(itemId)) {
            totals.put(row.getType(), row.getTotal());
        }

        BigDecimal opening = totals.get(TransactionType.OPENING_BALANCE);
        BigDecimal purchased = totals.get(TransactionType.PURCHASE_INWARD);
        BigDecimal issued = totals.get(TransactionType.ISSUE_OUTWARD).abs();
        BigDecimal returned = totals.get(TransactionType.RETURN_FROM_USER).add(totals.get(TransactionType.RETURN_TO_STOCK));
        BigDecimal adjIn = totals.get(TransactionType.STOCK_ADJUSTMENT_IN);
        BigDecimal adjOut = totals.get(TransactionType.STOCK_ADJUSTMENT_OUT).abs();
        BigDecimal damaged = totals.get(TransactionType.DAMAGE).abs();
        BigDecimal scrapped = totals.get(TransactionType.SCRAP).abs();
        BigDecimal current = transactionRepository.currentStock(itemId);

        return new InventoryDtos.StockSummaryResponse(item.getId(), item.getItemCode(), item.getName(),
                opening, purchased, issued, returned, adjIn, adjOut, damaged, scrapped, current);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistory(Long itemId, Pageable pageable) {
        return transactionRepository.findByItem_IdOrderByTxnDateDescCreatedAtDesc(itemId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistoryByMachine(Long machineId, Pageable pageable) {
        return transactionRepository.findByMachine_IdOrderByTxnDateDescCreatedAtDesc(machineId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistoryByEmployee(Long employeeId, Pageable pageable) {
        return transactionRepository.findByEmployee_IdOrderByTxnDateDescCreatedAtDesc(employeeId, pageable).map(this::toResponse);
    }

    private Item lockItem(Long itemId) {
        return itemRepository.findByIdForUpdate(itemId).orElseThrow(() -> ResourceNotFoundException.of("Item", itemId));
    }

    private Machine findActiveMachineOrNull(Long machineId) {
        if (machineId == null) {
            return null;
        }
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> ResourceNotFoundException.of("Machine", machineId));
        if (!machine.isActive()) {
            throw new BusinessRuleException("Machine '" + machine.getMachineName() + "' is inactive and cannot be selected.");
        }
        return machine;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private ItemCondition parseCondition(String raw) {
        try {
            return ItemCondition.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Condition must be GOOD, DAMAGED or SCRAP.");
        }
    }

    private InventoryTransaction newTxn(Item item, TransactionType type, BigDecimal signedQuantity,
                                         BigDecimal unitCost, User performedBy) {
        InventoryTransaction txn = new InventoryTransaction();
        txn.setTxnNo(generateTxnNo());
        txn.setItem(item);
        txn.setTxnType(type);
        txn.setQuantity(signedQuantity);
        txn.setUnitCostAtTxn(unitCost);
        txn.setPerformedBy(performedBy);
        txn.setTxnDate(LocalDate.now());
        return txn;
    }

    private String generateTxnNo() {
        long millis = Instant.now().toEpochMilli() % 1_000_000_000L;
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TXN" + millis + rand;
    }

    private TransactionResponse toResponse(InventoryTransaction t) {
        return new TransactionResponse(
                t.getId(), t.getTxnNo(),
                t.getItem().getId(), t.getItem().getItemCode(), t.getItem().getName(),
                t.getTxnType().name(), t.getQuantity(), t.getUnitCostAtTxn(),
                t.getQuantity().abs().multiply(t.getUnitCostAtTxn()),
                t.getMachine() != null ? t.getMachine().getId() : null,
                t.getMachine() != null ? t.getMachine().getMachineCode() : null,
                t.getEmployee() != null ? t.getEmployee().getId() : null,
                t.getEmployee() != null ? t.getEmployee().getName() : null,
                t.getPerformedBy().getUsername(),
                t.getPurpose(), t.getRemark(),
                t.getItemCondition() != null ? t.getItemCondition().name() : null,
                t.getReversalOfTxn() != null ? t.getReversalOfTxn().getId() : null,
                t.getSource().name(), t.getTxnDate(), t.getCreatedAt());
    }

    private AssignmentDtos.Response toAssignmentResponse(StockAssignment a) {
        return new AssignmentDtos.Response(
                a.getId(), a.getItem().getId(), a.getItem().getItemCode(), a.getItem().getName(),
                a.getEmployee().getId(), a.getEmployee().getName(),
                a.getMachine() != null ? a.getMachine().getId() : null,
                a.getMachine() != null ? a.getMachine().getMachineCode() : null,
                a.getAssignedQty(), a.getReturnedQty(), a.getAssignedQty().subtract(a.getReturnedQty()),
                a.getStatus().name(), a.getOpenedAt(), a.getClosedAt());
    }
}
