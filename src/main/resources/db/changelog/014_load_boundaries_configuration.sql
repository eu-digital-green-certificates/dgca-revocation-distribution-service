--liquibase formatted sql
--changeset slaurenz:load-boundary-values splitStatements:true endDelimiter:;

-- Load boundaries for different mode types into configuration table

INSERT INTO configuration (key, value, value2) VALUES ('POINTLIMIT', '0', '100000');
INSERT INTO configuration (key, value, value2) VALUES ('VECTORLIMIT', '100001', '1600000');
INSERT INTO configuration (key, value, value2) VALUES ('COORDINATELIMIT', '1600001', '999999999999');