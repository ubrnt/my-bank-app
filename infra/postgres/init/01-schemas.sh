#!/bin/bash

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE USER accounts_service WITH PASSWORD '${ACCOUNTS_DB_PASSWORD}';
	CREATE SCHEMA accounts AUTHORIZATION accounts_service;
	ALTER ROLE accounts_service SET search_path = accounts;

	CREATE USER notifications_service WITH PASSWORD '${NOTIFICATIONS_DB_PASSWORD}';
	CREATE SCHEMA notifications AUTHORIZATION notifications_service;
	ALTER ROLE notifications_service SET search_path = notifications;

	CREATE USER cash_service WITH PASSWORD '${CASH_DB_PASSWORD}';
	CREATE SCHEMA cash AUTHORIZATION cash_service;
	ALTER ROLE cash_service SET search_path = cash;

	CREATE USER transfer_service WITH PASSWORD '${TRANSFER_DB_PASSWORD}';
	CREATE SCHEMA transfer AUTHORIZATION transfer_service;
	ALTER ROLE transfer_service SET search_path = transfer;
EOSQL
