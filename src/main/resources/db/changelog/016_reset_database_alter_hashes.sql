--liquibase formatted sql
--changeset slaurenz:recreate database splitStatements:false

-- Drop existing data
drop TABLE IF EXISTS hashes cascade;
drop TABLE IF EXISTS batch_list cascade;
drop TABLE IF EXISTS configuration cascade;
drop TABLE IF EXISTS info cascade;
drop TABLE IF EXISTS partitions cascade;
drop TABLE IF EXISTS revocation_list_json cascade;
drop TABLE IF EXISTS shedlock_rd cascade;
drop TABLE IF EXISTS slices cascade;
DROP FUNCTION IF EXISTS set_last_updated_function();
DROP FUNCTION IF EXISTS set_new_etag(text);


-- recreate batch list

CREATE TABLE IF NOT EXISTS batch_list
(
    batch_id character varying(36) COLLATE pg_catalog."default" NOT NULL,
    country character varying(2) COLLATE pg_catalog."default" NOT NULL,
    expires timestamp with time zone NOT NULL,
    kid character varying(12) COLLATE pg_catalog."default",
    type character varying(255) COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp with time zone,
    CONSTRAINT batch_list_pkey PRIMARY KEY (batch_id)
)
WITH (
    OIDS = FALSE
);


-- recreate configuration
CREATE TABLE IF NOT EXISTS configuration
(
    key text COLLATE pg_catalog."default" NOT NULL,
    value text COLLATE pg_catalog."default",
    value2 text COLLATE pg_catalog."default",
    CONSTRAINT configuration_pkey PRIMARY KEY (key)
)
WITH (
    OIDS = FALSE
);

-- recreate
CREATE OR REPLACE FUNCTION set_last_updated_function()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
BEGIN
  NEW.last_updated := NOW();
  NEW.updated := true;
  RETURN NEW;
END;
$BODY$;

