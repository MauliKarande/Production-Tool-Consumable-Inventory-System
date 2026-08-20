package com.ameya.inventory.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Machine-readable liveness/identity endpoint. The bare "/" URL is the
 * web UI (static index.html), so this lives under /api instead.
 */
@RestController
public class StatusController {

    @GetMapping("/api/status")
    public Map<String, String> status() {
        return Map.of(
                "service", "Ameya Production Tool & Consumable Inventory API",
                "status", "UP",
                "docs", "/swagger-ui.html"
        );
    }
}
