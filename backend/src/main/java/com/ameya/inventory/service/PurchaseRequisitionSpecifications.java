package com.ameya.inventory.service;

import com.ameya.inventory.entity.PrPriority;
import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.entity.PurchaseRequisition;
import org.springframework.data.jpa.domain.Specification;

final class PurchaseRequisitionSpecifications {

    private PurchaseRequisitionSpecifications() {
    }

    static Specification<PurchaseRequisition> status(PrStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<PurchaseRequisition> priority(PrPriority priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    static Specification<PurchaseRequisition> departmentId(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }

    @SafeVarargs
    static Specification<PurchaseRequisition> and(Specification<PurchaseRequisition>... specs) {
        Specification<PurchaseRequisition> result = Specification.where(null);
        for (Specification<PurchaseRequisition> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
