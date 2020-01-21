ALTER TABLE seq_type ADD COLUMN legacy boolean;
UPDATE seq_type SET legacy = FALSE WHERE legacy IS NOT FALSE;
ALTER TABLE seq_type ALTER COLUMN legacy SET NOT NULL;

ALTER TABLE library_preparation_kit ADD COLUMN legacy boolean;
UPDATE library_preparation_kit SET legacy = FALSE WHERE legacy IS NOT FALSE;
ALTER TABLE library_preparation_kit ALTER COLUMN legacy SET NOT NULL;

ALTER TABLE antibody_target ADD COLUMN legacy boolean;
UPDATE antibody_target SET legacy = FALSE WHERE legacy IS NOT FALSE;
ALTER TABLE antibody_target ALTER COLUMN legacy SET NOT NULL;

ALTER TABLE seq_platform ADD COLUMN legacy boolean;
UPDATE seq_platform SET legacy = FALSE WHERE legacy IS NOT FALSE;
ALTER TABLE seq_platform ALTER COLUMN legacy SET NOT NULL;

ALTER TABLE sample_type ADD COLUMN legacy boolean;
UPDATE sample_type SET legacy = FALSE WHERE legacy IS NOT FALSE;
ALTER TABLE sample_type ALTER COLUMN legacy SET NOT NULL;

ALTER TABLE seq_platform_model_label ADD COLUMN legacy boolean;
UPDATE seq_platform_model_label SET legacy = FALSE WHERE legacy IS NOT FALSE;
ALTER TABLE seq_platform_model_label ALTER COLUMN legacy SET NOT NULL;

ALTER TABLE sequencing_kit_label ADD COLUMN legacy boolean;
UPDATE sequencing_kit_label SET legacy = FALSE WHERE legacy IS NOT FALSE;
ALTER TABLE sequencing_kit_label ALTER COLUMN legacy SET NOT NULL;