# Production Tool & Consumable Inventory System

A production-ready inventory management system for Ameya Precision Engineers' Production Department, replacing the existing Excel-based tracking of tools and consumables (inserts, drills, taps, cutting tools, oils).

## Status

**Phase 1 — Requirements & Architecture** (complete, pending sign-off).

See [`docs/phase1-requirements.md`](docs/phase1-requirements.md) for:
- Analysis of the existing Excel process
- Recommended workflow, module list, and role permission matrix
- Database ER design
- Excel → database column mapping
- Inventory transaction rules and valuation method
- Reporting design
- Recommended improvements and open business decisions

## Stack (planned)

- **Backend:** Java, Spring Boot, Spring MVC/REST, Spring Data JPA, Spring Security, MySQL, Maven
- **Frontend:** React, Bootstrap/Material UI

## Roadmap

1. ~~Requirements & architecture~~
2. Database schema (DDL)
3. Backend foundation (auth, users, masters)
4. Inventory transaction engine
5. Accountability module
6. CNC-wise consumption
7. Alerts
8. Purchasing / requisition
9. Reports & dashboard
10. Excel migration
11. Testing & production hardening
