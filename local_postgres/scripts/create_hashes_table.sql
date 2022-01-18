-- Table: public.hashes

-- DROP TABLE IF EXISTS public.hashes;

CREATE TABLE IF NOT EXISTS public.hashes
(
    hash character varying(255) COLLATE pg_catalog."default" NOT NULL,
    batch_id character varying(36) COLLATE pg_catalog."default",
    kid character varying(12) COLLATE pg_catalog."default",
    updated boolean,
    x character(1) COLLATE pg_catalog."default" NOT NULL,
    y character(1) COLLATE pg_catalog."default" NOT NULL,
    z character(1) COLLATE pg_catalog."default" NOT NULL,
    last_updated timestamp with time zone DEFAULT now(),
    CONSTRAINT hashes_pkey PRIMARY KEY (hash),
    CONSTRAINT fk_batch_id FOREIGN KEY (batch_id)
        REFERENCES public.batch_list (batch_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE SET NULL
        NOT VALID
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.hashes
    OWNER to postgres;
-- Index: fki_fk_batch_id

-- DROP INDEX IF EXISTS public.fki_fk_batch_id;

CREATE INDEX IF NOT EXISTS fki_fk_batch_id
    ON public.hashes USING btree
    (batch_id COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

-- Trigger: set_last_updated_trigger

-- DROP TRIGGER IF EXISTS set_last_updated_trigger ON public.hashes;

CREATE TRIGGER set_last_updated_trigger
    BEFORE UPDATE 
    ON public.hashes
    FOR EACH ROW
    WHEN (new.updated IS TRUE OR new.batch_id IS NULL)
    EXECUTE PROCEDURE public.set_last_updated_function();