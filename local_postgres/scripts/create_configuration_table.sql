-- Table: public.configuration

-- DROP TABLE IF EXISTS public.configuration;

CREATE TABLE IF NOT EXISTS public.configuration
(
    key text COLLATE pg_catalog."default" NOT NULL,
    value text COLLATE pg_catalog."default",
    value2 text COLLATE pg_catalog."default",
    CONSTRAINT configuration_pkey PRIMARY KEY (key)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.configuration
    OWNER to postgres;