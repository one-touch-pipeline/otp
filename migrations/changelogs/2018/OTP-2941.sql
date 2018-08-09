-- add columns from project_role
ALTER TABLE user_project_role
  ADD COLUMN access_to_otp BOOLEAN,
  ADD COLUMN access_to_files BOOLEAN,
  ADD COLUMN manage_users_and_delegate BOOLEAN;


-- change NO_ACCESS to OTHER with access_to_otp set to TRUE
UPDATE user_project_role
  SET project_role_id = (SELECT id FROM project_role WHERE name = 'OTHER')
  WHERE project_role_id = (SELECT id FROM project_role WHERE name = 'NO_ACCESS');

-- migrate the role permissions
UPDATE user_project_role
SET
  access_to_otp = (SELECT access_to_otp FROM project_role WHERE project_role.id = user_project_role.project_role_id),
  access_to_files = (SELECT access_to_files FROM project_role WHERE project_role.id = user_project_role.project_role_id),
  manage_users_and_delegate = (SELECT manage_users_and_delegate FROM project_role WHERE project_role.id = user_project_role.project_role_id);

-- add missing null constraint to the new columns
ALTER TABLE user_project_role
  ALTER COLUMN access_to_otp SET NOT NULL,
  ALTER COLUMN access_to_files SET NOT NULL,
  ALTER COLUMN manage_users SET NOT NULL,
  ALTER COLUMN manage_users_and_delegate SET NOT NULL;

-- delete NO_ACCESS project_role as it is now obsolete
DELETE FROM project_role
WHERE name = 'NO_ACCESS';

-- delete the obsolete columns in project_role
ALTER TABLE project_role
  DROP COLUMN access_to_otp,
  DROP COLUMN access_to_files,
  DROP COLUMN manage_users_and_delegate;
