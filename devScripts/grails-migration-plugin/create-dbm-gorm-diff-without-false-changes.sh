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


# This scripts creates a database migration script from the gorm to defined database
# since the tool generates much false positives, they are removed afterwards
# see the comments on the regular expression for more details
# for simpler debugging, the result of each step is kept in a tmp directory

set -e

if [ ! "$1" ]; then
    echo "Please provide a filename (without suffix)"
    exit 1
fi

# see http://grails-plugins.github.io/grails-database-migration/1.4.0/ref/Diff%20Scripts/dbm-gorm-diff.html for reference
# $1 is the filename

#variables
year=`date +'%Y'`
changelogPath="migrations/changelogs/${year}/${1}.groovy"
tmpDir="tmp"

prefixChangeSet="\n\s{4}changeSet.*\n\s{8}"
suffixChangeSet=".*\n\s{4}\}\n"

infixChangeSet="${suffixChangeSet}${prefixChangeSet}"
prefix="s/${prefixChangeSet}"
suffix="${suffixChangeSet}//g"

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
(job_error_definition_job_definition)|\
(job_error_definition_job_error_definition)|\
(library_preparation_kit_import_alias)|\
(merging_work_package_seq_track)|\
(project_request_keywords)|\
(project_request_seq_type)|\
(project_request_project_request_user)|\
(project_request_user_project_role)|\
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
(sophia_instance_roddy_execution_directory_names)|\
(text_field_definition_allowed_text_values)|\
(user_project_role_project_role)|\
(workflow_command_log)|\
(workflow_message_log)|\
"

#contains false positive unique constraint
addUniqueConstraints="\
(UC_ABSTRACT_BAM_FILEALIGNMENT_PASS_ID_COL)|\
(UC_ABSTRACT_BAM_FILEMERGING_PASS_ID_COL)|\
"

dropUniqueConstraints="\
(seq_type)|\
"

#create directories
mkdir -p migrations/changelogs/${year} ${tmpDir}

#create diff and cleanup false positives
bash gradlew --build-cache dbmGormDiff -q | \
  tee ${tmpDir}/migration-step01.groovy | \

  # delete anything before the line with "databaseChangeLog = {"
  # that's mostly logger output, which make the file invalid
  sed -e/databaseChangeLog\ \=\ \{/\{ -e:1 -en\;b1 -e\} -ed | \
  tee ${tmpDir}/migration-step02.groovy | \

  # there is a sequence adaption, which shouldn't be there
  perl -0p -e "${prefix}alterSequence${suffix}" | \
  tee ${tmpDir}/migration-step03.groovy | \

  # on the m2m tables we add some index and foreign keys, which should stay
  perl -0p -e "${prefix}((dropPrimaryKey)|(dropNotNullConstraint)|(dropUniqueConstraint)|(dropForeignKeyConstraint)).*ableName: \"(${joinTables})\"${suffix}"  | \
  tee ${tmpDir}/migration-step04.groovy | \

  #for many index the migration creates a dropped follow by an create, that will be removed
  perl -0p -e "${prefix}dropIndex${infixChangeSet}createIndex.*\{(\n\s{12}column.*\n)+\s{8}\}${suffix}" | \
  tee ${tmpDir}/migration-step05.groovy | \

  #for many uniqueConstraint the migration creates a dropped follow by an create, that will be removed
  perl -0p -e "${prefix}dropUniqueConstraint${infixChangeSet}addUniqueConstraint${suffix}" | \
  tee ${tmpDir}/migration-step06.groovy | \

  #the migration creates adding of unique constraints already exist. They are removed by an explicit false positive list
  perl -0p -e "${prefix}addUniqueConstraint.*constraintName.*\"(${addUniqueConstraints})\"${suffix}" | \
  tee ${tmpDir}/migration-step07.groovy | \

  #the migration creates dropping of unique constraints which should exist. They are removed by an explicit false positive list
  perl -0p -e "${prefix}dropUniqueConstraint.*constraintName.*\"(${dropUniqueConstraints})\"${suffix}" | \
  tee ${tmpDir}/migration-step08.groovy | \

  #delete local usernames
  perl -0p -e "s/author: \".*?\"/author: \"\"/g"  | \
  tee ${tmpDir}/migration-step09.groovy | \

  #delete empty changelog
  perl -0p -e "s/databaseChangeLog = \{\n\}\n//g" | \
  tee ${tmpDir}/migration-step10.groovy $changelogPath

if [ ! -s $changelogPath ]; then
    rm $changelogPath
    echo "No changes found, file was automatically deleted"
else
    perl -0pi -e "s/\t/    /g" $changelogPath
    echo "Created file '$changelogPath'"
fi
