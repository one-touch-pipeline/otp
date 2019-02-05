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
    echo "Please provide a filename (without suffix) and optionally '--add'"
    exit 1
fi

if [ "$2" -a "$2" != "--add" ]; then
    echo "Unknown argument '$2'"
    exit 1
fi

# see http://grails-plugins.github.io/grails-database-migration/1.4.0/ref/Diff%20Scripts/dbm-gorm-diff.html for reference
# $1 is the filename
# $2 additional parameter --add, to add directly to changelog.groovy
year=`date +'%Y'`
#dbm-gorm-diff adds automatically the directory 'migrations', but mkdir not
mkdir -p migrations/changelogs/${year}
grails dbm-gorm-diff changelogs/${year}/${1}.groovy $2


prefix="s/\n\tchangeSet.*\n\t\t"
suffix=".*\n\t\}\n//g"
changelogPath="migrations/changelogs/${year}/${1}.groovy"
indices="(_class_idx)|(config_per_project_project_seqtype_pipeline_individual_idx)"
tables="(job_error_definition_job_error_definition)|(aceseq_instance_roddy_execution_directory_names)|(externally_processed_merged_bam_file_further_files)|\
(import_process_externally_processed_merged_bam_file)|(merging_work_package_seq_track)|(indel_calling_instance_roddy_execution_directory_names)|\
(job_error_definition_job_definition)|(roddy_bam_file_seq_track)|(roddy_snv_calling_instance_roddy_execution_directory_names)|\
(roddy_bam_file_roddy_execution_directory_names)|(seq_track_log_message)|(seq_platform_group_comment)|(single_cell_bam_file_seq_track)|\
(sophia_instance_roddy_execution_directory_names)|(swap_info_seq_track)|(seq_platform_model_label_import_alias)|(seq_type_import_alias)|\
(sequencing_kit_label_import_alias)|(antibody_target_import_alias)|(library_preparation_kit_import_alias)|(seq_center_import_dirs_allow_linking)|\
(ega_submission_bam_file_submission_object)|(ega_submission_data_file_submission_object)|(ega_submission_sample_submission_object)"

perl -0pi -e "${prefix}.*\"meta_data_key\".*\n.*\n${suffix}" $changelogPath
perl -0pi -e "${prefix}.*file_system_changes_idx${suffix}" $changelogPath
perl -0pi -e "${prefix}dropIndex\(.*(${indices})\"${suffix}" $changelogPath
perl -0pi -e "${prefix}modifyDataType.*newDataType: \"((boolean)|(int4))\"${suffix}" $changelogPath
perl -0pi -e "${prefix}.*ableName: \"(${tables})\"${suffix}" $changelogPath
perl -0pi -e "s/databaseChangeLog = \{\n\}\n//g" $changelogPath

if [ ! -s $changelogPath ]; then
    if [ "$2" ]; then
        perl -0pi -e "s/\n\tinclude file: \'changelogs\/${year}\/${1}.groovy\'\n//g" migrations/changelog.groovy
    fi
    rm $changelogPath
    echo "No changes found, file was automatically deleted"
else
    perl -0pi -e "s/\t/    /g" $changelogPath
    echo "Created file '$changelogPath'"
    if [ "$2" ]; then
        perl -0pi -e "s/\t/    /g" migrations/changelog.groovy
        echo "Added file to 'migrations/changelog.groovy'"
    fi
fi
