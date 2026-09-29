CREATE TYPE account_status_enum AS ENUM ('pending','active','suspended','banned');
CREATE TYPE role_enum AS ENUM ('admin','user');

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(15),
    last_name VARCHAR(25),
    user_name VARCHAR(30) UNIQUE,
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(30),
    password_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_status account_status_enum DEFAULT 'pending',
    email_verified_at TIMESTAMP,
    last_login_at TIMESTAMP,
    role role_enum DEFAULT 'user'
);