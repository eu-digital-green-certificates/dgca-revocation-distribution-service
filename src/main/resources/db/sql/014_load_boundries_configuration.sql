-- Load boundries for different mode types into configuration table

INSERT INTO public.configuration (key, value, value2) VALUES ('POINTLIMIT', '0', '1000');
INSERT INTO public.configuration (key, value, value2) VALUES ('VECTORLIMIT', '1001', '16000');
INSERT INTO public.configuration (key, value, value2) VALUES ('COORDINATELIMIT', '16001', '999999999');