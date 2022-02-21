-- View: public.coordinate_view

-- DROP VIEW public.coordinate_view;

CREATE OR REPLACE VIEW public.coordinate_view
 AS
 SELECT row_number() OVER ()::text AS row_id,
    hashes.kid,
    max(date_trunc('minute'::text, batch_list.expires))::timestamp with time zone AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.z::text AS chunk,
    concat(hashes.x, hashes.y) AS partition_id,
    hashes.x::text AS x,
    hashes.y::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  GROUP BY hashes.kid, hashes.x, hashes.y, hashes.z, (date_trunc('minute'::text, batch_list.expires))
  ORDER BY hashes.kid, (concat(hashes.x, hashes.y)), (hashes.z::text), (date_trunc('minute'::text, batch_list.expires));

ALTER TABLE public.coordinate_view
    OWNER TO postgres;

