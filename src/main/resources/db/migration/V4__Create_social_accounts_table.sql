-- V4__Create_social_accounts_table.sql

CREATE TABLE social_accounts (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 provider VARCHAR(50) NOT NULL,
                                 provider_user_id VARCHAR(255) NOT NULL,
                                 email VARCHAR(255),
                                 display_name VARCHAR(255),
                                 linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                 CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id),
                                 CONSTRAINT chk_provider CHECK (provider IN ('GOOGLE', 'KAKAO', 'NAVER'))
);

CREATE INDEX idx_social_accounts_user_id ON social_accounts(user_id);
CREATE INDEX idx_social_accounts_provider ON social_accounts(provider);