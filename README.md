# Production Tool & Consumable Inventory System

A production-ready inventory management system for Ameya Precision Engineers' Production Department, replacing the existing Excel-based tracking of tools and consumables (inserts, drills, taps, cutting tools, oils).

## Status

**Phase 1** (requirements/architecture) and **Phase 2 + 3** (database schema, backend foundation) are complete. See [`docs/phase1-requirements.md`](docs/phase1-requirements.md) for the full requirements/architecture writeup.

Phase 3 delivers, fully working end to end (entity → repository → service → DTO → controller):
- JWT authentication (`/api/auth/login`), role-based authorization (ADMIN / ISSUER / VIEWER)
- User management, with a self-bootstrapping first Admin account (see below — no password is ever committed to the repo)
- Masters: Departments, Employees, Units of Measure, Manufacturers, Suppliers, Machines, Item Categories (with admin-configurable per-category attributes), Items
- Global exception handling with clean, user-facing error messages
- Audit fields (created/updated by + timestamp) on every master record

**Not yet built** (later phases): the inventory transaction ledger, accountability, CNC consumption, alerts, purchasing, reports/dashboard, Excel migration, and the React frontend. `items.current_unit_cost` and `safe_stock` exist on the Item master now, but there is no `current_stock` yet — that's computed from the ledger in Phase 4, not stored directly.

## Stack

- **Backend:** Java 17, Spring Boot 3.3, Spring MVC/REST, Spring Data JPA (Hibernate), Spring Security (JWT), MySQL 8, Flyway, Maven
- **Frontend:** React (not yet started)

## Running the backend

1. **Database.** You need a MySQL 8 instance and an empty schema:
   ```sql
   CREATE DATABASE ameya_inventory CHARACTER SET utf8mb4;
   ```
   This machine already has MySQL services installed (`MySQL80`, and a separate `Mysql For EPS 7.60` used by another application) — point at whichever instance you intend for this project, and use a **new, dedicated database name** (`ameya_inventory`) so it never collides with the EPS system's data. Do not reuse the EPS credentials/schema.

2. **Configure connection.** Either edit `backend/src/main/resources/application.yml` or set environment variables before starting:
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/ameya_inventory?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true"
   export DB_USERNAME=root
   export DB_PASSWORD=your_password
   export JWT_SECRET="a-long-random-string-at-least-32-bytes"
   ```

3. **Run.**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Flyway runs the schema migrations automatically on startup (`V1__init_schema.sql`, `V2__seed_reference_data.sql`).

4. **First login.** On an empty `users` table, the app creates one Admin account automatically and prints its password **once**, to the console log, on that first startup only:
   ```
   No ADMIN user existed - created one automatically:
     username: admin
     password: <random>
   ```
   Log in and change it immediately (`POST /api/users/me/change-password`), or set `APP_BOOTSTRAP_ADMIN_PASSWORD` before the very first startup to choose it yourself instead of getting a random one.

5. **API docs.** Swagger UI is at `http://localhost:8080/swagger-ui.html` once the app is running.

### Verified so far

`mvn package` builds a runnable jar and all unit tests pass in this environment (Java 17, Maven 3.9). A full boot-and-migrate smoke test against a live MySQL instance has **not** been run yet, since doing so meant either touching the existing EPS database service without credentials or asking first — flagged here rather than guessed. Run step 3 above against your own dedicated schema to confirm the migrations apply cleanly end to end.

## Roadmap

1. ~~Requirements & architecture~~
2. ~~Database schema (DDL)~~
3. ~~Backend foundation (auth, users, masters, item management)~~
4. Inventory transaction engine (ledger, stock formula, concurrency, valuation)
5. Accountability module
6. CNC-wise consumption
7. Alerts
8. Purchasing / requisition
9. Reports & dashboard
10. Excel migration
11. Testing & production hardening
12. React frontend
