-- Table: public.info

-- DROP TABLE IF EXISTS public.info;

CREATE TABLE IF NOT EXISTS public.info
(
    key text COLLATE pg_catalog."default" NOT NULL,
    value text COLLATE pg_catalog."default",
    CONSTRAINT info_pkey PRIMARY KEY (key)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.info
    OWNER to postgres;