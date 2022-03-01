-- Table: public.slices

-- DROP TABLE IF EXISTS public.slices;

CREATE TABLE IF NOT EXISTS public.slices
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
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.slices
    OWNER to postgres;