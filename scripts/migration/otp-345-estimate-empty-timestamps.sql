/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

-- helper-sql mentioned below
/*
 find all normal tables which are in the public schema and have the columns 'id',
 'date_created' and 'last_updated', which makes them a viable candidate for setting
 or updating the timestamps.
 */
SELECT t.table_name
FROM information_schema.tables t
         INNER JOIN information_schema.columns c
                    ON c.table_name = t.table_name AND
                       c.table_schema = t.table_schema
WHERE t.table_schema = 'public'
  AND t.table_type = 'BASE TABLE'
  AND c.column_name IN ('id', 'date_created', 'last_updated')
GROUP BY t.table_name
HAVING COUNT(t.table_name) = 3;

-------------------------------------------------------------------------------

-- Create a helper table holding
/*
 The id, date_created and the name of the source table as a helper table.
 Get the list of table names with the helper-sql at the top of this document.

 To build the helper table, lead of with:
 > CREATE TABLE union_helper_table AS

 Then, for each table the helper-sql returns, create one of these:
 > SELECT id, date_created, '<table_name>' as source FROM <table_name>

 And join them with:
 > UNION ALL
 */
CREATE TABLE union_helper_table AS
SELECT id, date_created, 'merging_set' as source FROM merging_set
UNION ALL
SELECT id, date_created, 'alignment_params' as source FROM alignment_params
UNION ALL
SELECT id, date_created, 'picard_mark_duplicates_metrics' as source FROM picard_mark_duplicates_metrics
UNION ALL
SELECT id, date_created, 'merged_alignment_data_file' as source FROM merged_alignment_data_file
UNION ALL
SELECT id, date_created, 'artefact' as source FROM artefact
UNION ALL
SELECT id, date_created, 'user_project_role' as source FROM user_project_role
UNION ALL
SELECT id, date_created, 'sophia_qc' as source FROM sophia_qc
UNION ALL
SELECT id, date_created, 'config_per_project_and_seq_type' as source FROM config_per_project_and_seq_type
UNION ALL
SELECT id, date_created, 'individual' as source FROM individual
UNION ALL
SELECT id, date_created, 'process' as source FROM process
UNION ALL
SELECT id, date_created, 'wes_server' as source FROM wes_server
UNION ALL
SELECT id, date_created, 'ega_library_selection' as source FROM ega_library_selection
UNION ALL
SELECT id, date_created, 'sample_submission_object' as source FROM sample_submission_object
UNION ALL
SELECT id, date_created, 'processing_step_update' as source FROM processing_step_update
UNION ALL
SELECT id, date_created, 'shutdown_information' as source FROM shutdown_information
UNION ALL
SELECT id, date_created, 'processing_thresholds' as source FROM processing_thresholds
UNION ALL
SELECT id, date_created, 'cluster_job' as source FROM cluster_job
UNION ALL
SELECT id, date_created, 'bam_file_pair_analysis' as source FROM bam_file_pair_analysis
UNION ALL
SELECT id, date_created, 'species_with_strain' as source FROM species_with_strain
UNION ALL
SELECT id, date_created, 'quality_assessment_pass' as source FROM quality_assessment_pass
UNION ALL
SELECT id, date_created, 'keyword' as source FROM keyword
UNION ALL
SELECT id, date_created, 'abstract_quality_assessment' as source FROM abstract_quality_assessment
UNION ALL
SELECT id, date_created, 'swap_info' as source FROM swap_info
UNION ALL
SELECT id, date_created, 'users' as source FROM users
UNION ALL
SELECT id, date_created, 'gene_model' as source FROM gene_model
UNION ALL
SELECT id, date_created, 'project_group' as source FROM project_group
UNION ALL
SELECT id, date_created, 'ega_platform_model' as source FROM ega_platform_model
UNION ALL
SELECT id, date_created, 'seed_me_checksum' as source FROM seed_me_checksum
UNION ALL
SELECT id, date_created, 'seq_scan' as source FROM seq_scan
UNION ALL
SELECT id, date_created, 'document' as source FROM document
UNION ALL
SELECT id, date_created, 'workflow' as source FROM workflow
UNION ALL
SELECT id, date_created, 'reference_genome_index' as source FROM reference_genome_index
UNION ALL
SELECT id, date_created, 'seq_type' as source FROM seq_type
UNION ALL
SELECT id, date_created, 'meta_data_entry' as source FROM meta_data_entry
UNION ALL
SELECT id, date_created, 'fastq_import_instance' as source FROM fastq_import_instance
UNION ALL
SELECT id, date_created, 'meta_data_key' as source FROM meta_data_key
UNION ALL
SELECT id, date_created, 'file_type' as source FROM file_type
UNION ALL
SELECT id, date_created, 'fastqc_processed_file' as source FROM fastqc_processed_file
UNION ALL
SELECT id, date_created, 'reference_genome' as source FROM reference_genome
UNION ALL
SELECT id, date_created, 'alignment_log' as source FROM alignment_log
UNION ALL
SELECT id, date_created, 'decision_mapping' as source FROM decision_mapping
UNION ALL
SELECT id, date_created, 'seq_center' as source FROM seq_center
UNION ALL
SELECT id, date_created, 'seq_platform_group' as source FROM seq_platform_group
UNION ALL
SELECT id, date_created, 'merging_assignment' as source FROM merging_assignment
UNION ALL
SELECT id, date_created, 'audit_log' as source FROM audit_log
UNION ALL
SELECT id, date_created, 'library_preparation_kit' as source FROM library_preparation_kit
UNION ALL
SELECT id, date_created, 'software_tool' as source FROM software_tool
UNION ALL
SELECT id, date_created, 'abstract_bam_file' as source FROM abstract_bam_file
UNION ALL
SELECT id, date_created, 'data_transfer' as source FROM data_transfer
UNION ALL
SELECT id, date_created, 'antibody_target' as source FROM antibody_target
UNION ALL
SELECT id, date_created, 'seq_platform' as source FROM seq_platform
UNION ALL
SELECT id, date_created, 'meta_data_file' as source FROM meta_data_file
UNION ALL
SELECT id, date_created, 'skipped_message' as source FROM skipped_message
UNION ALL
SELECT id, date_created, 'ega_library_strategy' as source FROM ega_library_strategy
UNION ALL
SELECT id, date_created, 'data_file' as source FROM data_file
UNION ALL
SELECT id, date_created, 'parameter' as source FROM parameter
UNION ALL
SELECT id, date_created, 'merging_set_assignment' as source FROM merging_set_assignment
UNION ALL
SELECT id, date_created, 'sequencing_kit_label' as source FROM sequencing_kit_label
UNION ALL
SELECT id, date_created, 'project_request' as source FROM project_request
UNION ALL
SELECT id, date_created, 'sample_identifier' as source FROM sample_identifier
UNION ALL
SELECT id, date_created, 'strain' as source FROM strain
UNION ALL
SELECT id, date_created, 'data_file_submission_object' as source FROM data_file_submission_object
UNION ALL
SELECT id, date_created, 'alignment_pass' as source FROM alignment_pass
UNION ALL
SELECT id, date_created, 'tool_name' as source FROM tool_name
UNION ALL
SELECT id, date_created, 'file_system_changes' as source FROM file_system_changes
UNION ALL
SELECT id, date_created, 'ega_submission' as source FROM ega_submission
UNION ALL
SELECT id, date_created, 'project_role' as source FROM project_role
UNION ALL
SELECT id, date_created, 'import_process' as source FROM import_process
UNION ALL
SELECT id, date_created, 'consistency_check' as source FROM consistency_check
UNION ALL
SELECT id, date_created, 'merging_work_package_alignment_property' as source FROM merging_work_package_alignment_property
UNION ALL
SELECT id, date_created, 'parameter_type' as source FROM parameter_type
UNION ALL
SELECT id, date_created, 'seq_platform_model_label' as source FROM seq_platform_model_label
UNION ALL
SELECT id, date_created, 'workflow_step' as source FROM workflow_step
UNION ALL
SELECT id, date_created, 'processing_step' as source FROM processing_step
UNION ALL
SELECT id, date_created, 'comment' as source FROM comment
UNION ALL
SELECT id, date_created, 'reference_genome_project_seq_type' as source FROM reference_genome_project_seq_type
UNION ALL
SELECT id, date_created, 'qc_threshold' as source FROM qc_threshold
UNION ALL
SELECT id, date_created, 'seq_track' as source FROM seq_track
UNION ALL
SELECT id, date_created, 'tumor_entity' as source FROM tumor_entity
UNION ALL
SELECT id, date_created, 'stat_size_file_name' as source FROM stat_size_file_name
UNION ALL
SELECT id, date_created, 'abstract_merging_work_package' as source FROM abstract_merging_work_package
UNION ALL
SELECT id, date_created, 'sample_type' as source FROM sample_type
UNION ALL
SELECT id, date_created, 'job_decision' as source FROM job_decision
UNION ALL
SELECT id, date_created, 'processed_sai_file' as source FROM processed_sai_file
UNION ALL
SELECT id, date_created, 'log_message' as source FROM log_message
UNION ALL
SELECT id, date_created, 'reference_genome_project_seq_type_alignment_property' as source FROM reference_genome_project_seq_type_alignment_property
UNION ALL
SELECT id, date_created, 'workflow_run' as source FROM workflow_run
UNION ALL
SELECT id, date_created, 'consistency_status' as source FROM consistency_status
UNION ALL
SELECT id, date_created, 'merging_pass' as source FROM merging_pass
UNION ALL
SELECT id, date_created, 'reference_genome_entry' as source FROM reference_genome_entry
UNION ALL
SELECT id, date_created, 'processing_option' as source FROM processing_option
UNION ALL
SELECT id, date_created, 'ilse_submission' as source FROM ilse_submission
UNION ALL
SELECT id, date_created, 'bam_file_submission_object' as source FROM bam_file_submission_object
UNION ALL
SELECT id, date_created, 'sample' as source FROM sample
UNION ALL
SELECT id, date_created, 'role' as source FROM role
UNION ALL
SELECT id, date_created, 'job_definition' as source FROM job_definition
UNION ALL
SELECT id, date_created, 'run' as source FROM run
UNION ALL
SELECT id, date_created, 'document_type' as source FROM document_type
UNION ALL
SELECT id, date_created, 'ega_library_source' as source FROM ega_library_source
UNION ALL
SELECT id, date_created, 'sample_type_per_project' as source FROM sample_type_per_project
UNION ALL
SELECT id, date_created, 'sample_pair' as source FROM sample_pair
UNION ALL
SELECT id, date_created, 'indel_sample_swap_detection' as source FROM indel_sample_swap_detection
UNION ALL
SELECT id, date_created, 'parameter_mapping' as source FROM parameter_mapping
UNION ALL
SELECT id, date_created, 'job_error_definition' as source FROM job_error_definition
UNION ALL
SELECT id, date_created, 'project' as source FROM project
UNION ALL
SELECT id, date_created, 'pipeline' as source FROM pipeline
UNION ALL
SELECT id, date_created, 'species' as source FROM species
UNION ALL
SELECT id, date_created, 'quality_assessment_merged_pass' as source FROM quality_assessment_merged_pass
UNION ALL
SELECT id, date_created, 'merging_log' as source FROM merging_log
UNION ALL
SELECT id, date_created, 'workflow_config' as source FROM workflow_config
UNION ALL
SELECT id, date_created, 'common_name' as source FROM common_name
UNION ALL
SELECT id, date_created, 'aceseq_qc' as source FROM aceseq_qc
UNION ALL
SELECT id, date_created, 'indel_quality_control' as source FROM indel_quality_control
UNION ALL
SELECT id, date_created, 'otrs_ticket' as source FROM otrs_ticket
UNION ALL
SELECT id, date_created, 'merging_criteria' as source FROM merging_criteria
UNION ALL
SELECT id, date_created, 'software_tool_identifier' as source FROM software_tool_identifier
UNION ALL
SELECT id, date_created, 'workflow_artefact' as source FROM workflow_artefact
UNION ALL
SELECT id, date_created, 'processing_error' as source FROM processing_error
UNION ALL
SELECT id, date_created, 'bed_file' as source FROM bed_file
UNION ALL
SELECT id, date_created, 'job_execution_plan' as source FROM job_execution_plan
UNION ALL
SELECT id, date_created, 'project_info' as source FROM project_info
;

