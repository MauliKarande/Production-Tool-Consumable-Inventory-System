# Production Tool & Consumable Inventory System

A production-ready inventory management system for Ameya Precision Engineers' Production Department, replacing the existing Excel-based tracking of tools and consumables (inserts, drills, taps, cutting tools, oils).

## Status

**Phases 1, 2, 3, 4, and 6 are complete**, plus a working React frontend covering everything built so far. See [`docs/phase1-requirements.md`](docs/phase1-requirements.md) for the full requirements/architecture writeup.

Phase 3 (backend foundation), fully working end to end (entity → repository → service → DTO → controller):
- JWT authentication (`/api/auth/login`), role-based authorization (ADMIN / ISSUER / VIEWER)
- User management, with a self-bootstrapping first Admin account (see below — no password is ever committed to the repo)
- Masters: Departments, Employees, Units of Measure, Manufacturers, Suppliers, Machines, Item Categories (with admin-configurable per-category attributes), Items
- Global exception handling with clean, user-facing error messages
- Audit fields (created/updated by + timestamp) on every master record

Phase 4 (inventory transaction engine) — the core of the system, verified live against MySQL:
- **Immutable ledger** (`inventory_transactions`) — `current_stock` is always the signed sum of a item's rows, never stored redundantly
- `POST /api/inventory/opening-balance` — seed an item's starting stock (admin, once per item)
- `POST /api/inventory/issue` — issue to an employee/machine; rejects if quantity exceeds available stock; opens a `stock_assignment`
- `POST /api/inventory/return` — return against a specific assignment; **GOOD** stock returns to inventory, **DAMAGED/SCRAP** returns post two offsetting ledger entries (return-in, then write-off) so stock correctly stays where it was, not silently disappearing or double-counting
- `POST /api/inventory/inward` — purchase inward with automatic **weighted-average cost** recalculation; each transaction still keeps its own `unit_cost_at_txn`, so a later price change never rewrites historical consumption value
- `POST /api/inventory/adjustment`, `/damage-scrap`, `/reversal` — admin-only corrections, reversal never edits a posted row, it posts an offsetting one
- `GET /api/items/{id}/transactions`, `/stock-summary`, `/current-stock`, `GET /api/accountability` — full lifecycle + "who currently has what" views
- **Concurrency-safe**: every stock-mutating call takes a pessimistic write lock on the item row for the duration of its transaction, so two simultaneous issues of the same item can't both succeed past available stock

Phase 6 (CNC-wise consumption reporting) — all figures derived live from the ledger, nothing precomputed:
- `GET /api/consumption/machine/{id}?from=&to=` — item-wise breakdown for one machine
- `GET /api/consumption/machines?from=&to=` — cross-machine comparison, ranked by value ("which CNC consumes the most")
- `GET /api/consumption/category?from=&to=&machineId=` — category-wise breakdown, optionally scoped to one machine
- `GET /api/consumption/top-items?from=&to=&by=value|quantity&limit=` — top-N consumed items

**Frontend** (React + TypeScript + Vite + MUI, in `frontend/`): login, dashboard, item master (list/detail/create/edit with full transaction history and stock summary), issue/return workflow with live stock checks, accountability view, the consumption report above (with drill-down), and an admin Masters management screen. The centerpiece is a reusable **creatable dropdown**: every master-data select (Category, Manufacturer, Supplier, UOM, Employee, Machine, Department) lets Admin type a new value and add it inline via a small dialog, without leaving the form — verified live end-to-end (typed a new category → created via API → auto-selected → item saved).

**Not yet built**: alerts, purchase requisition workflow, KPI dashboard/charts, Excel migration.

## Stack

- **Backend:** Java 17, Spring Boot 3.3, Spring MVC/REST, Spring Data JPA (Hibernate), Spring Security (JWT), MySQL 8, Flyway, Maven
- **Frontend:** React 19 + TypeScript, Vite, MUI, React Router, Axios

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
   export SERVER_PORT=7000   # optional, defaults to 8080
   ```

3. **Run.**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Flyway runs the schema migrations automatically on startup (`V1__init_schema.sql`, `V2__seed_reference_data.sql`).

4. **First login.** On an empty `users` table, the app creates one Admin account automatically and prints its password **once**, to the console log, on that first startup only. Log in and change it immediately (`POST /api/users/me/change-password`), or set `APP_BOOTSTRAP_ADMIN_PASSWORD` before the very first startup to choose it yourself instead of getting a random one.

5. **API docs.** Swagger UI is at `/swagger-ui.html` once the app is running; the bare root `/` returns a small status JSON.

**Note:** JWT tokens expire after 8 hours (`app.jwt.expiration-minutes`). If the frontend suddenly starts getting 403s on every request after being idle overnight, that's an expired token, not a bug — log out and back in.

## Running the frontend

```bash
cd frontend
npm install   # first time only
npm run dev
```
Runs on `http://localhost:5173` by default and expects the backend at `http://localhost:7000` (see `frontend/.env.development`). The backend's CORS allowlist is pinned to `http://localhost:5173` — if that port is already taken by a stray process, Vite will pick 5174 instead and every API call will fail with a CORS "Network Error". Free port 5173 first rather than reconfiguring CORS.

### Verified so far

`mvn package` builds a runnable jar and all unit tests pass (18 tests: master CRUD + the full inventory transaction engine — issue/return/inward/adjustment/reversal, insufficient-stock rejection, weighted-average cost, damaged-return net-zero effect, double-reversal rejection). `npm run build` type-checks and builds the frontend cleanly.

Both have been run live end-to-end together against a real MySQL instance on this machine, through the actual browser UI (not just curl): the exact worked example from the Phase 1 doc (opening 16 → issue 5 → return 2 → return 3 → back to 16), over-issue/over-return rejection, a damaged-return correctly holding stock steady instead of bouncing back, item creation via the creatable-dropdown flow, and the CNC-wise consumption report with drill-down (verified the machine comparison, category breakdown, and per-machine item detail all reconcile to the same totals).

## Roadmap

1. ~~Requirements & architecture~~
2. ~~Database schema (DDL)~~
3. ~~Backend foundation (auth, users, masters, item management)~~
4. ~~Inventory transaction engine (ledger, stock formula, concurrency, valuation)~~
5. Accountability reporting (currently-assigned-tools views beyond the basic list already built)
6. ~~CNC-wise consumption~~
7. Alerts
8. Purchasing / requisition
9. Reports & dashboard (KPIs, charts)
10. Excel migration
11. Testing & production hardening
12. ~~React frontend~~ (core screens done; grows alongside remaining backend phases)
