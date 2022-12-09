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
(job_definition_parameter)|\
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
(software_tool_program_name_program_version_type_key)|\
(sophia_instance_roddy_execution_directory_names)|\
(text_field_definition_allowed_text_values)|\
(user_project_role_project_role)|\
(workflow_command_log)|\
(workflow_message_log)|\
(workflow_run_workflow_config)|\
(workflow_step_workflow_log)|\
"

#contains false positive unique constraint
uniqueConstraints="\
(UC_ABSTRACT_BAM_FILEALIGNMENT_PASS_ID_COL)|\
(UC_ABSTRACT_BAM_FILEMERGING_PASS_ID_COL)|\
(UC_ABSTRACT_FIELD_DEFINITIONNAME_COL)|\
(UC_ACL_CLASSCLASS_COL)|\
(UC_ANTIBODY_TARGETNAME_COL)|\
(UC_BAM_FILE_SUBMISSION_OBJECTEGA_ALIAS_NAME_COL)|\
(UC_CONFIG_PER_PROJECT_AND_SEQ_TYPECONFIG_FILE_PATH_COL)|\
(UC_DATA_FILE_SUBMISSION_OBJECTEGA_ALIAS_NAME_COL)|\
(UC_DECISION_MAPPINGDECISION_ID_COL)|\
(UC_DOCUMENT_TYPETITLE_COL)|\
(UC_EGA_LIBRARY_SELECTIONLIBRARY_SELECTION_EGA_NAME_COL)|\
(UC_EGA_LIBRARY_STRATEGYLIBRARY_STRATEGY_EGA_NAME_COL)|\
(UC_EGA_PLATFORM_MODELPLATFORM_MODEL_EGA_NAME_COL)|\
(UC_EXTERNAL_WORKFLOW_CONFIG_FRAGMENTNAME_COL)|\
(UC_EXTERNAL_WORKFLOW_CONFIG_SELECTORNAME_COL)|\
(UC_FASTQC_PROCESSED_FILEDATA_FILE_ID_COL)|\
(UC_ILSE_SUBMISSIONILSE_NUMBER_COL)|\
(UC_INDEL_QUALITY_CONTROLINDEL_CALLING_INSTANCE_ID_COL)|\
(UC_INDEL_SAMPLE_SWAP_DETECTIONINDEL_CALLING_INSTANCE_ID_COL)|\
(UC_INDIVIDUALPID_COL)|\
(UC_KEYWORDNAME_COL)|\
(UC_LIBRARY_PREPARATION_KITNAME_COL)|\
(UC_LIBRARY_PREPARATION_KITSHORT_DISPLAY_NAME_COL)|\
(UC_OTRS_TICKETTICKET_NUMBER_COL)|\
(UC_PIPELINENAME_COL)|\
(UC_PROCESSING_PRIORITYNAME_COL)|\
(UC_PROCESSING_PRIORITYPRIORITY_COL)|\
(UC_PROCESSRESTARTED_ID_COL)|\
(UC_PROJECTDIR_NAME_COL)|\
(UC_PROCESS_PARAMETERPROCESS_ID_COL)|\
(UC_PROJECT_GROUPNAME_COL)|\
(UC_PROJECT_REQUESTNAME_COL)|\
(UC_PROJECT_ROLENAME_COL)|\
(UC_PROJECTNAME_COL)|\
(UC_REALMNAME_COL)|\
(UC_REFERENCE_GENOMENAME_COL)|\
(UC_REFERENCE_GENOMEPATH_COL)|\
(UC_ROLEAUTHORITY_COL)|\
(UC_RUNNAME_COL)|\
(UC_SAMPLE_IDENTIFIERNAME_COL)|\
(UC_SAMPLE_SUBMISSION_OBJECTEGA_ALIAS_NAME_COL)|\
(UC_SAMPLE_TYPENAME_COL)|\
(UC_SEQUENCING_KIT_LABELNAME_COL)|\
(UC_SEQ_CENTERAUTO_IMPORT_DIR_COL)|\
(UC_SEQ_CENTERDIR_NAME_COL)|\
(UC_SEQ_CENTERNAME_COL)|\
(UC_SEQ_PLATFORM_MODEL_LABELNAME_COL)|\
(UC_SOPHIA_QCSOPHIA_INSTANCE_ID_COL)|\
(UC_STRAINNAME_COL)|\
(UC_TOOL_NAMENAME_COL)|\
(UC_TOOL_NAMEPATH_COL)|\
(UC_TUMOR_ENTITYNAME_COL)|\
(UC_WORKFLOWNAME_COL)|\
(UC_WORKFLOW_RUNRESTARTED_FROM_ID_COL)|\
(UC_WORKFLOW_STEPRESTARTED_FROM_ID_COL)|\
(UK177311e38d02354891bf47dcaf9f)|\
(UK1781b9a084dff171b580608b3640)|\
(UK195a23ffbe4428ba7eaaccb47645)|\
(UK1efb073cf902dd70d6631b67b564)|\
(UK324f84fe9e2682e71201316081b9)|\
(UK39c048627a9699768034f800dfe3)|\
(UK3a7102bd07e8f7e38140507ce831)|\
(UK3a9b2b49148a72946ea7922dec50)|\
(UK3c476766119bb42f2680ccfbe2b1)|\
(UK45f1295304fe910bfac1acd34670)|\
(UK4f777b5a5910a2d7d4edab3aea9f)|\
(UK4fe362d3b753414fa9b1a80b6720)|\
(UK523bd3e9030ca0c3c992770d0231)|\
(UK56103a82abb455394f8c97a95587)|\
(UK5907adb0f38a1b425be48b9e0357)|\
(UK5e2770429f37f8f83af98ded5741)|\
(UK5ed1d94d1d3e07b21bade12682b0)|\
(UK63059ec3b8a393cb2addcc0c5e7a)|\
(UK6615f9e2cd25d67dcf418f344e6b)|\
(UK6cb454c8bcac6b4d804f975198d6)|\
(UK79e9db6b093294dfe0036ab264a4)|\
(UK8f99bb491b7d0b82687c43f660f3)|\
(UK90f1dddc247b084bf6ea7df25813)|\
(UK986b4059d08cedff89f0f334bb5f)|\
(UK9f522e63c97479850e5d9b81286f)|\
(UKa1bacb088b69505ab70013e79694)|\
(UKa74a5644b48ab8b66ceb6d552ff2)|\
(UKb7a01d2be3fdbf83d79c75252adb)|\
(UKc1f0d44a5da068a763bc37f8fa69)|\
(UKc8144e05783d99c4c427a2106888)|\
(UKcabae7dda3e0a9f765ad6d9d5453)|\
(UKcb6a815f64f1f4ab82d06a967434)|\
(UKcb8bd959752a92ddf76fef03121d)|\
(UKce200ed06800e5a163c6ab6c0c85)|\
(UKd0ffe3452b1e6297e17368cdd0b2)|\
(UKeed0aae143d64f14fcf94553c9ec)|\
(UKef39ad3edbe91ab8ebbcfc0882ec)|\
(UKf0a2c354ea7a498e07eb5d008dd5)|\
(UKf4aa9c070dc37d9d875cb6f7e5ca)|\
(UKf73fa0246b0db85b4ab1817fa80a)|\
(UKfe31b46979536931e6054227f749)|\
(UK5a0d148daef7ad278ef33a7aff20)|\
"

