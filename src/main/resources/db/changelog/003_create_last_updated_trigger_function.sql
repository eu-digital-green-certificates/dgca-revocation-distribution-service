--liquibase formatted sql
--changeset slaurenz:create-set-last-updated-function splitStatements:false

CREATE OR REPLACE FUNCTION set_last_updated_function()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
BEGIN
  NEW.last_updated := NOW();
  NEW.updated := true;
  RETURN NEW;
END;
$BODY$;

