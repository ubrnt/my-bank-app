--liquibase formatted sql

--changeset ubrnt:1
INSERT INTO customers (uuid, login, name, birthdate, created_ts, updated_ts)
VALUES (gen_random_uuid(), 'user1', 'user1_first_name user1_last_name', DATE '1990-01-15', now(), now()),
       (gen_random_uuid(), 'user2', 'user2_first_name user2_last_name', DATE '1985-06-02', now(), now()),
       (gen_random_uuid(), 'user3', 'user3_first_name user3_last_name', DATE '2000-11-27', now(), now());

INSERT INTO accounts (uuid, number, customer_id, balance, created_ts, updated_ts)
SELECT gen_random_uuid(),
       '40817810' || lpad(nextval('account_number_seq')::text, 12, '0'),
       c.id,
       100000,
       now(),
       now()
  FROM customers c
 ORDER BY c.id;