-- recreate hashes
CREATE TABLE IF NOT EXISTS hashes
(
    id uuid NOT NULL,
    hash character varying(255) COLLATE pg_catalog."default" NOT NULL,
    batch_id character varying(36) COLLATE pg_catalog."default",
    kid character varying(12) COLLATE pg_catalog."default",
    updated boolean,
    x character(1) COLLATE pg_catalog."default" NOT NULL,
    y character(1) COLLATE pg_catalog."default" NOT NULL,
    z character(1) COLLATE pg_catalog."default" NOT NULL,
    last_updated timestamp with time zone DEFAULT now(),
    CONSTRAINT hashes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_batch_id FOREIGN KEY (batch_id)
        REFERENCES batch_list (batch_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE SET NULL
        NOT VALID
)
WITH (
    OIDS = FALSE
);

-- Index: fki_fk_batch_id
-- DROP INDEX IF EXISTS fki_fk_batch_id;

CREATE INDEX IF NOT EXISTS fki_fk_batch_id
    ON hashes USING btree
    (batch_id COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

-- Trigger: set_last_updated_trigger
-- DROP TRIGGER IF EXISTS set_last_updated_trigger ON hashes;

CREATE TRIGGER set_last_updated_trigger
    BEFORE UPDATE 
    ON hashes
    FOR EACH ROW
    WHEN (new.updated IS TRUE OR new.batch_id IS NULL)
    EXECUTE PROCEDURE set_last_updated_function();

-- recreate info
CREATE TABLE IF NOT EXISTS info
(
    key text COLLATE pg_catalog."default" NOT NULL,
    value text COLLATE pg_catalog."default",
    CONSTRAINT info_pkey PRIMARY KEY (key)
)
WITH (
    OIDS = FALSE
);

-- recreate partitions
CREATE TABLE IF NOT EXISTS partitions
(
    db_id BIGSERIAL,
    etag text COLLATE pg_catalog."default" NOT NULL,
    kid text COLLATE pg_catalog."default" NOT NULL,
    partition_id text COLLATE pg_catalog."default",
    x text COLLATE pg_catalog."default",
    y text COLLATE pg_catalog."default",
    z text COLLATE pg_catalog."default",
    expired timestamp with time zone,
    lastupdated timestamp with time zone,
    to_be_deleted boolean,
    data_type text COLLATE pg_catalog."default",
    chunks_json_data jsonb,
    CONSTRAINT partitions_pkey PRIMARY KEY (db_id)
)
WITH (
    OIDS = FALSE
);

-- recreate revocation list json

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


-- recreate slices
CREATE TABLE IF NOT EXISTS slices
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
    data_type text COLLATE pg_catalog."default",
    slice_binary_data bytea,
    CONSTRAINT slices_pkey PRIMARY KEY (db_id)
)
WITH (
    OIDS = FALSE
);

--recreate views
CREATE OR REPLACE VIEW coordinate_view
 AS
 SELECT row_number() OVER ()::text AS row_id,
        CASE
            WHEN hashes.kid IS NULL THEN 'UNKNOWN_KID'::character varying
            ELSE hashes.kid
        END AS kid,
    max(date_trunc('minute'::text, batch_list.expires))::timestamp with time zone AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.z::text AS chunk,
    concat(hashes.x, hashes.y) AS partition_id,
    hashes.x::text AS x,
    hashes.y::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  WHERE hashes.batch_id IS NOT NULL
  GROUP BY hashes.kid, hashes.x, hashes.y, hashes.z, (date_trunc('minute'::text, batch_list.expires))
  ORDER BY hashes.kid, (concat(hashes.x, hashes.y)), (hashes.z::text), (date_trunc('minute'::text, batch_list.expires));


CREATE OR REPLACE VIEW kid_view
 AS
 WITH configuration AS (
         SELECT
                CASE
                    WHEN configuration_1.key = 'POINTLIMIT'::text THEN 'POINT'::text
                    WHEN configuration_1.key = 'VECTORLIMIT'::text THEN 'VECTOR'::text
                    WHEN configuration_1.key = 'COORDINATELIMIT'::text THEN 'COORDINATE'::text
                    ELSE NULL::text
                END AS storage_mode,
            to_number(configuration_1.value, '999999999999'::text) AS minlimit,
            to_number(configuration_1.value2, '999999999999'::text) AS maxlimit
           FROM configuration configuration_1
          WHERE configuration_1.key = ANY (ARRAY['POINTLIMIT'::text, 'VECTORLIMIT'::text, 'COORDINATELIMIT'::text])
        )
 SELECT a.kid,
    a.hashtypes,
    configuration.storage_mode,
    a.lastupdated,
    a.expired,
    a.updated
   FROM ( SELECT
                CASE
                    WHEN hashes.kid IS NULL THEN 'UNKNOWN_KID'::character varying
                    ELSE hashes.kid
                END AS kid,
            count(*) AS c,
            array_to_string(array_agg(DISTINCT batch_list.type), ','::text) AS hashtypes,
            bool_or(hashes.updated) AS updated,
            max(batch_list.expires) AS expired,
            max(hashes.last_updated) AS lastupdated
           FROM hashes
             LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
          GROUP BY hashes.kid) a,
    configuration
  WHERE a.c::numeric >= configuration.minlimit AND a.c::numeric <= configuration.maxlimit;


CREATE OR REPLACE VIEW point_view
 AS
 SELECT row_number() OVER ()::text AS row_id,
        CASE
            WHEN hashes.kid IS NULL THEN 'UNKNOWN_KID'::character varying
            ELSE hashes.kid
        END AS kid,
    max(date_trunc('minute'::text, batch_list.expires))::timestamp with time zone AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.x::text AS chunk,
    NULL::text AS partition_id,
    NULL::text AS x,
    NULL::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  WHERE hashes.batch_id IS NOT NULL
  GROUP BY hashes.kid, hashes.x, (date_trunc('minute'::text, batch_list.expires));



CREATE OR REPLACE VIEW vector_view
 AS
 SELECT row_number() OVER ()::text AS row_id,
        CASE
            WHEN hashes.kid IS NULL THEN 'UNKNOWN_KID'::character varying
            ELSE hashes.kid
        END AS kid,
    max(date_trunc('minute'::text, batch_list.expires))::timestamp with time zone AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.y::text AS chunk,
    hashes.x::text AS partition_id,
    hashes.x::text AS x,
    NULL::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  WHERE hashes.batch_id IS NOT NULL
  GROUP BY hashes.kid, hashes.x, hashes.y, (date_trunc('minute'::text, batch_list.expires))
  ORDER BY hashes.kid, (hashes.x::text), NULL::text, (date_trunc('minute'::text, batch_list.expires));


-- create set new etag function
CREATE OR REPLACE FUNCTION set_new_etag(
	new_etag text)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
	BEGIN
		-- Update etag in info table
		INSERT INTO info (key, value)
		VALUES('CURRENTETAG', new_etag) 
		ON CONFLICT (key) 
		DO UPDATE SET value = EXCLUDED.value;

		-- Delete old slices and partitions
		DELETE FROM partitions WHERE to_be_deleted=true;
		DELETE FROM slices WHERE to_be_deleted=true;

		-- Update etag field of slices and partitions
		Update partitions SET etag = new_etag;
		Update slices SET etag = new_etag;	

		-- Update etag in info table
		INSERT INTO info (key, value)
		VALUES('CURRENTETAG', new_etag) 
		ON CONFLICT (key) 
		DO UPDATE SET value = EXCLUDED.value;
		
		RETURN 1;
	END;
$BODY$;

-- reload boundries
INSERT INTO configuration (key, value, value2) VALUES ('POINTLIMIT', '0', '100000');
INSERT INTO configuration (key, value, value2) VALUES ('VECTORLIMIT', '100001', '1600000');
INSERT INTO configuration (key, value, value2) VALUES ('COORDINATELIMIT', '1600001', '999999999999');

-- recreate schedlock
CREATE TABLE IF NOT EXISTS shedlock_rd
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
);
