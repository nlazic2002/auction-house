INSERT INTO accounts (
    first_name,
    last_name,
    user_name,
    email,
    role,
    phone_number,
    password_hash
)
VALUES (
           'Admin',
           'Admin',
           'admin.admin',
           'admin@example.com',
           'admin',
           '+385911111111',
           '$2a$12$mn6t7rsCxk2etxODWWHV7ONUgqv8t0df.iApI5wexMoSosqErvytq'
       );

INSERT INTO addresses (
    account_id,
    recipient_name,
    address_line_1,
    postal_code,
    locality,
    administrative_area,
    country_code
)
VALUES (
           (SELECT id FROM accounts WHERE user_name = 'admin.admin'),
           'Admin Admin',
           'Ilica 120',
           '10000',
           'Zagreb',
           'Grad Zagreb',
           'HR'
       );

INSERT INTO categories (
    name,
    slug
)
VALUES (
           'Classic Cars',
           'classic-cars'
       );

INSERT INTO auctions (
    seller_account_id,
    title,
    description,
    starts_at,
    ends_at,
    status
)
VALUES (
           (SELECT id FROM accounts WHERE user_name = 'admin.admin'),
           'Classic Cars Auction',
           'Auction of classic and collectible vehicles.',
           '2026-10-15 10:00:00',
           '2026-10-15 18:00:00',
           'scheduled'
       );

INSERT INTO lots (
    auction_id,
    category_id,
    lot_number,
    title,
    description,
    starting_price,
    reserve_price,
    minimum_bid_increment
)
VALUES (
           (SELECT id FROM auctions WHERE title = 'Classic Cars Auction'),
           (SELECT id FROM categories WHERE slug = 'classic-cars'),
           1,
           '1967 Ford Mustang',
           'Restored classic vehicle in excellent condition.',
           15000.00,
           25000.00,
           500.00
       );

INSERT INTO bids (
    account_id,
    lot_id,
    amount
)
VALUES (
           (SELECT id FROM accounts WHERE user_name = 'admin.admin'),
           (
               SELECT l.id
               FROM lots l
                        JOIN auctions a ON a.id = l.auction_id
               WHERE a.title = 'Classic Cars Auction'
                 AND l.lot_number = 1
           ),
           15500.00
       );