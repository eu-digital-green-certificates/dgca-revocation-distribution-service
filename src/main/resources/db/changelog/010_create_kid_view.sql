--liquibase formatted sql
--changeset slaurenz:create-kid-view

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


