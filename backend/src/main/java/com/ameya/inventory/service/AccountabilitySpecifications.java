package com.ameya.inventory.service;

import com.ameya.inventory.entity.AssignmentStatus;
import com.ameya.inventory.entity.StockAssignment;
import org.springframework.data.jpa.domain.Specification;

final class AccountabilitySpecifications {

    private AccountabilitySpecifications() {
    }

    static Specification<StockAssignment> employeeId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId);
    }

    static Specification<StockAssignment> machineId(Long machineId) {
        if (machineId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("machine").get("id"), machineId);
    }

    static Specification<StockAssignment> itemId(Long itemId) {
        if (itemId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("item").get("id"), itemId);
    }

    static Specification<StockAssignment> openOnly(boolean openOnly) {
        if (!openOnly) {
            return null;
        }
        return (root, query, cb) -> cb.notEqual(root.get("status"), AssignmentStatus.CLOSED);
    }

    @SafeVarargs
    static Specification<StockAssignment> and(Specification<StockAssignment>... specs) {
        Specification<StockAssignment> result = Specification.where(null);
        for (Specification<StockAssignment> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
