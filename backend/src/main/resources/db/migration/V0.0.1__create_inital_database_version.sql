CREATE TYPE account_status_enum AS ENUM ('pending','active','suspended','banned');
CREATE TYPE role_enum AS ENUM ('admin','user');

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    user_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30),
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(30) NOT NULL DEFAULT 'active',
    email_verified_at TIMESTAMP,
    last_login_at TIMESTAMP,
    role VARCHAR(30) NOT NULL DEFAULT 'user',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    anonymized_at TIMESTAMP,

CONSTRAINT uq_accounts_user_name
    UNIQUE (user_name),

CONSTRAINT uq_accounts_email
    UNIQUE (email),

CONSTRAINT chk_accounts_status
    CHECK (
            account_status IN (
                               'pending',
                               'active',
                               'suspended',
                               'banned'
                              )
            ),

CONSTRAINT chk_accounts_role
    CHECK (
            role IN (
                     'user',
                     'admin'
                    )
            )
);

CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    recipient_name VARCHAR(40) NOT NULL,
    company_name VARCHAR(150),
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    postal_code VARCHAR(20) NOT NULL,
    locality VARCHAR(128) NOT NULL,
    administrative_area VARCHAR(128),
    country_code CHAR(2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_addresses_account
    FOREIGN KEY (account_id)
    REFERENCES accounts(id)
    ON DELETE CASCADE
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    parent_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT uq_categories_slug
    UNIQUE (slug),

CONSTRAINT fk_categories_parent
    FOREIGN KEY (parent_id)
    REFERENCES categories(id)
    ON DELETE RESTRICT
);

CREATE TYPE auctions_status_enum AS ENUM ('draft','scheduled','active','ended','cancelled');

CREATE TABLE auctions (
    id BIGSERIAL PRIMARY KEY,
    seller_account_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_auctions_seller
    FOREIGN KEY (seller_account_id)
    REFERENCES accounts(id)

ON DELETE RESTRICT,

CONSTRAINT chk_auctions_dates
    CHECK (ends_at > starts_at),

CONSTRAINT chk_auctions_status
    CHECK (
            status IN (
                         'draft',
                         'scheduled',
                         'active',
                         'ended',
                         'cancelled'
                       )
           )
);

CREATE INDEX idx_auctions_seller_account_id
    ON auctions(seller_account_id);

CREATE INDEX idx_auctions_status
    ON auctions(status);

CREATE INDEX idx_auctions_starts_at
    ON auctions(starts_at);

CREATE INDEX idx_auctions_ends_at
    ON auctions(ends_at);

CREATE TABLE lots (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    lot_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    starting_price DECIMAL(12, 2) NOT NULL,
    reserve_price DECIMAL(12, 2),
    minimum_bid_increment DECIMAL(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_lots_auction
    FOREIGN KEY (auction_id)
    REFERENCES auctions(id)
    ON DELETE RESTRICT,

CONSTRAINT fk_lots_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id)
    ON DELETE RESTRICT,

CONSTRAINT uq_lots_auction_lot_number
    UNIQUE (auction_id, lot_number),

CONSTRAINT chk_lots_starting_price
    CHECK (starting_price >= 0),

CONSTRAINT chk_lots_reserve_price
    CHECK (
            reserve_price IS NULL
            OR reserve_price >= 0
           ),

CONSTRAINT chk_lots_minimum_bid_increment
    CHECK (minimum_bid_increment > 0),

CONSTRAINT chk_lots_status
    CHECK (
            status IN (
                        'pending',
                        'open',
                        'sold',
                        'unsold',
                        'withdrawn'
                        )
           )
);

CREATE TABLE bids (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    lot_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_bids_account
  FOREIGN KEY (account_id)
      REFERENCES accounts(id)
      ON DELETE RESTRICT,

CONSTRAINT fk_bids_lot
  FOREIGN KEY (lot_id)
      REFERENCES lots(id)
      ON DELETE RESTRICT,

CONSTRAINT chk_bids_amount
  CHECK (amount > 0)
);

CREATE INDEX idx_bids_account_id
    ON bids(account_id);

CREATE INDEX idx_bids_lot_id
    ON bids(lot_id);

CREATE INDEX idx_bids_lot_amount
    ON bids(lot_id, amount);

CREATE INDEX idx_bids_lot_created_at
    ON bids(lot_id, created_at);