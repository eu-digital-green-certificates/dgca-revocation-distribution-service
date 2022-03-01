-- Table: public.shedlock_rd

-- DROP TABLE IF EXISTS public.shedlock_rd;

CREATE TABLE IF NOT EXISTS public.shedlock_rd
(
    id BIGSERIAL,
    lock_until timestamp without time zone NOT NULL,
    locked_at timestamp without time zone NOT NULL,
    locked_by character varying(255) COLLATE pg_catalog."default" NOT NULL,
    name character varying(64) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT shedlock_rd_pkey PRIMARY KEY (id),
    CONSTRAINT uk_2ad9gyjxfy85r5k5yssh63e63 UNIQUE (name)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.shedlock_rd
    OWNER to postgres;