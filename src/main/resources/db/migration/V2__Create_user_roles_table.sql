
CREATE TABLE user_roles (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            role VARCHAR(50) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT chk_role CHECK (role IN ('PARENT', 'ADMIN', 'MASTER')),
                            CONSTRAINT uk_user_role UNIQUE (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role);