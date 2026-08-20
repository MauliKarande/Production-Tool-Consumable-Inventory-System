package com.ameya.inventory.service;

import com.ameya.inventory.entity.Alert;
import com.ameya.inventory.entity.AlertStatus;
import com.ameya.inventory.entity.AlertType;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.repository.AlertRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.PurchaseRequisitionItemRepository;
import com.ameya.inventory.repository.StockAssignmentRepository;
import com.ameya.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the idempotent upsert/auto-resolve pattern that every alert type
 * shares: recomputing must raise a new alert the first time a condition
 * is true, and must not raise a duplicate or a fresh one once the
 * condition already has an OPEN alert - then must resolve it once the
 * condition stops holding.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock private AlertRepository alertRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private StockAssignmentRepository assignmentRepository;
    @Mock private PurchaseRequisitionItemRepository prItemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AlertService service;

    private Item lowStockItem;

    @BeforeEach
    void setUp() {
        ItemCategory category = new ItemCategory();
        category.setId(1L);
        category.setName("INSERTS");
        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setId(1L);
        uom.setCode("PCS");

        lowStockItem = new Item();
        lowStockItem.setId(10L);
        lowStockItem.setItemCode("INS-0001");
        lowStockItem.setName("Test Insert");
        lowStockItem.setCategory(category);
        lowStockItem.setUom(uom);
        lowStockItem.setSafeStock(new BigDecimal("10"));

        ReflectionTestUtils.setField(service, "pendingReturnDays", 30);
        ReflectionTestUtils.setField(service, "highConsumptionFactor", 2.0);

        lenient().when(itemRepository.findByActiveTrue()).thenReturn(List.of(lowStockItem));
        lenient().when(transactionRepository.allItemStocks()).thenReturn(List.of(stockRow(10L, new BigDecimal("5"))));
        lenient().when(transactionRepository.monthlyIssuedQtyByItem(any(), any())).thenReturn(List.of());
        lenient().when(assignmentRepository.findByStatusInAndOpenedAtBefore(any(), any())).thenReturn(List.of());
        lenient().when(prItemRepository.existsByItem_IdAndPr_StatusIn(eq(10L), any())).thenReturn(false);
        lenient().when(alertRepository.findByTypeAndStatusIn(any(), any())).thenReturn(List.of());
        lenient().when(itemRepository.getReferenceById(10L)).thenReturn(lowStockItem);
        lenient().when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private InventoryTransactionRepository.ItemStockRow stockRow(Long itemId, BigDecimal stock) {
        return new InventoryTransactionRepository.ItemStockRow() {
            public Long getItemId() { return itemId; }
            public BigDecimal getStock() { return stock; }
        };
    }

    @Test
    void recomputeAll_raisesLowStockAlert_whenStockAtOrBelowSafeStock() {
        service.recomputeAll();

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Alert::getType)
                .contains(AlertType.LOW_STOCK, AlertType.PURCHASE_PENDING);
        assertThat(captor.getAllValues()).allSatisfy(a -> assertThat(a.getStatus()).isEqualTo(AlertStatus.OPEN));
    }

    @Test
    void recomputeAll_doesNotDuplicate_whenAnOpenAlertOfTheSameTypeAlreadyExists() {
        Alert existing = new Alert();
        existing.setType(AlertType.LOW_STOCK);
        existing.setItem(lowStockItem);
        existing.setStatus(AlertStatus.OPEN);
        existing.setMessage("'Test Insert' is at 5 PCS, at or below safe stock of 10.");
        when(alertRepository.findByTypeAndStatusIn(eq(AlertType.LOW_STOCK), any())).thenReturn(List.of(existing));

        service.recomputeAll();

        // The identical message means no update save for LOW_STOCK - only the new PURCHASE_PENDING alert is saved.
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(AlertType.PURCHASE_PENDING);
    }

    @Test
    void recomputeAll_resolvesOpenAlert_whenStockNoLongerLow() {
        lowStockItem.setSafeStock(new BigDecimal("2")); // 5 in stock is no longer <= safe stock of 2
        Alert existing = new Alert();
        existing.setId(500L);
        existing.setType(AlertType.LOW_STOCK);
        existing.setItem(lowStockItem);
        existing.setStatus(AlertStatus.OPEN);
        when(alertRepository.findByTypeAndStatusIn(eq(AlertType.LOW_STOCK), any())).thenReturn(List.of(existing));

        service.recomputeAll();

        assertThat(existing.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        verify(alertRepository, never()).save(argThatIsLowStockCreate());
    }

    private Alert argThatIsLowStockCreate() {
        return org.mockito.ArgumentMatchers.argThat(a -> a.getId() == null && a.getType() == AlertType.LOW_STOCK);
    }
}
