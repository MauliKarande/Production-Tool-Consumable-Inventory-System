package com.ameya.inventory.dto.alert;

import java.time.Instant;

public class AlertDtos {

    public record Response(
            Long id,
            String type,
            Long itemId,
            String itemCode,
            String itemName,
            String message,
            String status,
            Instant raisedAt,
            String acknowledgedByUsername,
            Instant acknowledgedAt,
            String resolvedByUsername,
            Instant resolvedAt
    ) {
    }

    public record RecomputeResult(int raised, int resolved) {
    }
}
