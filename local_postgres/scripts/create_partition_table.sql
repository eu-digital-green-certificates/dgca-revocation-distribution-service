-- Table: public.partitions

-- DROP TABLE IF EXISTS public.partitions;

CREATE TABLE IF NOT EXISTS public.partitions
(
    kid text COLLATE pg_catalog."default" NOT NULL,
    partition_id text COLLATE pg_catalog."default",
    x text COLLATE pg_catalog."default",
    y text COLLATE pg_catalog."default",
    z text COLLATE pg_catalog."default",
    expired timestamp with time zone,
    lastupdated timestamp with time zone,
    chunks_json_data jsonb,
    CONSTRAINT partitions_pkey PRIMARY KEY (kid)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.partitions
    OWNER to postgres;