-- V3: partial-receipt tracking on PR lines, and a no-login system user
-- used as performed_by/requested_by on rows the Excel migration tool
-- creates, where the legacy data has no employee/requester recorded
-- (see docs/phase1-requirements.md A.4 #6).

ALTER TABLE purchase_requisition_items
    ADD COLUMN received_qty DECIMAL(14,3) NOT NULL DEFAULT 0 AFTER received_txn_id;

-- active=FALSE means this account can never authenticate, regardless of
-- the unusable placeholder password hash below.
INSERT INTO users (username, password_hash, role_id, active)
SELECT 'legacy.import', '$2a$10$LEGACYxIMPORTxSYSTEMxUSERxNOxLOGINxPLACEHOLDERxHASHxx', id, FALSE
FROM roles WHERE name = 'VIEWER';
