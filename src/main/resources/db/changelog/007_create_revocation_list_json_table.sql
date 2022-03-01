--liquibase formatted sql
--changeset slaurenz:create-revocation-list-json-table

CREATE TABLE IF NOT EXISTS revocation_list_json
(
    etag text COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp with time zone,
    json_data jsonb,
    CONSTRAINT revocation_list_json_pkey PRIMARY KEY (etag)
)
WITH (
    OIDS = FALSE
);