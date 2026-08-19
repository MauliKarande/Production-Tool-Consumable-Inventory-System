-- =====================================================================
-- Ameya Precision Engineers - Production Tool & Consumable Inventory
-- V1: Initial schema
--
-- Conventions:
--   * All monetary columns use DECIMAL, never FLOAT/DOUBLE.
--   * All quantity columns use DECIMAL(14,3) because oil/coolant is
--     tracked in fractional litres in the source data (e.g. 6.667 L) -
--     an INTEGER quantity column would silently truncate that.
--   * "Enum-like" state/type columns are VARCHAR with a CHECK constraint
--     rather than MySQL native ENUM, so adding a new value later is a
--     constraint change, not a column-type rewrite.
--   * Every mutable master table carries created_at/updated_at/
--     created_by/updated_by for auditability without a full audit_logs
--     lookup.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Security: roles, permissions, users
-- ---------------------------------------------------------------------

CREATE TABLE roles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permissions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    CONSTRAINT uq_permissions_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE departments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE employees (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code   VARCHAR(50)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    department_id   BIGINT,
    designation     VARCHAR(100),
    contact         VARCHAR(50),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_employees_code UNIQUE (employee_code),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_employees_department ON employees(department_id);

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    employee_id     BIGINT,
    role_id         BIGINT       NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_users_employee ON users(employee_id);

-- ---------------------------------------------------------------------
-- Reference masters
-- ---------------------------------------------------------------------

CREATE TABLE units_of_measure (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_uom_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE manufacturers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_manufacturers_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE suppliers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    contact_person  VARCHAR(150),
    phone           VARCHAR(50),
    email           VARCHAR(150),
    address         VARCHAR(500),
    gst_number      VARCHAR(30),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_suppliers_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE machines (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_code        VARCHAR(50)  NOT NULL,
    machine_name        VARCHAR(150) NOT NULL,
    machine_type        VARCHAR(50),
    department_id       BIGINT,
    location            VARCHAR(150),
    manufacturer        VARCHAR(150),
    model               VARCHAR(150),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    installation_date   DATE,
    remarks             VARCHAR(500),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT uq_machines_code UNIQUE (machine_code),
    CONSTRAINT fk_machines_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT chk_machines_status CHECK (status IN ('ACTIVE','MAINTENANCE','INACTIVE','RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_machines_department ON machines(department_id);

-- ---------------------------------------------------------------------
-- Item master: categories (admin-configurable, not hard-coded),
-- category-specific attribute definitions (EAV), items, attribute values
-- ---------------------------------------------------------------------

CREATE TABLE item_categories (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    parent_category_id  BIGINT,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT uq_item_categories_name UNIQUE (name),
    CONSTRAINT fk_item_categories_parent FOREIGN KEY (parent_category_id) REFERENCES item_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_category_attribute_defs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     BIGINT       NOT NULL,
    attribute_name  VARCHAR(100) NOT NULL,
    data_type       VARCHAR(20)  NOT NULL,
    is_required     BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order   INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_category_attr_name UNIQUE (category_id, attribute_name),
    CONSTRAINT fk_category_attr_category FOREIGN KEY (category_id) REFERENCES item_categories(id),
    CONSTRAINT chk_category_attr_type CHECK (data_type IN ('STRING','NUMBER','BOOLEAN','DATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE items (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_code               VARCHAR(50)   NOT NULL,
    name                    VARCHAR(255)  NOT NULL,
    description             VARCHAR(1000),
    category_id             BIGINT        NOT NULL,
    manufacturer_id         BIGINT,
    preferred_supplier_id   BIGINT,
    uom_id                  BIGINT        NOT NULL,
    part_number             VARCHAR(100),
    specification           VARCHAR(500),
    safe_stock              DECIMAL(14,3) NOT NULL DEFAULT 0,
    max_stock               DECIMAL(14,3),
    current_unit_cost       DECIMAL(14,2) NOT NULL DEFAULT 0,
    currency                VARCHAR(10)   NOT NULL DEFAULT 'INR',
    legacy_description      VARCHAR(500),
    barcode_value           VARCHAR(100),
    active                  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    CONSTRAINT uq_items_code UNIQUE (item_code),
    CONSTRAINT uq_items_barcode UNIQUE (barcode_value),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES item_categories(id),
    CONSTRAINT fk_items_manufacturer FOREIGN KEY (manufacturer_id) REFERENCES manufacturers(id),
    CONSTRAINT fk_items_supplier FOREIGN KEY (preferred_supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_items_uom FOREIGN KEY (uom_id) REFERENCES units_of_measure(id),
    CONSTRAINT chk_items_safe_stock CHECK (safe_stock >= 0),
    CONSTRAINT chk_items_unit_cost CHECK (current_unit_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_items_category ON items(category_id);
CREATE INDEX idx_items_manufacturer ON items(manufacturer_id);
CREATE INDEX idx_items_active ON items(active);
CREATE FULLTEXT INDEX ftx_items_name_desc ON items(name, description);

CREATE TABLE item_attribute_values (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id         BIGINT       NOT NULL,
    attribute_def_id BIGINT      NOT NULL,
    value           VARCHAR(500),
    CONSTRAINT uq_item_attr_value UNIQUE (item_id, attribute_def_id),
    CONSTRAINT fk_item_attr_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_attr_def FOREIGN KEY (attribute_def_id) REFERENCES item_category_attribute_defs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- Month-end closing
-- ---------------------------------------------------------------------

CREATE TABLE accounting_periods (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    period           VARCHAR(7)   NOT NULL COMMENT 'YYYY-MM',
    status           VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    closed_by_user_id BIGINT,
    closed_at        TIMESTAMP    NULL,
    CONSTRAINT uq_accounting_periods_period UNIQUE (period),
    CONSTRAINT fk_accounting_periods_closed_by FOREIGN KEY (closed_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_accounting_periods_status CHECK (status IN ('OPEN','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- Inventory ledger (immutable) and derived accountability view
-- ---------------------------------------------------------------------

CREATE TABLE inventory_transactions (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_no                  VARCHAR(50)   NOT NULL,
    item_id                 BIGINT        NOT NULL,
    txn_type                VARCHAR(30)   NOT NULL,
    quantity                DECIMAL(14,3) NOT NULL,
    unit_cost_at_txn        DECIMAL(14,2) NOT NULL,
    machine_id              BIGINT,
    employee_id             BIGINT,
    performed_by_user_id    BIGINT        NOT NULL,
    purpose                 VARCHAR(255),
    remark                  VARCHAR(500),
    item_condition          VARCHAR(20),
    reversal_of_txn_id      BIGINT,
    source                  VARCHAR(20)   NOT NULL DEFAULT 'APP',
    accounting_period_id    BIGINT,
    txn_date                DATE          NOT NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_txn_no UNIQUE (txn_no),
    CONSTRAINT fk_txn_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_txn_machine FOREIGN KEY (machine_id) REFERENCES machines(id),
    CONSTRAINT fk_txn_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_txn_user FOREIGN KEY (performed_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_txn_reversal FOREIGN KEY (reversal_of_txn_id) REFERENCES inventory_transactions(id),
    CONSTRAINT fk_txn_period FOREIGN KEY (accounting_period_id) REFERENCES accounting_periods(id),
    CONSTRAINT chk_txn_type CHECK (txn_type IN (
        'OPENING_BALANCE','PURCHASE_INWARD','RETURN_TO_STOCK','ISSUE_OUTWARD',
        'RETURN_FROM_USER','STOCK_ADJUSTMENT_IN','STOCK_ADJUSTMENT_OUT',
        'TRANSFER_IN','TRANSFER_OUT','DAMAGE','SCRAP','REVERSAL'
    )),
    CONSTRAINT chk_txn_source CHECK (source IN ('APP','LEGACY_IMPORT')),
    CONSTRAINT chk_txn_condition CHECK (item_condition IS NULL OR item_condition IN ('GOOD','DAMAGED','SCRAP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_txn_item_date ON inventory_transactions(item_id, txn_date);
CREATE INDEX idx_txn_machine ON inventory_transactions(machine_id);
CREATE INDEX idx_txn_employee ON inventory_transactions(employee_id);
CREATE INDEX idx_txn_type ON inventory_transactions(txn_type);
CREATE INDEX idx_txn_period ON inventory_transactions(accounting_period_id);

CREATE TABLE stock_assignments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id         BIGINT        NOT NULL,
    employee_id     BIGINT        NOT NULL,
    machine_id      BIGINT,
    assigned_qty    DECIMAL(14,3) NOT NULL,
    returned_qty    DECIMAL(14,3) NOT NULL DEFAULT 0,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ASSIGNED',
    opened_by_txn_id BIGINT       NOT NULL,
    opened_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at        TIMESTAMP    NULL,
    CONSTRAINT fk_assignment_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_assignment_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_assignment_machine FOREIGN KEY (machine_id) REFERENCES machines(id),
    CONSTRAINT fk_assignment_txn FOREIGN KEY (opened_by_txn_id) REFERENCES inventory_transactions(id),
    CONSTRAINT chk_assignment_status CHECK (status IN ('ASSIGNED','PARTIALLY_RETURNED','CLOSED')),
    CONSTRAINT chk_assignment_qty CHECK (returned_qty <= assigned_qty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_assignment_employee ON stock_assignments(employee_id, status);
CREATE INDEX idx_assignment_machine ON stock_assignments(machine_id);
CREATE INDEX idx_assignment_item ON stock_assignments(item_id);

-- ---------------------------------------------------------------------
-- Purchasing
-- ---------------------------------------------------------------------

CREATE TABLE purchase_requisitions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    pr_no               VARCHAR(50)  NOT NULL,
    requested_by_user_id BIGINT      NOT NULL,
    department_id       BIGINT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    priority             VARCHAR(20),
    reason               VARCHAR(500),
    approved_by_user_id  BIGINT,
    approved_at          TIMESTAMP    NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_pr_no UNIQUE (pr_no),
    CONSTRAINT fk_pr_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_pr_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_pr_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_pr_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','ORDERED','RECEIVED','CLOSED')),
    CONSTRAINT chk_pr_priority CHECK (priority IS NULL OR priority IN ('LOW','NORMAL','HIGH','URGENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchase_requisition_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pr_id           BIGINT        NOT NULL,
    item_id         BIGINT        NOT NULL,
    quantity        DECIMAL(14,3) NOT NULL,
    estimated_price DECIMAL(14,2) NOT NULL,
    supplier_id     BIGINT,
    received_txn_id BIGINT,
    CONSTRAINT fk_pr_item_pr FOREIGN KEY (pr_id) REFERENCES purchase_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_item_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_pr_item_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_pr_item_txn FOREIGN KEY (received_txn_id) REFERENCES inventory_transactions(id),
    CONSTRAINT chk_pr_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_pr_item_price CHECK (estimated_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pr_items_pr ON purchase_requisition_items(pr_id);
CREATE INDEX idx_pr_items_item ON purchase_requisition_items(item_id);

-- ---------------------------------------------------------------------
-- Alerts and audit log
-- ---------------------------------------------------------------------

CREATE TABLE alerts (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    type                    VARCHAR(30)  NOT NULL,
    item_id                 BIGINT,
    message                 VARCHAR(500) NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    raised_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_by_user_id BIGINT,
    acknowledged_at         TIMESTAMP    NULL,
    resolved_by_user_id     BIGINT,
    resolved_at             TIMESTAMP    NULL,
    CONSTRAINT fk_alerts_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_alerts_ack_by FOREIGN KEY (acknowledged_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_alerts_resolved_by FOREIGN KEY (resolved_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_alerts_type CHECK (type IN ('LOW_STOCK','OUT_OF_STOCK','HIGH_CONSUMPTION','PENDING_RETURN','UNUSUAL_CONSUMPTION','PURCHASE_PENDING')),
    CONSTRAINT chk_alerts_status CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_item ON alerts(item_id);

CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT,
    action          VARCHAR(50)  NOT NULL,
    module          VARCHAR(50)  NOT NULL,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(50),
    old_value       JSON,
    new_value       JSON,
    ip_address      VARCHAR(50),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
