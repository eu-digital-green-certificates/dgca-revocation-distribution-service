-- FUNCTION: public.set_last_updated_function()

-- DROP FUNCTION IF EXISTS public.set_last_updated_function();

CREATE OR REPLACE FUNCTION public.set_last_updated_function()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
BEGIN
  NEW.last_updated := NOW();

  RETURN NEW;
END;
$BODY$;

ALTER FUNCTION public.set_last_updated_function()
    OWNER TO postgres;
