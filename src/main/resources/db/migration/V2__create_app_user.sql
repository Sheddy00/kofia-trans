-- Application accounts for JWT authentication (passwords are BCrypt hashes).
CREATE TABLE IF NOT EXISTS app_user (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
