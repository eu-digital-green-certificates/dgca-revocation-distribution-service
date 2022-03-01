--liquibase formatted sql
--changeset slaurenz:create-batch-list-table

CREATE TABLE IF NOT EXISTS batch_list
(
    batch_id character varying(36) COLLATE pg_catalog."default" NOT NULL,
    country character varying(2) COLLATE pg_catalog."default" NOT NULL,
    expires timestamp with time zone NOT NULL,
    kid character varying(12) COLLATE pg_catalog."default",
    type character varying(255) COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp with time zone,
    CONSTRAINT batch_list_pkey PRIMARY KEY (batch_id)
)
WITH (
    OIDS = FALSE
);