#create directories
mkdir -p migrations/changelogs/${year} ${tmpDir}

#create diff and cleanup false positives
bash gradlew --build-cache dbmGormDiff -q | \
  tee ${tmpDir}/migration-step01.groovy | \

  # delete anything before the line with "databaseChangeLog = {"
  # thats mostly logger output, which make the file invalid
  sed -e/databaseChangeLog\ \=\ \{/\{ -e:1 -en\;b1 -e\} -ed | \
  tee ${tmpDir}/migration-step02.groovy | \

  # there is a sequence adaption, which shouldn't be there
  perl -0p -e "${prefix}alterSequence${suffix}" | \
  tee ${tmpDir}/migration-step03.groovy | \

  # on the m2m tables we add some index and foreign keys, which should stay
  perl -0p -e "${prefix}((dropPrimaryKey)|(dropNotNullConstraint)|(dropUniqueConstraint)|(dropForeignKeyConstraint)).*ableName: \"(${joinTables})\"${suffix}"  | \
  tee ${tmpDir}/migration-step04.groovy | \

  #for many index the migration creates a droped follow by an create, that will be removed
  perl -0p -e "${prefix}dropIndex${infixChangeSet}createIndex.*\{(\n\s{12}column.*\n)+\s{8}\}${suffix}" | \
  tee ${tmpDir}/migration-step05.groovy | \

  #for many uniqueConstraint the migration creates a droped follow by an create, that will be removed
  perl -0p -e "${prefix}dropUniqueConstraint${infixChangeSet}addUniqueConstraint${suffix}" | \
  tee ${tmpDir}/migration-step06.groovy | \

  #the migration creates adding of unique constraints already exist. They are removed by an explicit false positive list
  perl -0p -e "${prefix}addUniqueConstraint.*constraintName.*\"(${uniqueConstraints})\"${suffix}" | \
  tee ${tmpDir}/migration-step07.groovy | \

  #delete local usernames
  perl -0p -e "s/author: \".*?\"/author: \"\"/g"  | \
  tee ${tmpDir}/migration-step08.groovy | \

  #delete empty changelog
  perl -0p -e "s/databaseChangeLog = \{\n\}\n//g" | \
  tee ${tmpDir}/migration-step09.groovy $changelogPath

if [ ! -s $changelogPath ]; then
    rm $changelogPath
    echo "No changes found, file was automatically deleted"
else
    perl -0pi -e "s/\t/    /g" $changelogPath
    echo "Created file '$changelogPath'"
fi