-- Create a function to update the timestamps
/*
 It iterates the entire union table and executes updates on all objects, setting the timestamp.
 The timestamp used is always the last non-1970 timestamp, in descending order. Meaning entities
 without a timestamp get the timestamp of the next (based on ID) entity with a valid timestamp.

 Be careful with comparing the timestamps. The format may not be the same for production and your
 local database.
 */
CREATE OR REPLACE FUNCTION fill_timestamps()
    RETURNS void AS
$BODY$
DECLARE
    union_table_cursor CURSOR FOR
        SELECT *
        FROM union_helper_table
        ORDER BY id DESC;
    last_timestamp TIMESTAMP WITH TIME ZONE;
BEGIN
    last_timestamp := '1970-01-01 01:00:00+01s';
    FOR r IN union_table_cursor LOOP
            IF r.date_created = '1970-01-01 01:00:00+01s' THEN
                --raise notice 'UPDATE %s SET date_created = ''%s'' WHERE id = %s', r.source, last_timestamp, r.id;
                EXECUTE format('UPDATE %s SET date_created = ''%s'' WHERE id = %s', r.source, last_timestamp, r.id);
            ELSE
                --raise notice 'change timestamp: %s', r.date_created;
                last_timestamp := r.date_created;
            END IF;
        END LOOP;
