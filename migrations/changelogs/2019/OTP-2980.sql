ALTER TABLE project
  ADD COLUMN closed BOOLEAN;

UPDATE project SET
  closed = FALSE;

ALTER TABLE project ALTER COLUMN closed SET NOT NULL;
