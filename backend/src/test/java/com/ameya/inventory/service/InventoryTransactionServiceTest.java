package com.ameya.inventory.service;

import com.ameya.inventory.dto.inventory.InventoryDtos;
import com.ameya.inventory.entity.AssignmentStatus;
import com.ameya.inventory.entity.Employee;
import com.ameya.inventory.entity.InventoryTransaction;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.Role;
import com.ameya.inventory.entity.StockAssignment;
import com.ameya.inventory.entity.TransactionType;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.repository.EmployeeRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.MachineRepository;
import com.ameya.inventory.repository.StockAssignmentRepository;
import com.ameya.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryTransactionServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private StockAssignmentRepository assignmentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private InventoryTransactionService service;

    private Item item;
    private Employee employee;
    private User performedBy;

    @BeforeEach
    void setUp() {
        ItemCategory category = new ItemCategory();
        category.setId(1L);
        category.setName("INSERTS");

        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setId(1L);
        uom.setCode("PCS");

        item = new Item();
        item.setId(10L);
        item.setItemCode("XYZ-INSERT");
        item.setName("XYZ Insert");
        item.setCategory(category);
        item.setUom(uom);
        item.setSafeStock(new BigDecimal("10"));
        item.setCurrentUnitCost(new BigDecimal("1032.00"));
        item.setActive(true);

        employee = new Employee();
        employee.setId(5L);
        employee.setName("Rahul");
        employee.setActive(true);

        Role role = new Role();
        role.setId(1L);
        role.setName("ISSUER");

        performedBy = new User();
        performedBy.setId(2L);
        performedBy.setUsername("issuer1");
        performedBy.setRole(role);

        lenient().when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        lenient().when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(performedBy));
        lenient().when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(inv -> {
                    InventoryTransaction t = inv.getArgument(0);
                    if (t.getId() == null) {
                        t.setId((long) (Math.random() * 100000));
                    }
                    return t;
                });
    }

    @Test
    void issue_reducesAvailableStock_andOpensAssignment() {
        when(transactionRepository.currentStock(10L)).thenReturn(new BigDecimal("16"));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(assignmentRepository.save(any(StockAssignment.class))).thenAnswer(inv -> {
            StockAssignment a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        InventoryDtos.IssueRequest req = new InventoryDtos.IssueRequest(10L, new BigDecimal("5"), 5L, null, "Production", null);
        InventoryDtos.IssueResponse response = service.issue(req, 2L);

        assertThat(response.remainingStock()).isEqualByComparingTo("11");
        assertThat(response.transaction().quantity()).isEqualByComparingTo("-5");
        assertThat(response.transaction().txnType()).isEqualTo("ISSUE_OUTWARD");
        assertThat(response.assignment().assignedQty()).isEqualByComparingTo("5");
        assertThat(response.assignment().status()).isEqualTo("ASSIGNED");
    }

    @Test
    void issue_rejectsWhenQuantityExceedsAvailableStock() {
        when(transactionRepository.currentStock(10L)).thenReturn(new BigDecimal("16"));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));

        InventoryDtos.IssueRequest req = new InventoryDtos.IssueRequest(10L, new BigDecimal("20"), 5L, null, "Production", null);

        assertThatThrownBy(() -> service.issue(req, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only 16");
    }

    @Test
    void issue_rejectsWhenItemIsInactive() {
        item.setActive(false);
        InventoryDtos.IssueRequest req = new InventoryDtos.IssueRequest(10L, new BigDecimal("1"), 5L, null, null, null);

        assertThatThrownBy(() -> service.issue(req, 2L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void issue_rejectsWhenEmployeeIsInactive() {
        employee.setActive(false);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));

        InventoryDtos.IssueRequest req = new InventoryDtos.IssueRequest(10L, new BigDecimal("1"), 5L, null, null, null);

        assertThatThrownBy(() -> service.issue(req, 2L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void returnStock_good_addsBackToStock_andClosesAssignmentWhenFullyReturned() {
        StockAssignment assignment = openAssignment(new BigDecimal("5"), BigDecimal.ZERO);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        InventoryDtos.ReturnRequest req = new InventoryDtos.ReturnRequest(100L, new BigDecimal("5"), "GOOD", "Unused");
        InventoryDtos.ReturnResponse response = service.returnStock(req, 2L);

        assertThat(response.transactions()).hasSize(1);
        assertThat(response.transactions().get(0).quantity()).isEqualByComparingTo("5");
        assertThat(response.transactions().get(0).txnType()).isEqualTo("RETURN_FROM_USER");
        assertThat(response.assignment().status()).isEqualTo("CLOSED");
        assertThat(response.assignment().remainingQty()).isEqualByComparingTo("0");
    }

    @Test
    void returnStock_partial_leavesAssignmentPartiallyReturned() {
        StockAssignment assignment = openAssignment(new BigDecimal("5"), BigDecimal.ZERO);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        InventoryDtos.ReturnRequest req = new InventoryDtos.ReturnRequest(100L, new BigDecimal("2"), "GOOD", null);
        InventoryDtos.ReturnResponse response = service.returnStock(req, 2L);

        assertThat(response.assignment().status()).isEqualTo("PARTIALLY_RETURNED");
        assertThat(response.assignment().remainingQty()).isEqualByComparingTo("3");
    }

    @Test
    void returnStock_damaged_createsOffsettingWriteOff_withZeroNetStockEffect() {
        StockAssignment assignment = openAssignment(new BigDecimal("5"), BigDecimal.ZERO);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        InventoryDtos.ReturnRequest req = new InventoryDtos.ReturnRequest(100L, new BigDecimal("2"), "DAMAGED", "Dropped");
        InventoryDtos.ReturnResponse response = service.returnStock(req, 2L);

        assertThat(response.transactions()).hasSize(2);
        BigDecimal netQuantity = response.transactions().stream()
                .map(t -> t.quantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(netQuantity).isEqualByComparingTo("0");
        assertThat(response.transactions().get(1).txnType()).isEqualTo("DAMAGE");
    }

    @Test
    void returnStock_rejectsWhenQuantityExceedsAssignedRemaining() {
        StockAssignment assignment = openAssignment(new BigDecimal("5"), new BigDecimal("3"));
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        InventoryDtos.ReturnRequest req = new InventoryDtos.ReturnRequest(100L, new BigDecimal("3"), "GOOD", null);

        assertThatThrownBy(() -> service.returnStock(req, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only 2");
    }

    @Test
    void returnStock_rejectsWhenAssignmentAlreadyClosed() {
        StockAssignment assignment = openAssignment(new BigDecimal("5"), new BigDecimal("5"));
        assignment.setStatus(AssignmentStatus.CLOSED);
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));

        InventoryDtos.ReturnRequest req = new InventoryDtos.ReturnRequest(100L, new BigDecimal("1"), "GOOD", null);

        assertThatThrownBy(() -> service.returnStock(req, 2L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void purchaseInward_computesWeightedAverageCost() {
        // 16 units on hand at 1032, buying 4 more at 1200
        // (16*1032 + 4*1200) / 20 = (16512 + 4800) / 20 = 1065.60
        when(transactionRepository.currentStock(10L)).thenReturn(new BigDecimal("16"));

        InventoryDtos.PurchaseInwardRequest req = new InventoryDtos.PurchaseInwardRequest(10L, new BigDecimal("4"), new BigDecimal("1200.00"), "GRN-1");
        InventoryDtos.InwardResponse response = service.purchaseInward(req, 2L);

        assertThat(response.newAverageUnitCost()).isEqualByComparingTo("1065.60");
        assertThat(response.transaction().unitCostAtTxn()).isEqualByComparingTo("1200.00");
        assertThat(item.getCurrentUnitCost()).isEqualByComparingTo("1065.60");
    }

    @Test
    void purchaseInward_pastConsumptionRetainsItsOriginalUnitCost() {
        // A later price change on the master must never rewrite an
        // already-posted transaction's unit_cost_at_txn (Phase 1 doc G.3).
        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        when(transactionRepository.currentStock(10L)).thenReturn(new BigDecimal("0"));

        InventoryDtos.PurchaseInwardRequest req = new InventoryDtos.PurchaseInwardRequest(10L, new BigDecimal("10"), new BigDecimal("900.00"), null);
        service.purchaseInward(req, 2L);

        // Master price now moves to 1032 via a second purchase...
        when(transactionRepository.currentStock(10L)).thenReturn(new BigDecimal("10"));
        InventoryDtos.PurchaseInwardRequest req2 = new InventoryDtos.PurchaseInwardRequest(10L, new BigDecimal("10"), new BigDecimal("1032.00"), null);
        service.purchaseInward(req2, 2L);

        // ...but the FIRST transaction's own stored cost must still read 900,
        // not silently pick up the later price. Both calls captured in order.
        verify(transactionRepository, times(2)).save(captor.capture());
        List<InventoryTransaction> saved = captor.getAllValues();
        assertThat(saved.get(0).getUnitCostAtTxn()).isEqualByComparingTo("900.00");
        assertThat(saved.get(1).getUnitCostAtTxn()).isEqualByComparingTo("1032.00");
    }

    @Test
    void openingBalance_rejectsWhenItemAlreadyHasHistory() {
        when(transactionRepository.existsByItem_Id(10L)).thenReturn(true);

        InventoryDtos.OpeningBalanceRequest req = new InventoryDtos.OpeningBalanceRequest(10L, new BigDecimal("16"), new BigDecimal("1032"), null);

        assertThatThrownBy(() -> service.openingBalance(req, 2L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reverse_rejectsDoubleReversal() {
        InventoryTransaction original = new InventoryTransaction();
        original.setId(500L);
        original.setTxnNo("TXN500");
        original.setTxnType(TransactionType.ISSUE_OUTWARD);
        original.setQuantity(new BigDecimal("-5"));
        original.setUnitCostAtTxn(new BigDecimal("1032"));
        original.setItem(item);

        when(transactionRepository.findById(500L)).thenReturn(Optional.of(original));
        when(transactionRepository.existsByReversalOfTxn_Id(500L)).thenReturn(true);

        InventoryDtos.ReversalRequest req = new InventoryDtos.ReversalRequest(500L, "Data entry error");

        assertThatThrownBy(() -> service.reverse(req, 2L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reverse_createsOppositeSignedTransaction() {
        InventoryTransaction original = new InventoryTransaction();
        original.setId(500L);
        original.setTxnNo("TXN500");
        original.setTxnType(TransactionType.ISSUE_OUTWARD);
        original.setQuantity(new BigDecimal("-5"));
        original.setUnitCostAtTxn(new BigDecimal("1032"));
        original.setItem(item);

        when(transactionRepository.findById(500L)).thenReturn(Optional.of(original));
        when(transactionRepository.existsByReversalOfTxn_Id(500L)).thenReturn(false);
        when(transactionRepository.currentStock(10L)).thenReturn(new BigDecimal("11"));

        InventoryDtos.ReversalRequest req = new InventoryDtos.ReversalRequest(500L, "Data entry error");
        InventoryDtos.StockMutationResponse response = service.reverse(req, 2L);

        assertThat(response.transaction().quantity()).isEqualByComparingTo("5");
        assertThat(response.transaction().txnType()).isEqualTo("REVERSAL");
    }

    private StockAssignment openAssignment(BigDecimal assignedQty, BigDecimal returnedQty) {
        StockAssignment assignment = new StockAssignment();
        assignment.setId(100L);
        assignment.setItem(item);
        assignment.setEmployee(employee);
        assignment.setAssignedQty(assignedQty);
        assignment.setReturnedQty(returnedQty);
        assignment.setStatus(returnedQty.signum() == 0 ? AssignmentStatus.ASSIGNED : AssignmentStatus.PARTIALLY_RETURNED);
        return assignment;
    }
}
