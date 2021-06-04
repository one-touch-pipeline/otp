INSERT INTO strain(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'No strain available', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO strain(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'Unknown', now(), now())
ON CONFLICT DO NOTHING;
