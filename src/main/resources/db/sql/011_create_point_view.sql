-- View: public.point_view

-- DROP VIEW public.point_view;

CREATE OR REPLACE VIEW public.point_view
 AS
 SELECT row_number() OVER ()::text AS row_id,
    hashes.kid,
    max(date_trunc('minute'::text, batch_list.expires))::timestamp with time zone AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.x::text AS chunk,
    NULL::text AS partition_id,
    NULL::text AS x,
    NULL::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  GROUP BY hashes.kid, hashes.x, (date_trunc('minute'::text, batch_list.expires));

ALTER TABLE public.point_view
    OWNER TO postgres;