END
$BODY$
    LANGUAGE plpgsql;

-- Execute the function
SELECT fill_timestamps();

-- Remove the function again, as we are not going to need it again
DROP FUNCTION fill_timestamps();


-- Special timestamp update for related entities
/*
 Some entities have been determined to be related so closely that it is worth using the
 timestamp determined by the datafiles. These entities are:
   - SeqTrack
   - Sample
   - Individual

 It follows one script for each of these entities, where the earliest date found in
 related DataFiles is used to set the timestamp.

 We restrict the entity selection by a given ID, which has to be determined with the helper
 script below and then added to the WHERE of the three update scripts.
 Look for `0/*[ID]*/` and replace it.

 We execute these after the general timestamp-cleanup to avoid large gaps in the timestamps.
 There are two causes for this:
   - sample swaps, where old data could have been swapped into newer samples, causing the
     timestamp of the sample to be vastly earlier.
   - creation of empty individuals, which was done in the early days of OTP, to cover a
     range of "possible data" to be received. In this case the timestamp would be later than
     it likely would have been.
 */

-- Helper script to determine the cut-off ID for updating SeqTrack, Sample and Individual
/*
 This retrieves the highest entity ID with an unset/1970 dateCreated. This is the value to use
 instead of `0/*[ID]*/`.
 Also consider the timestamp format again, if it should not work.
 */
SELECT MAX(id) FROM union_helper_table
WHERE date_created = timestamp '1970-01-01 00:00:00';


-- Update Timestamps of SeqTracks based on DataFiles
WITH query (st_id, df_date) AS (
    SELECT seq_track.id,
           MIN(data_file.date_created)
    FROM data_file
             JOIN seq_track ON data_file.seq_track_id = seq_track.id
    GROUP BY seq_track.id
)
UPDATE seq_track
SET date_created = query.df_date
FROM query
WHERE
        query.st_id = seq_track.id AND
        id < 0/*[ID]*/;

-- Update Timestamps of Samples based on DataFiles
WITH query (s_id, df_date) AS (
    SELECT sample.id,
           MIN(data_file.date_created)
    FROM data_file
             JOIN seq_track ON data_file.seq_track_id = seq_track.id
             JOIN sample ON seq_track.sample_id = sample.id
    GROUP BY sample.id
)
UPDATE sample
SET date_created = query.df_date
FROM query
WHERE
        query.s_id = sample.id AND
        id < 0/*[ID]*/;

-- Update Timestamps of Individuals based on DataFiles
WITH query (i_id, df_date) AS (
    SELECT individual.id,
           MIN(data_file.date_created)
    FROM data_file
             JOIN seq_track ON data_file.seq_track_id = seq_track.id
             JOIN sample ON seq_track.sample_id = sample.id
             JOIN individual ON sample.individual_id = individual.id
    GROUP BY individual.id
)
UPDATE individual
SET date_created = query.df_date
FROM query
WHERE
        query.i_id = individual.id AND
        id < 0/*[ID]*/;


-- Remove the helper table
DROP TABLE union_helper_table;
