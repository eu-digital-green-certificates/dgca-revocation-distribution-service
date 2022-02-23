-- View: public.vector_view

-- DROP VIEW public.vector_view;

CREATE OR REPLACE VIEW public.vector_view
 AS
 SELECT row_number() OVER ()::text AS row_id,
    case when hashes.kid isNull then 'UNKNOWN_KID'
      	 else hashes.kid
    END as kid,
    max(date_trunc('minute'::text, batch_list.expires))::timestamp with time zone AS expired,
    max(hashes.last_updated) AS lastupdated,
    array_agg(DISTINCT hashes.hash) AS hashes,
    hashes.y::text AS chunk,
    hashes.x::text AS partition_id,
    hashes.x::text AS x,
    NULL::text AS y
   FROM hashes
     LEFT JOIN batch_list ON hashes.batch_id::text = batch_list.batch_id::text
  GROUP BY hashes.kid, hashes.x, hashes.y, (date_trunc('minute'::text, batch_list.expires))
  ORDER BY hashes.kid, (hashes.x::text), NULL::text, (date_trunc('minute'::text, batch_list.expires));

ALTER TABLE public.vector_view
    OWNER TO postgres;

