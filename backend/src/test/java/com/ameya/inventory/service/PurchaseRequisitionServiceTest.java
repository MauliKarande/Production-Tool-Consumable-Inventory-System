package com.ameya.inventory.service;

import com.ameya.inventory.dto.purchase.PurchaseRequisitionDtos;
import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.entity.PurchaseRequisition;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.repository.DepartmentRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.PurchaseRequisitionRepository;
import com.ameya.inventory.repository.SupplierRepository;
import com.ameya.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers the approval-gate rule (J.4: requester cannot approve their own
 * requisition, enforced regardless of role) and the status-transition
 * guards, since those are the two rules a live UI test wouldn't
 * necessarily exercise in every combination.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseRequisitionServiceTest {

    @Mock private PurchaseRequisitionRepository prRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private InventoryTransactionService inventoryTransactionService;

    @InjectMocks
    private PurchaseRequisitionService service;

    private PurchaseRequisition pr;
    private User requester;
    private User otherAdmin;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setUsername("issuer1");

        otherAdmin = new User();
        otherAdmin.setId(2L);
        otherAdmin.setUsername("admin2");

        pr = new PurchaseRequisition();
        pr.setId(100L);
        pr.setPrNo("PR0001");
        pr.setRequestedBy(requester);
        pr.setStatus(PrStatus.SUBMITTED);
    }

    @Test
    void approve_throwsBusinessRuleException_whenApproverIsTheRequester() {
        when(prRepository.findById(100L)).thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> service.approve(100L, requester.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot approve their own requisition");
    }

    @Test
    void approve_succeeds_whenApproverIsADifferentUser() {
        when(prRepository.findById(100L)).thenReturn(Optional.of(pr));
        when(userRepository.findById(otherAdmin.getId())).thenReturn(Optional.of(otherAdmin));
        when(prRepository.save(any(PurchaseRequisition.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequisitionDtos.Response response = service.approve(100L, otherAdmin.getId());

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.approvedByUsername()).isEqualTo("admin2");
    }

    @Test
    void approve_throwsBusinessRuleException_whenNotInSubmittedStatus() {
        pr.setStatus(PrStatus.DRAFT);
        when(prRepository.findById(100L)).thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> service.approve(100L, otherAdmin.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void reject_throwsBusinessRuleException_whenRejecterIsTheRequester() {
        when(prRepository.findById(100L)).thenReturn(Optional.of(pr));
        var request = new PurchaseRequisitionDtos.RejectRequest("Not needed anymore");

        assertThatThrownBy(() -> service.reject(100L, request, requester.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot reject their own requisition");
    }

    @Test
    void markOrdered_throwsBusinessRuleException_whenNotApproved() {
        when(prRepository.findById(100L)).thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> service.markOrdered(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SUBMITTED");
    }
}
