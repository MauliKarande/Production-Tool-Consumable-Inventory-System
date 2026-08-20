package com.ameya.inventory.service;

import com.ameya.inventory.dto.inventory.InventoryDtos;
import com.ameya.inventory.dto.purchase.PurchaseRequisitionDtos;
import com.ameya.inventory.entity.Department;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.PrPriority;
import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.entity.PurchaseRequisition;
import com.ameya.inventory.entity.PurchaseRequisitionItem;
import com.ameya.inventory.entity.Supplier;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.DepartmentRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.PurchaseRequisitionRepository;
import com.ameya.inventory.repository.SupplierRepository;
import com.ameya.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PR lifecycle: DRAFT -> SUBMITTED -> APPROVED/REJECTED -> ORDERED ->
 * RECEIVED -> CLOSED (Phase 1 doc §J.4). The approval gate is enforced
 * here, not just by role: the approver must be a different user than the
 * requester, even if both happen to be Admin, per the confirmed decision
 * that a real second-person approval is required in V1.
 */
@Service
@RequiredArgsConstructor
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository prRepository;
    private final ItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionService inventoryTransactionService;

    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDtos.Response> search(String status, String priority, Long departmentId, Pageable pageable) {
        var spec = PurchaseRequisitionSpecifications.and(
                PurchaseRequisitionSpecifications.status(parseEnum(status, PrStatus.class, "status")),
                PurchaseRequisitionSpecifications.priority(parseEnum(priority, PrPriority.class, "priority")),
                PurchaseRequisitionSpecifications.departmentId(departmentId)
        );
        return prRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseRequisitionDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PurchaseRequisitionDtos.Response create(PurchaseRequisitionDtos.CreateRequest req, Long userId) {
        PurchaseRequisition pr = new PurchaseRequisition();
        pr.setPrNo(generatePrNo());
        pr.setRequestedBy(getUser(userId));
        pr.setDepartment(findDepartmentOrNull(req.departmentId()));
        pr.setStatus(PrStatus.DRAFT);
        pr.setPriority(parseEnum(req.priority(), PrPriority.class, "priority"));
        pr.setReason(req.reason());

        for (PurchaseRequisitionDtos.LineRequest line : req.items()) {
            PurchaseRequisitionItem item = new PurchaseRequisitionItem();
            item.setPr(pr);
            item.setItem(findItem(line.itemId()));
            item.setQuantity(line.quantity());
            item.setEstimatedPrice(line.estimatedPrice());
            item.setSupplier(findSupplierOrNull(line.supplierId()));
            item.setReceivedQty(BigDecimal.ZERO);
            pr.getItems().add(item);
        }

        return toResponse(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequisitionDtos.Response submit(Long id, Long userId) {
        PurchaseRequisition pr = findOrThrow(id);
        requireStatus(pr, PrStatus.DRAFT, "submitted");
        pr.setStatus(PrStatus.SUBMITTED);
        return toResponse(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequisitionDtos.Response approve(Long id, Long approverUserId) {
        PurchaseRequisition pr = findOrThrow(id);
        requireStatus(pr, PrStatus.SUBMITTED, "approved");
        if (pr.getRequestedBy().getId().equals(approverUserId)) {
            throw new BusinessRuleException("The requester cannot approve their own requisition " + pr.getPrNo() + " - a different user must approve it.");
        }
        pr.setStatus(PrStatus.APPROVED);
        pr.setApprovedBy(getUser(approverUserId));
        pr.setApprovedAt(Instant.now());
        return toResponse(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequisitionDtos.Response reject(Long id, PurchaseRequisitionDtos.RejectRequest req, Long userId) {
        PurchaseRequisition pr = findOrThrow(id);
        requireStatus(pr, PrStatus.SUBMITTED, "rejected");
        if (pr.getRequestedBy().getId().equals(userId)) {
            throw new BusinessRuleException("The requester cannot reject their own requisition " + pr.getPrNo() + " - a different user must decide it.");
        }
        pr.setStatus(PrStatus.REJECTED);
        pr.setApprovedBy(getUser(userId));
        pr.setApprovedAt(Instant.now());
        pr.setReason(req.reason());
        return toResponse(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequisitionDtos.Response markOrdered(Long id) {
        PurchaseRequisition pr = findOrThrow(id);
        requireStatus(pr, PrStatus.APPROVED, "marked as ordered");
        pr.setStatus(PrStatus.ORDERED);
        return toResponse(prRepository.save(pr));
    }

    /**
     * Receives goods against an ORDERED PR: posts one PURCHASE_INWARD per
     * line (reusing InventoryTransactionService, so weighted-average cost
     * and locking stay uniform), then transitions the PR to RECEIVED. A
     * line can carry directToFloor=true to immediately post a linked
     * ISSUE_OUTWARD for goods that bypass the store (§J.5) - the same
     * real-world pattern found in the legacy Purchase file's "USE ON SHOP
     * FLOOR" column.
     */
    @Transactional
    public PurchaseRequisitionDtos.Response receiveGoods(Long id, PurchaseRequisitionDtos.ReceiveRequest req, Long userId) {
        PurchaseRequisition pr = findOrThrow(id);
        requireStatus(pr, PrStatus.ORDERED, "received");

        for (PurchaseRequisitionDtos.ReceiveLineRequest line : req.lines()) {
            PurchaseRequisitionItem prItem = pr.getItems().stream()
                    .filter(i -> i.getId().equals(line.prItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("PR line " + line.prItemId() + " does not belong to requisition " + pr.getPrNo() + "."));

            BigDecimal alreadyReceived = prItem.getReceivedQty() != null ? prItem.getReceivedQty() : BigDecimal.ZERO;
            if (alreadyReceived.add(line.receivedQty()).compareTo(prItem.getQuantity()) > 0) {
                throw new BusinessRuleException("Cannot receive " + line.receivedQty() + " for '" + prItem.getItem().getName() +
                        "' - only " + prItem.getQuantity().subtract(alreadyReceived) + " remains against the requisitioned quantity of " + prItem.getQuantity() + ".");
            }

            InventoryDtos.PurchaseInwardRequest inwardReq = new InventoryDtos.PurchaseInwardRequest(
                    prItem.getItem().getId(), line.receivedQty(), line.unitCost(),
                    "Receipt against PR " + pr.getPrNo());
            InventoryDtos.InwardResponse inward = inventoryTransactionService.purchaseInward(inwardReq, userId);

            prItem.setReceivedQty(alreadyReceived.add(line.receivedQty()));
            prItem.setReceivedTxn(transactionRepository.getReferenceById(inward.transaction().id()));

            if (line.directToFloor()) {
                inventoryTransactionService.postDirectIssue(
                        prItem.getItem().getId(), line.receivedQty(), line.machineId(),
                        "Direct-to-floor issue at receipt",
                        "PR " + pr.getPrNo() + " - direct-to-floor issue at receipt (legacy pattern, §J.5)", userId);
            }
        }

        pr.setStatus(PrStatus.RECEIVED);
        return toResponse(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequisitionDtos.Response close(Long id) {
        PurchaseRequisition pr = findOrThrow(id);
        requireStatus(pr, PrStatus.RECEIVED, "closed");
        pr.setStatus(PrStatus.CLOSED);
        return toResponse(prRepository.save(pr));
    }

    private void requireStatus(PurchaseRequisition pr, PrStatus expected, String actionPastTense) {
        if (pr.getStatus() != expected) {
            throw new BusinessRuleException("Requisition " + pr.getPrNo() + " cannot be " + actionPastTense +
                    " from status " + pr.getStatus() + " (expected " + expected + ").");
        }
    }

    private PurchaseRequisition findOrThrow(Long id) {
        return prRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Purchase requisition", id));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Item", id));
    }

    private Supplier findSupplierOrNull(Long id) {
        if (id == null) {
            return null;
        }
        return supplierRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    private Department findDepartmentOrNull(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private <E extends Enum<E>> E parseEnum(String raw, Class<E> type, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Invalid " + field + ": " + raw);
        }
    }

    private String generatePrNo() {
        long millis = Instant.now().truncatedTo(ChronoUnit.SECONDS).toEpochMilli() % 1_000_000_000L;
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PR" + millis + rand;
    }

    private PurchaseRequisitionDtos.Response toResponse(PurchaseRequisition pr) {
        List<PurchaseRequisitionDtos.LineResponse> lines = pr.getItems().stream()
                .map(i -> new PurchaseRequisitionDtos.LineResponse(
                        i.getId(), i.getItem().getId(), i.getItem().getItemCode(), i.getItem().getName(),
                        i.getQuantity(), i.getEstimatedPrice(),
                        i.getSupplier() != null ? i.getSupplier().getId() : null,
                        i.getSupplier() != null ? i.getSupplier().getName() : null,
                        i.getReceivedQty(),
                        i.getReceivedTxn() != null ? i.getReceivedTxn().getId() : null))
                .toList();

        return new PurchaseRequisitionDtos.Response(
                pr.getId(), pr.getPrNo(),
                pr.getRequestedBy().getId(), pr.getRequestedBy().getUsername(),
                pr.getDepartment() != null ? pr.getDepartment().getId() : null,
                pr.getDepartment() != null ? pr.getDepartment().getName() : null,
                pr.getStatus().name(), pr.getPriority() != null ? pr.getPriority().name() : null, pr.getReason(),
                pr.getApprovedBy() != null ? pr.getApprovedBy().getId() : null,
                pr.getApprovedBy() != null ? pr.getApprovedBy().getUsername() : null,
                pr.getApprovedAt(), pr.getCreatedAt(), pr.getUpdatedAt(), lines);
    }
}
