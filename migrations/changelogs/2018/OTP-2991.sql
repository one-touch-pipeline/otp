-- add new column
ALTER TABLE user_project_role
  ADD COLUMN receives_notifications BOOLEAN;

-- for now everyone will get the notifications enabled
UPDATE user_project_role
SET
  receives_notifications = TRUE;

-- add missing null constraint
ALTER TABLE user_project_role
  ALTER COLUMN receives_notifications SET NOT NULL;
