-- View: public.vector_view

-- DROP VIEW public.vector_view;

CREATE OR REPLACE VIEW public.vector_view
 AS
 SELECT hashes.kid,
    max(batch_list.expires) AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.y::text AS chunk,
    hashes.x::text AS id,
    hashes.x::text AS x,
    NULL::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  GROUP BY hashes.kid, hashes.x, hashes.y, batch_list.expires
  ORDER BY hashes.kid, (hashes.x::text), NULL::text, batch_list.expires;

ALTER TABLE public.vector_view
    OWNER TO postgres;

