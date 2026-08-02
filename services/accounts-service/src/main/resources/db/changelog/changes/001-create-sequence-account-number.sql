--liquibase formatted sql

--changeset ubrnt:1
CREATE SEQUENCE account_number_seq START WITH 1 INCREMENT BY 1;
