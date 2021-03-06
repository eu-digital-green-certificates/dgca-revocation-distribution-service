--liquibase formatted sql
--changeset slaurenz:create-new-etag-function splitStatements:false

CREATE OR REPLACE FUNCTION set_new_etag(
	new_etag text)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
	BEGIN
		-- Update etag in info table
		INSERT INTO info (key, value)
		VALUES('CURRENTETAG', new_etag) 
		ON CONFLICT (key) 
		DO UPDATE SET value = EXCLUDED.value;

		-- Delete old slices and partitions
		DELETE FROM partitions WHERE to_be_deleted=true;
		DELETE FROM slices WHERE to_be_deleted=true;

		-- Update etag field of slices and partitions
		Update partitions SET etag = new_etag;
		Update slices SET etag = new_etag;	

		-- Update etag in info table
		INSERT INTO info (key, value)
		VALUES('CURRENTETAG', new_etag) 
		ON CONFLICT (key) 
		DO UPDATE SET value = EXCLUDED.value;
		
		RETURN 1;
	END;
$BODY$;

