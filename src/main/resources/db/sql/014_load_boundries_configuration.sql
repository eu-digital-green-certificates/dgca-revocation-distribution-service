-- Load boundries for different mode types into configuration table

INSERT INTO public.configuration (key, value, value2) VALUES ('POINTLIMIT', '0', '100000');
INSERT INTO public.configuration (key, value, value2) VALUES ('VECTORLIMIT', '100001', '1600000');
INSERT INTO public.configuration (key, value, value2) VALUES ('COORDINATELIMIT', '1600001', '999999999999');