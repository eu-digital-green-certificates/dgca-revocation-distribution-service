-- View: public.coordinate_view

-- DROP VIEW public.coordinate_view;

CREATE OR REPLACE VIEW public.coordinate_view
 AS
 SELECT hashes.kid,
    max(batch_list.expires) AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.z::text AS chunk,
    concat(hashes.x, hashes.y) AS id,
    hashes.x::text AS x,
    hashes.y::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  GROUP BY hashes.kid, hashes.x, hashes.y, hashes.z, batch_list.expires
  ORDER BY hashes.kid, (concat(hashes.x, hashes.y)), (hashes.z::text), batch_list.expires;

ALTER TABLE public.coordinate_view
    OWNER TO postgres;

