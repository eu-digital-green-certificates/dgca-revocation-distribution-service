-- Trigger: set_last_updated_trigger

-- DROP TRIGGER IF EXISTS set_last_updated_trigger ON public.hashes;

CREATE TRIGGER set_last_updated_trigger
    BEFORE UPDATE 
    ON public.hashes
    FOR EACH ROW
    WHEN (new.updated IS TRUE OR new.batch_id IS NULL)
    EXECUTE PROCEDURE public.set_last_updated_function();