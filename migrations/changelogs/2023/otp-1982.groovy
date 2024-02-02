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

databaseChangeLog = {
    changeSet(author: "-", id: "1678154402851-1") {
        sql("""
            ALTER TABLE data_file RENAME TO raw_sequence_file;
            ALTER TABLE raw_sequence_file ADD COLUMN class VARCHAR(255);
            UPDATE raw_sequence_file SET class = 'de.dkfz.tbi.otp.ngsdata.FastqFile';
            ALTER TABLE raw_sequence_file ALTER COLUMN class SET NOT NULL;
            ALTER TABLE raw_sequence_file RENAME COLUMN md5sum TO fastq_md5sum;

            ALTER TABLE fastqc_processed_file RENAME COLUMN data_file_id TO sequence_file_id;
            ALTER TABLE meta_data_entry RENAME COLUMN data_file_id TO sequence_file_id;

            ALTER TABLE data_file_submission_object RENAME TO raw_sequence_file_submission_object;
            ALTER TABLE raw_sequence_file_submission_object RENAME COLUMN data_file_id TO sequence_file_id;

            ALTER TABLE ega_submission_data_file_submission_object RENAME TO ega_submission_raw_sequence_file_submission_object;
            ALTER TABLE ega_submission_raw_sequence_file_submission_object RENAME COLUMN data_file_submission_object_id TO raw_sequence_file_submission_object_id;
            ALTER TABLE ega_submission_raw_sequence_file_submission_object RENAME COLUMN ega_submission_data_files_to_submit_id TO ega_submission_raw_sequence_files_to_submit_id;
            
            ALTER INDEX data_file_run_idx RENAME TO raw_sequence_file_run_idx;
            ALTER INDEX data_file_project_idx RENAME TO raw_sequence_file_project_idx;
            ALTER INDEX data_file_fastq_import_instance_idx RENAME TO raw_sequence_file_fastq_import_instance_idx;
            ALTER INDEX data_file_seq_track_idx RENAME TO raw_sequence_file_seq_track_idx;
            ALTER INDEX data_file_md5sum_idx RENAME TO raw_sequence_file_fastq_md5sum_idx;
            ALTER INDEX data_file_file_type_idx RENAME TO raw_sequence_file_file_type_idx;
            ALTER INDEX data_file_date_last_checked_idx RENAME TO raw_sequence_file_date_last_checked_idx;
        """)
    }

    changeSet(author: "-", id: "1678154402851-87") {
        addColumn(tableName: "raw_sequence_file") {
            column(name: "cram_md5sum", type: "varchar(255)")
        }
    }

    changeSet(author: "-", id: "1678154402851-89") {
        addColumn(tableName: "raw_sequence_file") {
            column(name: "reference_genome_id", type: "int8")
        }
    }

    changeSet(author: "-", id: "1678154402851-90") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_id", baseTableName: "raw_sequence_file", constraintName: "FK5cc2skclkhbhafvjdaq3tmwy2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome", validate: "true")
    }
}
