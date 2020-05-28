-- The entries should be kept in sync with the values of ProjectRole.Basic
INSERT INTO project_role(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'PI', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO project_role(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'DEPUTY_PI', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO project_role(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'LEAD_BIOINFORMATICIAN', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO project_role(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'RESPONSIBLE_BIOINFORMATICIAN', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO project_role(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'BIOINFORMATICIAN', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO project_role(id, version, name, date_created, last_updated)
VALUES(nextval('hibernate_sequence'), 0, 'SUBMITTER', now(), now())
ON CONFLICT DO NOTHING;
