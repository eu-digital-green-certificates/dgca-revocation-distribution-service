-- View: public.point_view

-- DROP VIEW public.point_view;

CREATE OR REPLACE VIEW public.point_view
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

ALTER TABLE public.point_view
    OWNER TO postgres;

