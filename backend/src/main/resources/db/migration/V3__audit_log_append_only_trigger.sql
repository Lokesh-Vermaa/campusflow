-- =============================================================================
-- JKLU CampusFlow: Audit Log Append-Only Guarantee (V3)
-- Ensures audit_log is strictly immutable and append-only.
-- Updates and Deletions are forbidden by database trigger.
-- =============================================================================

CREATE OR REPLACE FUNCTION forbid_audit_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log table is append-only: UPDATE and DELETE operations are forbidden.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_log_append_only ON audit_logs;

CREATE TRIGGER trg_audit_log_append_only
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW
EXECUTE FUNCTION forbid_audit_log_modification();
