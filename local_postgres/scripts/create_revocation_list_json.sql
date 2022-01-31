-- Table: public.revocation_list_json

-- DROP TABLE IF EXISTS public.revocation_list_json;

CREATE TABLE IF NOT EXISTS public.revocation_list_json
(
    etag text COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp with time zone,
    json_data jsonb,
    CONSTRAINT revocation_list_json_pkey PRIMARY KEY (etag)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.revocation_list_json
    OWNER to postgres;