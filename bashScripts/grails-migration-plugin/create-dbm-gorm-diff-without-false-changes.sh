#!/bin/bash

# Copyright 2011-2019 The OTP authors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

set -e

if [ ! "$1" ]; then
    echo "Please provide a filename (without suffix)"
    exit 1
fi

# see http://grails-plugins.github.io/grails-database-migration/1.4.0/ref/Diff%20Scripts/dbm-gorm-diff.html for reference
# $1 is the filename
year=`date +'%Y'`
#dbm-gorm-diff adds automatically the directory 'migrations', but mkdir does not
mkdir -p migrations/changelogs/${year}
bash gradlew dbmGormDiff -q > migrations/changelogs/${year}/${1}.groovy

prefix="s/\n\s{4}changeSet.*\n\s{8}"
suffix=".*\n\s{4}\}\n//g"
changelogPath="migrations/changelogs/${year}/${1}.groovy"

joinTables="\
(aceseq_instance_roddy_execution_directory_names)|\
(antibody_target_import_alias)|\
(cluster_job_cluster_job)|\
(date_field_definition_allowed_date_values)|\
(decimal_number_field_definition_allowed_decimal_number_values)|\
(ega_library_selection_library_preparation_kit)|\
(ega_library_source_seq_type)|\
(ega_library_strategy_seq_type)|\
(ega_platform_model_seq_platform_model_label)|\
(ega_submission_bam_file_submission_object)|\
(ega_submission_data_file_submission_object)|\
(ega_submission_sample_submission_object)|\
(externally_processed_merged_bam_file_further_files)|\
(import_process_externally_processed_merged_bam_file)|\
(indel_calling_instance_roddy_execution_directory_names)|\
(integer_field_definition_allowed_integer_values)|\
(job_definition_parameter)|\
(job_error_definition_job_definition)|\
(job_error_definition_job_error_definition)|\
(library_preparation_kit_import_alias)|\
(merging_work_package_seq_track)|\
(project_request_bioinformatician)|\
(project_request_keywords)|\
(project_request_lead_bioinformatician)|\
(project_request_seq_type)|\
(project_request_submitter)|\
(roddy_bam_file_roddy_execution_directory_names)|\
(roddy_bam_file_seq_track)|\
(roddy_snv_calling_instance_roddy_execution_directory_names)|\
(seq_center_import_dirs_allow_linking)|\
(seq_platform_group_comment)|\
(seq_platform_model_label_import_alias)|\
(seq_track_log_message)|\
(seq_type_import_alias)|\
(sequencing_kit_label_import_alias)|\
(single_cell_bam_file_seq_track)|\
(software_tool_program_name_program_version_type_key)|\
(sophia_instance_roddy_execution_directory_names)|\
(text_field_definition_allowed_text_values)|\
(workflow_command_log)|\
(workflow_message_log)|\
(workflow_run_workflow_config)|\
(workflow_step_workflow_log)|\
"
perl -0pi -e "${prefix}((dropPrimaryKey)|(dropNotNullConstraint)|(dropUniqueConstraint)|(dropForeignKeyConstraint)).*ableName: \"(${joinTables})\"${suffix}" $changelogPath

perl -0pi -e "${prefix}dropIndex.*\n\n\s{8}createIndex.*\{(\n\s{12}column.*\n)+\s{8}\}${suffix}" $changelogPath
perl -0pi -e "${prefix}dropUniqueConstraint.*\n\n\s{8}addUniqueConstraint${suffix}" $changelogPath
perl -0pi -e "${prefix}addUniqueConstraint.*columnNames: \"((alignment_pass_id)|(merging_pass_id))\".*tableName: \"abstract_bam_file\"${suffix}" $changelogPath
perl -0pi -e "s/databaseChangeLog = \{\n\}\n//g" $changelogPath
# delete local usernames
perl -0pi -e "s/author: \".*?\"/author: \"\"/g" $changelogPath
# delete anything before the line with "databaseChangeLog = {"
sed -i -e/databaseChangeLog\ \=\ \{/\{ -e:1 -en\;b1 -e\} -ed $changelogPath

if [ ! -s $changelogPath ]; then
    rm $changelogPath
    echo "No changes found, file was automatically deleted"
else
    perl -0pi -e "s/\t/    /g" $changelogPath
    echo "Created file '$changelogPath'"
fi
