--liquibase formatted sql
--changeset slaurenz:create-partitions-table

CREATE TABLE IF NOT EXISTS partitions
(
    db_id BIGSERIAL,
    etag text COLLATE pg_catalog."default" NOT NULL,
    kid text COLLATE pg_catalog."default" NOT NULL,
    partition_id text COLLATE pg_catalog."default",
    x text COLLATE pg_catalog."default",
    y text COLLATE pg_catalog."default",
    z text COLLATE pg_catalog."default",
    expired timestamp with time zone,
    lastupdated timestamp with time zone,
    to_be_deleted boolean,
    chunks_json_data jsonb,
    CONSTRAINT partitions_pkey PRIMARY KEY (db_id)
)
WITH (
    OIDS = FALSE
);