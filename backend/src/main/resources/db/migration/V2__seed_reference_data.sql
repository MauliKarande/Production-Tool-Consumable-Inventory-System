-- Reference data that the application depends on to boot (roles) and
-- real-world masters observed in the legacy Excel files, seeded so the
-- system is usable on day one. All of it remains editable by Admin
-- through the Masters screens - nothing here is hard-coded in Java.
--
-- Deliberately NOT seeded here: any user account. Creating the initial
-- Admin login is handled by an application startup bootstrap (see
-- config.AdminBootstrapRunner) so a password hash never has to live in
-- a migration file / version control.

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Full system access'),
    ('ISSUER', 'Store/production user - issues and receives tools'),
    ('VIEWER', 'Read-only access for management');

INSERT INTO departments (name) VALUES
    ('PRODUCTION'),
    ('STORE'),
    ('PURCHASE');

INSERT INTO units_of_measure (code, name) VALUES
    ('PCS', 'Pieces'),
    ('LTR', 'Litres'),
    ('SET', 'Set'),
    ('PKT', 'Packet'),
    ('MTR', 'Metre'),
    ('KG',  'Kilogram');

INSERT INTO item_categories (name) VALUES
    ('INSERTS'),
    ('DRILLS'),
    ('TAPS'),
    ('END MILLS'),
    ('CUTTING TOOLS'),
    ('OILS'),
    ('COOLANTS'),
    ('OTHER CONSUMABLES');

-- Machines observed in the legacy consumption/oil-tracking sheets.
INSERT INTO machines (machine_code, machine_name, machine_type, active) VALUES
    ('CNC-1', 'CNC-1', 'CNC', TRUE),
    ('CNC-3', 'CNC-3', 'CNC', TRUE),
    ('CNC-4', 'CNC-4', 'CNC', TRUE),
    ('CNC-5', 'CNC-5', 'CNC', TRUE),
    ('CNC-6', 'CNC-6', 'CNC', TRUE),
    ('CNC-7', 'CNC-7', 'CNC', TRUE),
    ('CNC-8', 'CNC-8', 'CNC', TRUE),
    ('CNC-9', 'CNC-9', 'CNC', TRUE),
    ('VMC-1', 'VMC-1', 'VMC', TRUE),
    ('VMC-3', 'VMC-3', 'VMC', TRUE),
    ('VMC-4', 'VMC-4', 'VMC', TRUE),
    ('VMC-5', 'VMC-5', 'VMC', TRUE),
    ('VTL-1', 'VTL-1', 'VTL', TRUE),
    ('VTL-2', 'VTL-2', 'VTL', TRUE),
    ('VTL-3', 'VTL-3', 'VTL', TRUE),
    ('GRINDING-1', 'Grinding-1', 'GRINDING', TRUE),
    ('GRINDING-2', 'Grinding-2', 'GRINDING', TRUE),
    ('CTM', 'CTM', 'OTHER', TRUE);

INSERT INTO accounting_periods (period, status) VALUES
    (DATE_FORMAT(CURDATE(), '%Y-%m'), 'OPEN');
