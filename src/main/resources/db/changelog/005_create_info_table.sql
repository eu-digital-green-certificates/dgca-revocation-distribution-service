--liquibase formatted sql
--changeset slaurenz:create-info-table

CREATE TABLE IF NOT EXISTS info
(
    key text COLLATE pg_catalog."default" NOT NULL,
    value text COLLATE pg_catalog."default",
    CONSTRAINT info_pkey PRIMARY KEY (key)
)
WITH (
    OIDS = FALSE
);