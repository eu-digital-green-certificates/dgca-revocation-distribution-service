--liquibase formatted sql
--changeset slaurenz:create-slices-table

CREATE TABLE IF NOT EXISTS slices
(
    db_id BIGSERIAL,
    etag text COLLATE pg_catalog."default" NOT NULL,
    kid text COLLATE pg_catalog."default" NOT NULL,
    partition_id text COLLATE pg_catalog."default",
    chunk text COLLATE pg_catalog."default",
    hash text COLLATE pg_catalog."default",
    expired timestamp with time zone,
    lastupdated timestamp with time zone,
    to_be_deleted boolean,
    slice_binary_data bytea,
    CONSTRAINT slices_pkey PRIMARY KEY (db_id)
)
WITH (
    OIDS = FALSE
);