
CREATE TABLE audit_logs (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT,
                            action VARCHAR(100) NOT NULL,
                            resource_type VARCHAR(100),
                            resource_id VARCHAR(255),
                            details JSONB,
                            ip_address VARCHAR(45),
                            user_agent VARCHAR(500),
                            status VARCHAR(20) NOT NULL,
                            error_message TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                            CONSTRAINT chk_status CHECK (status IN ('SUCCESS', 'FAILURE', 'PARTIAL'))
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_composite ON audit_logs(user_id, action, created_at);