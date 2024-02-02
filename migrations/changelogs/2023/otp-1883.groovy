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

    changeSet(author: "-", id: "1683287901299-131") {
        sql("ALTER TABLE quality_assessment_merged_pass RENAME abstract_merged_bam_file_id TO abstract_bam_file_id;")
    }

    changeSet(author: "-", id: "1683287901299-132") {
        dropColumn(columnName: "file_exists", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1683287901299-133") {
        dropColumn(columnName: "has_coverage_plot", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1683287901299-134") {
        dropColumn(columnName: "has_index_file", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1683287901299-135") {
        dropColumn(columnName: "has_insert_size_plot", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1683287901299-136") {
        dropColumn(columnName: "has_metrics_file", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1683287901299-137") {
        dropColumn(columnName: "type", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1683287901299-7") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "file_operation_status", tableName: "abstract_bam_file", validate: "true")
    }

    changeSet(author: "-", id: "1683287901299-8") {
        addNotNullConstraint(columnDataType: "bigint", columnName: "file_size", tableName: "abstract_bam_file", validate: "true")
    }

    changeSet(author: "-", id: "1683287901299-25") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "qc_traffic_light_status", tableName: "abstract_bam_file", validate: "true")
    }

    changeSet(author: "-", id: "1683287901299-46") {
        addNotNullConstraint(columnDataType: "bigint", columnName: "work_package_id", tableName: "abstract_bam_file", validate: "true")
    }

    changeSet(author: "-", id: "1683287901299-47") {
        sql("UPDATE abstract_bam_file SET class = 'de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile' WHERE class = 'de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile'")
        sql("ALTER TABLE externally_processed_merged_bam_file_further_files RENAME externally_processed_merged_bam_file_id TO externally_processed_bam_file_id;")
        sql("ALTER TABLE externally_processed_merged_bam_file_further_files RENAME TO externally_processed_bam_file_further_files;")
        sql("ALTER TABLE import_process_externally_processed_merged_bam_file RENAME externally_processed_merged_bam_file_id TO externally_processed_bam_file_id;")
        sql("ALTER TABLE import_process_externally_processed_merged_bam_file RENAME import_process_externally_processed_merged_bam_files_id TO import_process_externally_processed_bam_files_id;")
        sql("ALTER TABLE import_process_externally_processed_merged_bam_file RENAME TO import_process_externally_processed_bam_file;")
    }

    changeSet(author: "-", id: "1683287901299-48") {
        sql("UPDATE abstract_quality_assessment SET class = 'de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFileQualityAssessment' WHERE class = 'de.dkfz.tbi.otp.dataprocessing.ExternalProcessedMergedBamFileQualityAssessment'")
    }
}
