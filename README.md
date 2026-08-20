# Production Tool & Consumable Inventory System

A production-ready inventory management system for Ameya Precision Engineers' Production Department, replacing the existing Excel-based tracking of tools and consumables (inserts, drills, taps, cutting tools, oils).

## Status

**All 11 phases are complete**, including the legacy Excel data migration — see below. See [`docs/phase1-requirements.md`](docs/phase1-requirements.md) for the full requirements/architecture writeup.

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

Phase 7 (alerts) — a scheduled job (every 15 min, plus an admin-triggered recompute) keeps six alert types in sync with the ledger, idempotently:
- `LOW_STOCK` / `OUT_OF_STOCK`, `PENDING_RETURN` (assignment open past a configurable threshold), `HIGH_CONSUMPTION` / `UNUSUAL_CONSUMPTION` (vs. trailing 3-month average), `PURCHASE_PENDING` (below safe stock with no open PR covering it)
- `GET /api/alerts`, `POST /api/alerts/{id}/acknowledge`, `/resolve`, `/recompute`

Phase 8 (purchase requisitions) — full `DRAFT → SUBMITTED → APPROVED/REJECTED → ORDERED → RECEIVED → CLOSED` lifecycle:
- The approval gate is enforced in the service layer, not just by role: the approver must be a different user than the requester, even if both are Admin (verified live: same-user self-approval is rejected, a second Admin succeeds)
- `POST /api/purchase-requisitions/{id}/receive` reuses the Phase 4 ledger's `purchaseInward` — weighted-average cost and locking stay uniform — and supports a per-line **direct-to-floor** flag that immediately posts a linked `ISSUE_OUTWARD`, formalizing the real pattern found in the legacy Purchase file

Phase 9 (reports & dashboard) — every report is a query over the ledger, never a maintained table: Stock Valuation, Low/Out-of-Stock, Dead Stock (no consumption in N months), Supplier Price Comparison, Supplier Spend, Purchase Pipeline, each exportable as formatted `.xlsx` (Apache POI) or `.pdf` (PDFBox). The dashboard shows live KPI cards plus machine/category consumption charts (`@mui/x-charts`).

Phase 10 (Excel migration) — `POST /api/import/preview` (ADMIN only) runs the real import inside a transaction and rolls it back, so preview and commit are guaranteed identical; `POST /api/import/commit` runs the same code and keeps it. Three parsers, one per legacy file:
- **Consumption sheet** (INSERT/DRILLS/TAPS tabs): locates columns by header text at a fixed offset from "INSERT No." rather than hardcoded indices, since the three tabs' layouts differ; posts one `OPENING_BALANCE` per item (J.1) and one `ISSUE_OUTWARD` per non-empty daily-grid cell, `employee`/`machine` left `NULL` (never invented)
- **Oil/consumables sheet**: machine-wise oil consumption (the only legacy data with real machine attribution) plus a general-consumables table; oils have no opening-balance figure in the source at all, so consumption is posted via a dedicated unchecked ledger path rather than inventing one, with the resulting negative stock surfaced as an import warning, not hidden
- **Purchase requisitions**: imports from `Sheet1` (the consolidated log) directly rather than fuzzy-matching the 17 individual slip sheets back to it — `Sheet1` already carries every field a slip has, so no ambiguous-match guessing is needed
- All three were run against the real files: 300 items / 17 manufacturers / 474 transactions (consumption), 14 items / 4 machines / 250 transactions (oil/consumables), 8 suppliers / 49 items / 14 requisitions / 102 transactions (purchasing) — cross-checked against the database and live in the UI (category-wise June consumption report reconciles exactly to the ledger)

Phase 11 (testing & hardening) — 26 unit tests (Mockito + AssertJ: masters CRUD, the full inventory engine, the PR approval-gate and status-transition guards, the alert upsert/auto-resolve idempotency), `mvn package` and `npm run build` both clean, `@PreAuthorize` audited against the §D permission matrix on every new endpoint, unhandled-exception logging added to the global handler (previously silent 500s).

**Frontend** (React + TypeScript + Vite + MUI, in `frontend/`): login, dashboard with KPIs and charts, item master (list/detail/create/edit with full transaction history and stock summary), issue/return workflow with live stock checks, accountability view, the consumption report (with drill-down), purchase requisitions (create/approve/receive), reports (with Excel/PDF export), alerts, an admin Excel-import screen, and an admin Masters management screen. The centerpiece is a reusable **creatable dropdown**: every master-data select (Category, Manufacturer, Supplier, UOM, Employee, Machine, Department) lets Admin type a new value and add it inline via a small dialog, without leaving the form — verified live end-to-end (typed a new category → created via API → auto-selected → item saved).

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

`mvn package` builds a runnable jar and all 26 unit tests pass (master CRUD; the full inventory transaction engine — issue/return/inward/adjustment/reversal, insufficient-stock rejection, weighted-average cost, damaged-return net-zero effect, double-reversal rejection; the PR approval-gate and status-transition guards; the alert upsert/auto-resolve idempotency). `npm run build` type-checks and builds the frontend cleanly.

Everything has been run live end-to-end against a real MySQL instance on this machine, through the actual browser UI (not just curl): the exact worked example from the Phase 1 doc (opening 16 → issue 5 → return 2 → return 3 → back to 16), over-issue/over-return rejection, a damaged-return correctly holding stock steady instead of bouncing back, item creation via the creatable-dropdown flow, the CNC-wise consumption report with drill-down, the full PR lifecycle including a same-user self-approval rejection followed by a successful cross-user approval, dashboard KPIs/charts, report Excel/PDF export, and — the big one — all three legacy Excel files imported for real and cross-checked against the database and the UI (see Phase 10 above).

**A note on the imported data**: the database this was verified against now contains the real migrated data from all three legacy files, not just seed/demo data. A fresh environment starting from an empty `ameya_inventory` schema will only get the V1/V2 seed data (masters, roles) — re-running the migration means going to **Import Data** (Admin) and running Preview → Confirm for each of the three files in turn.

## Roadmap

1. ~~Requirements & architecture~~
2. ~~Database schema (DDL)~~
3. ~~Backend foundation (auth, users, masters, item management)~~
4. ~~Inventory transaction engine (ledger, stock formula, concurrency, valuation)~~
5. ~~Accountability reporting~~
6. ~~CNC-wise consumption~~
7. ~~Alerts~~
8. ~~Purchasing / requisition~~
9. ~~Reports & dashboard (KPIs, charts)~~
10. ~~Excel migration~~
11. ~~Testing & production hardening~~
12. ~~React frontend~~
