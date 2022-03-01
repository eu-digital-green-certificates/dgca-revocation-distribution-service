--liquibase formatted sql
--changeset slaurenz:create-coordinate-view

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


