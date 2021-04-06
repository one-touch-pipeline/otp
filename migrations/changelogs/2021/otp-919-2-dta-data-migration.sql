DO
'
DECLARE
    p_info record;
    dta_id integer;
BEGIN
FOR p_info IN SELECT *
              FROM project_info WHERE peer_institution IS NOT NULL
                  LOOP
INSERT INTO data_transfer_agreement(id, version, date_created, last_updated, project_id, comment, dta_id, peer_institution, legal_basis, validity_date)
VALUES (nextval(''hibernate_sequence''), p_info.version, p_info.date_created, p_info.last_updated, p_info.project_id, p_info.comment, p_info.dta_id, p_info.peer_institution, p_info.legal_basis, p_info.validity_date)
RETURNING id INTO dta_id;

INSERT INTO data_transfer_agreement_document(id, version, date_created, last_updated, data_transfer_agreement_id, file_name)
VALUES (nextval(''hibernate_sequence''), p_info.version, p_info.date_created, p_info.last_updated, dta_id, p_info.file_name);

UPDATE data_transfer SET data_transfer_agreement_id = dta_id WHERE project_info_id = p_info.id;
END LOOP;

ALTER TABLE data_transfer DROP COLUMN project_info_id;
ALTER TABLE data_transfer ALTER COLUMN data_transfer_agreement_id SET NOT NULL;

DELETE FROM project_info WHERE peer_institution IS NOT NULL;
END;
'  LANGUAGE PLPGSQL;
