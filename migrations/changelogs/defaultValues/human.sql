INSERT INTO common_name(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'Human', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO species(id, version, scientific_name, date_created, last_updated, common_name_id)
VALUES(nextval('hibernate_sequence'), 0, 'Homo sapiens', now(), now(),(SELECT id FROM common_name WHERE name = 'Human'))
ON CONFLICT DO NOTHING;

INSERT INTO species_with_strain(id, version, species_id, strain_id, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0,(SELECT id FROM species WHERE scientific_name = 'Homo sapiens'),(SELECT id FROM strain WHERE name = 'Not available'), now(), now())
ON CONFLICT DO NOTHING;
