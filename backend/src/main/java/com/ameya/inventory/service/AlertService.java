package com.ameya.inventory.service;

import com.ameya.inventory.dto.alert.AlertDtos;
import com.ameya.inventory.entity.Alert;
import com.ameya.inventory.entity.AlertStatus;
import com.ameya.inventory.entity.AlertType;
import com.ameya.inventory.entity.AssignmentStatus;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.entity.StockAssignment;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.AlertRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.PurchaseRequisitionItemRepository;
import com.ameya.inventory.repository.StockAssignmentRepository;
import com.ameya.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects the six alert conditions the DB schema already models (see V1
 * migration's chk_alerts_type) and keeps the `alerts` table in sync with
 * current reality. Every check is keyed by item id and is idempotent:
 * re-running it refreshes existing OPEN/ACKNOWLEDGED alerts of that type
 * instead of duplicating them, and auto-resolves ones whose condition no
 * longer holds. This is a V1 heuristic set, not a tuned ML model - factors
 * are config-driven (see application.yml `app.alerts.*`) so they can be
 * adjusted without a code change.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<PrStatus> OPEN_PR_STATUSES = List.of(PrStatus.SUBMITTED, PrStatus.APPROVED, PrStatus.ORDERED);
    private static final List<AssignmentStatus> OPEN_ASSIGNMENT_STATUSES = List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.PARTIALLY_RETURNED);

    private final AlertRepository alertRepository;
    private final ItemRepository itemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StockAssignmentRepository assignmentRepository;
    private final PurchaseRequisitionItemRepository prItemRepository;
    private final UserRepository userRepository;

    @Value("${app.alerts.pending-return-days:30}")
    private int pendingReturnDays;

    @Value("${app.alerts.high-consumption-factor:2.0}")
    private double highConsumptionFactor;

    @Scheduled(fixedRateString = "${app.alerts.recompute-interval-ms:900000}", initialDelay = 30000)
    @Transactional
    public void scheduledRecompute() {
        recomputeAll();
    }

    @Transactional
    public AlertDtos.RecomputeResult recomputeAll() {
        List<Item> activeItems = itemRepository.findByActiveTrue();
        Map<Long, BigDecimal> stockByItem = new HashMap<>();
        transactionRepository.allItemStocks().forEach(r -> stockByItem.put(r.getItemId(), r.getStock()));

        int raised = 0;
        int resolved = 0;

        Result lowStock = checkLowAndOutOfStock(activeItems, stockByItem);
        raised += upsert(AlertType.LOW_STOCK, lowStock.low());
        resolved += autoResolve(AlertType.LOW_STOCK, lowStock.low());
        raised += upsert(AlertType.OUT_OF_STOCK, lowStock.out());
        resolved += autoResolve(AlertType.OUT_OF_STOCK, lowStock.out());

        ConsumptionResult consumption = checkConsumptionSpikes();
        raised += upsert(AlertType.HIGH_CONSUMPTION, consumption.high());
        resolved += autoResolve(AlertType.HIGH_CONSUMPTION, consumption.high());
        raised += upsert(AlertType.UNUSUAL_CONSUMPTION, consumption.unusual());
        resolved += autoResolve(AlertType.UNUSUAL_CONSUMPTION, consumption.unusual());

        Map<Long, String> pendingReturn = checkPendingReturns();
        raised += upsert(AlertType.PENDING_RETURN, pendingReturn);
        resolved += autoResolve(AlertType.PENDING_RETURN, pendingReturn);

        Map<Long, String> purchasePending = checkPurchasePending(activeItems, stockByItem);
        raised += upsert(AlertType.PURCHASE_PENDING, purchasePending);
        resolved += autoResolve(AlertType.PURCHASE_PENDING, purchasePending);

        return new AlertDtos.RecomputeResult(raised, resolved);
    }

    private record Result(Map<Long, String> low, Map<Long, String> out) {
    }

    private record ConsumptionResult(Map<Long, String> high, Map<Long, String> unusual) {
    }

    private Result checkLowAndOutOfStock(List<Item> activeItems, Map<Long, BigDecimal> stockByItem) {
        Map<Long, String> low = new LinkedHashMap<>();
        Map<Long, String> out = new LinkedHashMap<>();
        for (Item item : activeItems) {
            BigDecimal stock = stockByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (stock.signum() <= 0) {
                out.put(item.getId(), "'" + item.getName() + "' is out of stock (0 " + item.getUom().getCode() + ").");
            } else if (item.getSafeStock().signum() > 0 && stock.compareTo(item.getSafeStock()) <= 0) {
                low.put(item.getId(), "'" + item.getName() + "' is at " + stock + " " + item.getUom().getCode() +
                        ", at or below safe stock of " + item.getSafeStock() + ".");
            }
        }
        return new Result(low, out);
    }

    private ConsumptionResult checkConsumptionSpikes() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusMonths(3).withDayOfMonth(1);
        String currentYm = to.format(YM);

        Map<Long, Map<String, BigDecimal>> byItem = new HashMap<>();
        for (var row : transactionRepository.monthlyIssuedQtyByItem(from, to)) {
            byItem.computeIfAbsent(row.getItemId(), k -> new HashMap<>()).put(row.getYm(), row.getQty());
        }

        Map<Long, String> high = new LinkedHashMap<>();
        Map<Long, String> unusual = new LinkedHashMap<>();
        Map<Long, Item> itemCache = new HashMap<>();

        for (var entry : byItem.entrySet()) {
            Map<String, BigDecimal> months = entry.getValue();
            BigDecimal current = months.getOrDefault(currentYm, BigDecimal.ZERO);
            if (current.signum() <= 0) {
                continue;
            }
            BigDecimal trailingSum = BigDecimal.ZERO;
            int trailingCount = 0;
            for (var monthEntry : months.entrySet()) {
                if (!monthEntry.getKey().equals(currentYm)) {
                    trailingSum = trailingSum.add(monthEntry.getValue());
                    trailingCount++;
                }
            }
            Item item = itemCache.computeIfAbsent(entry.getKey(),
                    id -> itemRepository.findById(id).orElse(null));
            if (item == null) {
                continue;
            }
            if (trailingCount == 0) {
                unusual.put(item.getId(), "'" + item.getName() + "' consumed " + current + " " + item.getUom().getCode() +
                        " this month with no consumption in the prior 3 months.");
            } else {
                BigDecimal trailingAvg = trailingSum.divide(BigDecimal.valueOf(trailingCount), 3, java.math.RoundingMode.HALF_UP);
                if (trailingAvg.signum() > 0 && current.compareTo(trailingAvg.multiply(BigDecimal.valueOf(highConsumptionFactor))) > 0) {
                    high.put(item.getId(), "'" + item.getName() + "' consumed " + current + " " + item.getUom().getCode() +
                            " this month, over " + highConsumptionFactor + "x its trailing 3-month average of " + trailingAvg + ".");
                }
            }
        }
        return new ConsumptionResult(high, unusual);
    }

    private Map<Long, String> checkPendingReturns() {
        Instant threshold = Instant.now().minus(pendingReturnDays, ChronoUnit.DAYS);
        List<StockAssignment> overdue = assignmentRepository.findByStatusInAndOpenedAtBefore(OPEN_ASSIGNMENT_STATUSES, threshold);
        Map<Long, Long> countByItem = new LinkedHashMap<>();
        Map<Long, Item> itemCache = new HashMap<>();
        for (StockAssignment a : overdue) {
            countByItem.merge(a.getItem().getId(), 1L, Long::sum);
            itemCache.putIfAbsent(a.getItem().getId(), a.getItem());
        }
        Map<Long, String> result = new LinkedHashMap<>();
        countByItem.forEach((itemId, count) -> {
            Item item = itemCache.get(itemId);
            result.put(itemId, count + " assignment(s) of '" + item.getName() + "' have been open more than " +
                    pendingReturnDays + " days without a return.");
        });
        return result;
    }

    private Map<Long, String> checkPurchasePending(List<Item> activeItems, Map<Long, BigDecimal> stockByItem) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (Item item : activeItems) {
            BigDecimal stock = stockByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
            boolean belowSafe = item.getSafeStock().signum() > 0 && stock.compareTo(item.getSafeStock()) <= 0;
            if (belowSafe && !prItemRepository.existsByItem_IdAndPr_StatusIn(item.getId(), OPEN_PR_STATUSES)) {
                result.put(item.getId(), "'" + item.getName() + "' is below safe stock and has no open purchase requisition covering it.");
            }
        }
        return result;
    }

    private int upsert(AlertType type, Map<Long, String> violating) {
        List<Alert> openOfType = alertRepository.findByTypeAndStatusIn(type, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));
        Map<Long, Alert> openByItem = new HashMap<>();
        for (Alert a : openOfType) {
            if (a.getItem() != null) {
                openByItem.put(a.getItem().getId(), a);
            }
        }
        int raised = 0;
        for (var entry : violating.entrySet()) {
            Alert existing = openByItem.get(entry.getKey());
            if (existing == null) {
                Alert a = new Alert();
                a.setType(type);
                a.setItem(itemRepository.getReferenceById(entry.getKey()));
                a.setMessage(entry.getValue());
                a.setStatus(AlertStatus.OPEN);
                a.setRaisedAt(Instant.now());
                alertRepository.save(a);
                raised++;
            } else if (!existing.getMessage().equals(entry.getValue())) {
                existing.setMessage(entry.getValue());
                alertRepository.save(existing);
            }
        }
        return raised;
    }

    private int autoResolve(AlertType type, Map<Long, String> stillViolating) {
        List<Alert> openOfType = alertRepository.findByTypeAndStatusIn(type, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));
        int resolved = 0;
        for (Alert a : openOfType) {
            Long itemId = a.getItem() != null ? a.getItem().getId() : null;
            if (itemId == null || !stillViolating.containsKey(itemId)) {
                a.setStatus(AlertStatus.RESOLVED);
                a.setResolvedAt(Instant.now());
                alertRepository.save(a);
                resolved++;
            }
        }
        return resolved;
    }

    @Transactional(readOnly = true)
    public Page<AlertDtos.Response> list(String status, String type, Pageable pageable) {
        AlertStatus s = parseEnum(status, AlertStatus.class, "status");
        AlertType t = parseEnum(type, AlertType.class, "type");
        return alertRepository.search(s, t, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return alertRepository.countByStatus(AlertStatus.OPEN);
    }

    @Transactional
    public AlertDtos.Response acknowledge(Long id, Long userId) {
        Alert a = alertRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Alert", id));
        if (a.getStatus() == AlertStatus.RESOLVED) {
            throw new BusinessRuleException("Alert " + id + " is already resolved.");
        }
        a.setStatus(AlertStatus.ACKNOWLEDGED);
        a.setAcknowledgedBy(getUser(userId));
        a.setAcknowledgedAt(Instant.now());
        return toResponse(alertRepository.save(a));
    }

    @Transactional
    public AlertDtos.Response resolve(Long id, Long userId) {
        Alert a = alertRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Alert", id));
        a.setStatus(AlertStatus.RESOLVED);
        a.setResolvedBy(getUser(userId));
        a.setResolvedAt(Instant.now());
        return toResponse(alertRepository.save(a));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
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

    private AlertDtos.Response toResponse(Alert a) {
        return new AlertDtos.Response(
                a.getId(), a.getType().name(),
                a.getItem() != null ? a.getItem().getId() : null,
                a.getItem() != null ? a.getItem().getItemCode() : null,
                a.getItem() != null ? a.getItem().getName() : null,
                a.getMessage(), a.getStatus().name(), a.getRaisedAt(),
                a.getAcknowledgedBy() != null ? a.getAcknowledgedBy().getUsername() : null,
                a.getAcknowledgedAt(),
                a.getResolvedBy() != null ? a.getResolvedBy().getUsername() : null,
                a.getResolvedAt());
    }
}
