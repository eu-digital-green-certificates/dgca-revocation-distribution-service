-- Table: public.rd_changelog_lock

-- DROP TABLE IF EXISTS public.rd_changelog_lock;

CREATE TABLE IF NOT EXISTS public.rd_changelog_lock
(
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT rd_changelog_lock_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.rd_changelog_lock
    OWNER to postgres;