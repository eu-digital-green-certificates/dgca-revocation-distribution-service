--liquibase formatted sql
--changeset slaurenz:create-configuration-table

CREATE TABLE IF NOT EXISTS configuration
(
    key text COLLATE pg_catalog."default" NOT NULL,
    value text COLLATE pg_catalog."default",
    value2 text COLLATE pg_catalog."default",
    CONSTRAINT configuration_pkey PRIMARY KEY (key)
)
WITH (
    OIDS = FALSE
);