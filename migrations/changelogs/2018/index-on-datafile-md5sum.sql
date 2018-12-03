DROP INDEX IF EXISTS data_file_md5sum_idx;
CREATE INDEX data_file_md5sum_idx on data_file(md5sum);
