ALTER TABLE project
ADD COLUMN sample_identifier_parser_bean_name varchar(255);

UPDATE project SET sample_identifier_parser_bean_name = 'NO_PARSER';

ALTER TABLE project
ALTER COLUMN sample_identifier_parser_bean_name SET NOT NULL;
