databaseChangeLog = {

    changeSet(author: "kosnac (generated)", id: "1552563073922-7") {
        addUniqueConstraint(columnNames: "indel_calling_instance_id", constraintName: "UC_INDEL_QUALITY_CONTROLINDEL_CALLING_INSTANCE_ID_COL", tableName: "indel_quality_control")
    }

    changeSet(author: "kosnac (generated)", id: "1552563073922-10") {
        addUniqueConstraint(columnNames: "process_id", constraintName: "UC_PROCESS_PARAMETERPROCESS_ID_COL", tableName: "process_parameter")
    }

    changeSet(author: "kosnac (generated)", id: "1552563073922-12") {
        addUniqueConstraint(columnNames: "phabricator_alias", constraintName: "UC_PROJECTPHABRICATOR_ALIAS_COL", tableName: "project")
    }

    changeSet(author: "kosnac (generated)", id: "1552563073922-27") {
        addUniqueConstraint(columnNames: "reference_genome_id, name", constraintName: "UK5907adb0f38a1b425be48b9e0357", tableName: "stat_size_file_name")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-3") {
        addUniqueConstraint(columnNames: "class", constraintName: "UC_ACL_CLASSCLASS_COL", tableName: "acl_class")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-4") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_ANTIBODY_TARGETNAME_COL", tableName: "antibody_target")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-5") {
        addUniqueConstraint(columnNames: "config_file_path", constraintName: "UC_CONFIG_PER_PROJECT_AND_SEQ_TYPECONFIG_FILE_PATH_COL", tableName: "config_per_project_and_seq_type")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-6") {
        addUniqueConstraint(columnNames: "ilse_number", constraintName: "UC_ILSE_SUBMISSIONILSE_NUMBER_COL", tableName: "ilse_submission")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-7") {
        addUniqueConstraint(columnNames: "indel_calling_instance_id", constraintName: "UC_INDEL_SAMPLE_SWAP_DETECTIONINDEL_CALLING_INSTANCE_ID_COL", tableName: "indel_sample_swap_detection")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-8") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_LIBRARY_PREPARATION_KITNAME_COL", tableName: "library_preparation_kit")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-9") {
        addUniqueConstraint(columnNames: "dir_name", constraintName: "UC_PROJECTDIR_NAME_COL", tableName: "project")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-10") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_PROJECT_GROUPNAME_COL", tableName: "project_group")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-11") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_PROJECT_ROLENAME_COL", tableName: "project_role")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-12") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_REFERENCE_GENOMENAME_COL", tableName: "reference_genome")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-13") {
        addUniqueConstraint(columnNames: "relative_path", constraintName: "UC_SAMPLE_PAIRRELATIVE_PATH_COL", tableName: "sample_pair")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-14") {
        addUniqueConstraint(columnNames: "auto_import_dir", constraintName: "UC_SEQ_CENTERAUTO_IMPORT_DIR_COL", tableName: "seq_center")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-15") {
        addUniqueConstraint(columnNames: "sophia_instance_id", constraintName: "UC_SOPHIA_QCSOPHIA_INSTANCE_ID_COL", tableName: "sophia_qc")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-16") {
        addUniqueConstraint(columnNames: "library_preparation_kit_id, reference_genome_id", constraintName: "UK195a23ffbe4428ba7eaaccb47645", tableName: "bed_file")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-17") {
        addUniqueConstraint(columnNames: "single_cell, library_layout, dir_name", constraintName: "UK39c048627a9699768034f800dfe3", tableName: "seq_type")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-18") {
        addUniqueConstraint(columnNames: "reference_genome_project_seq_type_id, name", constraintName: "UK3a7102bd07e8f7e38140507ce831", tableName: "reference_genome_project_seq_type_alignment_property")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-19") {
        addUniqueConstraint(columnNames: "individual_id, sample_type_id", constraintName: "UK3a9b2b49148a72946ea7922dec50", tableName: "sample")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-20") {
        addUniqueConstraint(columnNames: "abstract_merged_bam_file_id, identifier", constraintName: "UK3c476766119bb42f2680ccfbe2b1", tableName: "quality_assessment_merged_pass")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-21") {
        addUniqueConstraint(columnNames: "sequencing_kit_label_id, seq_platform_model_label_id, name", constraintName: "UK4a1aabf09bb3f07bedfc932f16d4", tableName: "seq_platform")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-22") {
        addUniqueConstraint(columnNames: "merging_work_package_id, identifier", constraintName: "UK4fe362d3b753414fa9b1a80b6720", tableName: "merging_set")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-23") {
        addUniqueConstraint(columnNames: "sample_pair_id, instance_name", constraintName: "UK5e2770429f37f8f83af98ded5741", tableName: "bam_file_pair_analysis")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-24") {
        addUniqueConstraint(columnNames: "reference_genome_id, file_name", constraintName: "UK90f1dddc247b084bf6ea7df25813", tableName: "bed_file")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-25") {
        addUniqueConstraint(columnNames: "seq_track_id, identifier", constraintName: "UK9f522e63c97479850e5d9b81286f", tableName: "alignment_pass")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-26") {
        addUniqueConstraint(columnNames: "aceseq_instance_id, number", constraintName: "UKa1bacb088b69505ab70013e79694", tableName: "aceseq_qc")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-27") {
        addUniqueConstraint(columnNames: "project_id, sample_type_id", constraintName: "UKa74a5644b48ab8b66ceb6d552ff2", tableName: "sample_type_per_project")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-28") {
        addUniqueConstraint(columnNames: "processed_bam_file_id, identifier", constraintName: "UKcb6a815f64f1f4ab82d06a967434", tableName: "quality_assessment_pass")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-29") {
        addUniqueConstraint(columnNames: "reference_genome_id, name", constraintName: "UKeed0aae143d64f14fcf94553c9ec", tableName: "reference_genome_entry")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-30") {
        addUniqueConstraint(columnNames: "merging_set_id, identifier", constraintName: "UKef39ad3edbe91ab8ebbcfc0882ec", tableName: "merging_pass")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-31") {
        addUniqueConstraint(columnNames: "merging_work_package_id, name", constraintName: "UKf0a2c354ea7a498e07eb5d008dd5", tableName: "merging_work_package_alignment_property")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-32") {
        addUniqueConstraint(columnNames: "reference_genome_id, alias", constraintName: "UKf4aa9c070dc37d9d875cb6f7e5ca", tableName: "reference_genome_entry")
    }

    changeSet(author: "kosnac (generated)", id: "1552578038413-33") {
        addUniqueConstraint(columnNames: "single_cell, library_layout, name", constraintName: "UKf73fa0246b0db85b4ab1817fa80a", tableName: "seq_type")
    }
}
