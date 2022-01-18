-- Table: public.rd_changelog

-- DROP TABLE IF EXISTS public.rd_changelog;

CREATE TABLE IF NOT EXISTS public.rd_changelog
(
    id character varying(255) COLLATE pg_catalog."default" NOT NULL,
    author character varying(255) COLLATE pg_catalog."default" NOT NULL,
    filename character varying(255) COLLATE pg_catalog."default" NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) COLLATE pg_catalog."default" NOT NULL,
    md5sum character varying(35) COLLATE pg_catalog."default",
    description character varying(255) COLLATE pg_catalog."default",
    comments character varying(255) COLLATE pg_catalog."default",
    tag character varying(255) COLLATE pg_catalog."default",
    liquibase character varying(20) COLLATE pg_catalog."default",
    contexts character varying(255) COLLATE pg_catalog."default",
    labels character varying(255) COLLATE pg_catalog."default",
    deployment_id character varying(10) COLLATE pg_catalog."default"
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.rd_changelog
    OWNER to postgres;