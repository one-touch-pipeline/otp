ALTER TABLE seq_track ADD COLUMN sample_identifier VARCHAR(255);

UPDATE seq_track st SET sample_identifier = subquery.value
FROM (SELECT regexp_replace(meta_data_entry.value,'\(.+\)\(.+\)\(.+\)\((.+)\)', '\1') as value, df.seq_track_id
      FROM meta_data_entry
               JOIN data_file df ON meta_data_entry.data_file_id = df.id
               JOIN meta_data_key mdk ON meta_data_entry.key_id = mdk.id
      WHERE mdk.name = 'SAMPLE_ID'
     ) AS subquery
WHERE st.id = subquery.seq_track_id;

/*ALTER TABLE seq_track ALTER COLUMN sample_identifier SET NOT NULL;*/