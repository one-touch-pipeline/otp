ALTER TABLE project_info ADD COLUMN recipient VARCHAR(255);
ALTER TABLE project_info ADD COLUMN performing_user_id BIGINT;
ALTER TABLE project_info ADD COLUMN commissioning_user_id BIGINT;
ALTER TABLE project_info ADD COLUMN transfer_date DATE;
ALTER TABLE project_info ADD COLUMN validity_date DATE;
ALTER TABLE project_info ADD COLUMN transfer_mode VARCHAR(255);
ALTER TABLE project_info ADD COLUMN legal_basis VARCHAR(255);
ALTER TABLE project_info ADD FOREIGN KEY (performing_user_id) REFERENCES users(id);
ALTER TABLE project_info ADD FOREIGN KEY (commissioning_user_id) REFERENCES users(id);
