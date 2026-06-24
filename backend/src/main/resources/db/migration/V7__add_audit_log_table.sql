CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY,
    admin_user_id UUID,
    admin_email VARCHAR(255),
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    details JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_admin_user ON admin_audit_log (admin_user_id);
CREATE INDEX idx_audit_log_entity ON admin_audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON admin_audit_log (created_at DESC);
CREATE INDEX idx_audit_log_action_type ON admin_audit_log (action_type);
