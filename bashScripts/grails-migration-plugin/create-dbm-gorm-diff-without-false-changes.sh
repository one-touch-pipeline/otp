#!/bin/bash

set -e

if [ ! "$1" ]; then
echo "Please provide a filename."
exit 1
fi

# see http://grails-plugins.github.io/grails-database-migration/1.4.0/ref/Diff%20Scripts/dbm-gorm-diff.html for reference
# $1 is the filename
# $2 additional parameter --add, to add directly to changelog.groovy
year=`date +'%Y'`
mkdir -p changelogs/${year}
grails dbm-gorm-diff changelogs/${year}/${1}.groovy $2


prefix="s/\n\tchangeSet.*\n\t\t"
suffix=".*\n\t\}\n//g"
changelogPath="migrations/changelogs/${year}/${1}.groovy"
indices="(_class_idx)|(config_per_project_project_seqtype_pipeline_individual_idx)"
tables="(job_error_definition_job_error_definition)|(aceseq_instance_roddy_execution_directory_names)|(externally_processed_merged_bam_file_further_files)|(import_process_externally_processed_merged_bam_file)|(merging_work_package_seq_track)|(indel_calling_instance_roddy_execution_directory_names)|(job_error_definition_job_definition)|(roddy_bam_file_seq_track)|(roddy_snv_calling_instance_roddy_execution_directory_names)|(roddy_bam_file_roddy_execution_directory_names)|(seq_track_log_message)|(seq_platform_group_comment)|(sophia_instance_roddy_execution_directory_names)|(swap_info_seq_track)"

perl -0pi -e "${prefix}.*\"meta_data_key\".*\n.*\n${suffix}" $changelogPath
perl -0pi -e "${prefix}.*file_system_changes_idx${suffix}" $changelogPath
perl -0pi -e "${prefix}dropIndex\(.*(${indices})\"${suffix}" $changelogPath
perl -0pi -e "${prefix}modifyDataType.*newDataType: \"((boolean)|(int4))\"${suffix}" $changelogPath
perl -0pi -e "${prefix}.*ableName: \"(${tables})\"${suffix}" $changelogPath
