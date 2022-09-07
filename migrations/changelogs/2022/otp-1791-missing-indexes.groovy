databaseChangeLog = {

    changeSet(author: "", id: "1662710599131-171") {
        createIndex(indexName: "abstract_merging_work_package_library_preparation_kit_idx", tableName: "abstract_merging_work_package") {
            column(name: "library_preparation_kit_id")
        }
    }

    changeSet(author: "", id: "1662710599131-172") {
        createIndex(indexName: "aceseq_qc_aceseq_instance_idx", tableName: "aceseq_qc") {
            column(name: "aceseq_instance_id")
        }
    }

    changeSet(author: "", id: "1662710599131-173") {
        createIndex(indexName: "bam_file_pair_analysis_config_idx", tableName: "bam_file_pair_analysis") {
            column(name: "config_id")
        }
    }

    changeSet(author: "", id: "1662710599131-174") {
        createIndex(indexName: "cluster_job_individual_idx", tableName: "cluster_job") {
            column(name: "individual_id")
        }
    }

    changeSet(author: "", id: "1662710599131-175") {
        createIndex(indexName: "cluster_job_old_system_check_status_idx", tableName: "cluster_job") {
            column(name: "old_system")

            column(name: "check_status")
        }
    }

    changeSet(author: "", id: "1662710599131-176") {
        createIndex(indexName: "cluster_job_realm_idx", tableName: "cluster_job") {
            column(name: "realm_id")
        }
    }

    changeSet(author: "", id: "1662710599131-177") {
        createIndex(indexName: "data_file_project_idx", tableName: "data_file") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-178") {
        createIndex(indexName: "data_transfer_agreement_document_data_transfer_agreement_idx", tableName: "data_transfer_agreement_document") {
            column(name: "data_transfer_agreement_id")
        }
    }

    changeSet(author: "", id: "1662710599131-179") {
        createIndex(indexName: "data_transfer_agreement_project_idx", tableName: "data_transfer_agreement") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-180") {
        createIndex(indexName: "data_transfer_data_transfer_agreement_idx", tableName: "data_transfer") {
            column(name: "data_transfer_agreement_id")
        }
    }

    changeSet(author: "", id: "1662710599131-181") {
        createIndex(indexName: "data_transfer_document_data_transfer_idx", tableName: "data_transfer_document") {
            column(name: "data_transfer_id")
        }
    }

    changeSet(author: "", id: "1662710599131-182") {
        createIndex(indexName: "decision_processing_step_decision_idx", tableName: "processing_step") {
            column(name: "decision_id")
        }
    }

    changeSet(author: "", id: "1662710599131-183") {
        createIndex(indexName: "external_workflow_config_fragment_previous_idx", tableName: "external_workflow_config_fragment") {
            column(name: "previous_id")
        }
    }

    changeSet(author: "", id: "1662710599131-184") {
        createIndex(indexName: "external_workflow_config_selector_external_workflow_config_fragment_idx", tableName: "external_workflow_config_selector") {
            column(name: "external_workflow_config_fragment_id")
        }
    }

    changeSet(author: "", id: "1662710599131-185") {
        createIndex(indexName: "external_workflow_config_selector_selector_type_idx", tableName: "external_workflow_config_selector") {
            column(name: "selector_type")
        }
    }

    changeSet(author: "", id: "1662710599131-186") {
        createIndex(indexName: "file_type_type_idx", tableName: "file_type") {
            column(name: "type")
        }
    }

    changeSet(author: "", id: "1662710599131-187") {
        createIndex(indexName: "gene_model_reference_genome_idx", tableName: "gene_model") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "", id: "1662710599131-188") {
        createIndex(indexName: "import_process_state_idx", tableName: "import_process") {
            column(name: "state")
        }
    }

    changeSet(author: "", id: "1662710599131-189") {
        createIndex(indexName: "job_definition_bean_idx", tableName: "job_definition") {
            column(name: "bean")
        }
    }

    changeSet(author: "", id: "1662710599131-190") {
        createIndex(indexName: "job_definition_next_idx", tableName: "job_definition") {
            column(name: "next_id")
        }
    }

    changeSet(author: "", id: "1662710599131-191") {
        createIndex(indexName: "job_definition_plan_idx", tableName: "job_definition") {
            column(name: "plan_id")
        }
    }

    changeSet(author: "", id: "1662710599131-192") {
        createIndex(indexName: "job_definition_previous_idx", tableName: "job_definition") {
            column(name: "previous_id")
        }
    }

    changeSet(author: "", id: "1662710599131-193") {
        createIndex(indexName: "job_execution_plan_first_job_idx", tableName: "job_execution_plan") {
            column(name: "first_job_id")
        }
    }

    changeSet(author: "", id: "1662710599131-194") {
        createIndex(indexName: "job_execution_plan_previous_plan_idx", tableName: "job_execution_plan") {
            column(name: "previous_plan_id")
        }
    }

    changeSet(author: "", id: "1662710599131-195") {
        createIndex(indexName: "job_execution_process_parameter_idx", tableName: "job_execution_plan") {
            column(name: "process_parameter_id")
        }
    }

    changeSet(author: "", id: "1662710599131-196") {
        createIndex(indexName: "job_execution_start_job_idx", tableName: "job_execution_plan") {
            column(name: "start_job_id")
        }
    }

    changeSet(author: "", id: "1662710599131-197") {
        createIndex(indexName: "merging_work_package_alignment_property_merging_work_package_idx", tableName: "merging_work_package_alignment_property") {
            column(name: "merging_work_package_id")
        }
    }

    changeSet(author: "", id: "1662710599131-198") {
        createIndex(indexName: "parameter_mapping_from_idx", tableName: "parameter_mapping") {
            column(name: "from_id")
        }
    }

    changeSet(author: "", id: "1662710599131-199") {
        createIndex(indexName: "parameter_mapping_job_idx", tableName: "parameter_mapping") {
            column(name: "job_id")
        }
    }

    changeSet(author: "", id: "1662710599131-200") {
        createIndex(indexName: "parameter_mapping_to_idx", tableName: "parameter_mapping") {
            column(name: "to_id")
        }
    }

    changeSet(author: "", id: "1662710599131-201") {
        createIndex(indexName: "parameter_type_class_name_idx", tableName: "parameter_type") {
            column(name: "class_name")
        }
    }

    changeSet(author: "", id: "1662710599131-202") {
        createIndex(indexName: "parameter_type_job_definition_idx", tableName: "parameter_type") {
            column(name: "job_definition_id")
        }
    }

    changeSet(author: "", id: "1662710599131-203") {
        createIndex(indexName: "parameter_type_parameter_usage_idx", tableName: "parameter_type") {
            column(name: "parameter_usage")
        }
    }

    changeSet(author: "", id: "1662710599131-204") {
        createIndex(indexName: "parameter_type_idx", tableName: "parameter") {
            column(name: "type_id")
        }
    }

    changeSet(author: "", id: "1662710599131-205") {
        createIndex(indexName: "process_started_idx", tableName: "process") {
            column(name: "started")
        }
    }

    changeSet(author: "", id: "1662710599131-206") {
        createIndex(indexName: "processing_step_job_definition_idx", tableName: "processing_step") {
            column(name: "job_definition_id")
        }
    }

    changeSet(author: "", id: "1662710599131-207") {
        createIndex(indexName: "project_info_project_idx", tableName: "project_info") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-208") {
        createIndex(indexName: "project_name_in_metadata_files_idx", tableName: "project") {
            column(name: "name_in_metadata_files")
        }
    }

    changeSet(author: "", id: "1662710599131-209") {
        createIndex(indexName: "project_realm_idx", tableName: "project") {
            column(name: "realm_id")
        }
    }

    changeSet(author: "", id: "1662710599131-210") {
        createIndex(indexName: "project_request_persistent_state_current_owner_idx", tableName: "project_request_persistent_state") {
            column(name: "current_owner_id")
        }
    }

    changeSet(author: "", id: "1662710599131-211") {
        createIndex(indexName: "project_request_state_idx", tableName: "project_request") {
            column(name: "state_id")
        }
    }

    changeSet(author: "", id: "1662710599131-212") {
        createIndex(indexName: "project_request_user_user_idx", tableName: "project_request_user") {
            column(name: "user_id")
        }
    }

    changeSet(author: "", id: "1662710599131-213") {
        createIndex(indexName: "project_unix_group_idx", tableName: "project") {
            column(name: "unix_group")
        }
    }

    changeSet(author: "", id: "1662710599131-214") {
        createIndex(indexName: "reference_genome_index_reference_genome_idx", tableName: "reference_genome_index") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "", id: "1662710599131-215") {
        createIndex(indexName: "reference_genome_index_tool_name_idx", tableName: "reference_genome_index") {
            column(name: "tool_name_id")
        }
    }

    changeSet(author: "", id: "1662710599131-216") {
        createIndex(indexName: "reference_genome_project_seq_type_alignment_property_reference_genome_project_seq_type_idx", tableName: "reference_genome_project_seq_type_alignment_property") {
            column(name: "reference_genome_project_seq_type_id")
        }
    }

    changeSet(author: "", id: "1662710599131-217") {
        createIndex(indexName: "reference_genome_selector_project_idx", tableName: "reference_genome_selector") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-218") {
        createIndex(indexName: "reference_genome_selector_reference_genome_idx", tableName: "reference_genome_selector") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "", id: "1662710599131-219") {
        createIndex(indexName: "reference_genome_selector_seq_type_idx", tableName: "reference_genome_selector") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "", id: "1662710599131-220") {
        createIndex(indexName: "reference_genome_selector_workflow_idx", tableName: "reference_genome_selector") {
            column(name: "workflow_id")
        }
    }

    changeSet(author: "", id: "1662710599131-221") {
        createIndex(indexName: "restarted_processing_step_original_idx", tableName: "processing_step") {
            column(name: "original_id")
        }
    }

    changeSet(author: "", id: "1662710599131-222") {
        createIndex(indexName: "sample_pair_merging_work_package_1_idx", tableName: "sample_pair") {
            column(name: "merging_work_package1_id")
        }
    }

    changeSet(author: "", id: "1662710599131-223") {
        createIndex(indexName: "sample_pair_merging_work_package_2_idx", tableName: "sample_pair") {
            column(name: "merging_work_package2_id")
        }
    }

    changeSet(author: "", id: "1662710599131-224") {
        createIndex(indexName: "seq_platform_group_merging_criteria_idx", tableName: "seq_platform_group") {
            column(name: "merging_criteria_id")
        }
    }

    changeSet(author: "", id: "1662710599131-225") {
        createIndex(indexName: "seq_platform_sequencing_name_idx", tableName: "seq_platform") {
            column(name: "name")
        }
    }

    changeSet(author: "", id: "1662710599131-226") {
        createIndex(indexName: "seq_track_lane_id_idx", tableName: "seq_track") {
            column(name: "lane_id")
        }
    }

    changeSet(author: "", id: "1662710599131-227") {
        createIndex(indexName: "seq_track_pipeline_version_idx", tableName: "seq_track") {
            column(name: "pipeline_version_id")
        }
    }

    changeSet(author: "", id: "1662710599131-228") {
        createIndex(indexName: "stat_size_file_name_reference_genome_idx", tableName: "stat_size_file_name") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "", id: "1662710599131-229") {
        createIndex(indexName: "user_project_role_project_idx", tableName: "user_project_role") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-230") {
        createIndex(indexName: "user_project_role_user_idx", tableName: "user_project_role") {
            column(name: "user_id")
        }
    }

    changeSet(author: "", id: "1662710599131-231") {
        createIndex(indexName: "workflow_artefact_artefact_type_idx", tableName: "workflow_artefact") {
            column(name: "artefact_type")
        }
    }

    changeSet(author: "", id: "1662710599131-232") {
        createIndex(indexName: "workflow_job_error_definition_job_bean_name_idx", tableName: "workflow_job_error_definition") {
            column(name: "job_bean_name")
        }
    }

    changeSet(author: "", id: "1662710599131-233") {
        createIndex(indexName: "workflow_log_workflow_step_idx", tableName: "workflow_log") {
            column(name: "workflow_step_id")
        }
    }

    changeSet(author: "", id: "1662710599131-234") {
        createIndex(indexName: "workflow_run_project_idx", tableName: "workflow_run") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-235") {
        createIndex(indexName: "workflow_run_workflow_version_idx", tableName: "workflow_run") {
            column(name: "workflow_version_id")
        }
    }

    changeSet(author: "", id: "1662710599131-236") {
        createIndex(indexName: "workflow_step_state_idx", tableName: "workflow_step") {
            column(name: "state")
        }
    }

    changeSet(author: "", id: "1662710599131-237") {
        createIndex(indexName: "workflow_version_selector_previous_idx", tableName: "workflow_version_selector") {
            column(name: "previous_id")
        }
    }

    changeSet(author: "", id: "1662710599131-238") {
        createIndex(indexName: "workflow_version_selector_project_idx", tableName: "workflow_version_selector") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-239") {
        createIndex(indexName: "workflow_version_selector_seq_type_idx", tableName: "workflow_version_selector") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "", id: "1662710599131-240") {
        createIndex(indexName: "workflow_version_selector_workflow_version_idx", tableName: "workflow_version_selector") {
            column(name: "workflow_version_id")
        }
    }

    changeSet(author: "", id: "1662710599131-241") {
        createIndex(indexName: "workflow_version_workflow_idx", tableName: "workflow_version") {
            column(name: "workflow_id")
        }
    }

    //indexes for m2m tables

    changeSet(author: "", id: "1662710599131-m2m-1") {
        createIndex(indexName: "roddy_bam_file_seq_track_seq_track_id_idx", tableName: "roddy_bam_file_seq_track") {
            column(name: "seq_track_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-2") {
        createIndex(indexName: "single_cell_bam_file_seq_track_seq_track_id_idx", tableName: "single_cell_bam_file_seq_track") {
            column(name: "seq_track_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-3") {
        createIndex(indexName: "reference_genome_species_with_strain_reference_genome_species_id_idx", tableName: "reference_genome_species_with_strain") {
            column(name: "reference_genome_species_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-4") {
        createIndex(indexName: "reference_genome_species_with_strain_species_with_strain_id_idx", tableName: "reference_genome_species_with_strain") {
            column(name: "species_with_strain_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-5") {
        createIndex(indexName: "sample_species_with_strain_sample_mixed_in_species_id_idx", tableName: "sample_species_with_strain") {
            column(name: "sample_mixed_in_species_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-6") {
        createIndex(indexName: "sample_species_with_strain_species_with_strain_id_idx", tableName: "sample_species_with_strain") {
            column(name: "species_with_strain_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-7") {
        createIndex(indexName: "seq_platform_seq_platform_groups_seq_platform_group_id_idx", tableName: "seq_platform_seq_platform_groups") {
            column(name: "seq_platform_group_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-8") {
        createIndex(indexName: "project_species_with_strain_project_species_with_strains_id_idx", tableName: "project_species_with_strain") {
            column(name: "project_species_with_strains_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-9") {
        createIndex(indexName: "project_species_with_strain_species_with_strain_id_idx", tableName: "project_species_with_strain") {
            column(name: "species_with_strain_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-10") {
        createIndex(indexName: "project_request_persistent_state_users_that_need_to_approve_project_request_persistent_state_id_idx", tableName: "project_request_persistent_state_users_that_need_to_approve") {
            column(name: "project_request_persistent_state_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-11") {
        createIndex(indexName: "project_request_persistent_state_users_that_need_to_approve_user_id_idx", tableName: "project_request_persistent_state_users_that_need_to_approve") {
            column(name: "user_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-12") {
        createIndex(indexName: "project_request_persistent_state_users_that_already_approved_project_request_persistent_state_id_idx", tableName: "project_request_persistent_state_users_that_already_approved") {
            column(name: "project_request_persistent_state_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-13") {
        createIndex(indexName: "project_request_persistent_state_users_that_already_approved_user_id_idx", tableName: "project_request_persistent_state_users_that_already_approved") {
            column(name: "user_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-14") {
        createIndex(indexName: "project_request_project_request_user_project_request_user_id_idx", tableName: "project_request_project_request_user") {
            column(name: "project_request_user_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-15") {
        createIndex(indexName: "project_request_seq_type_seq_type_id_idx", tableName: "project_request_seq_type") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-16") {
        createIndex(indexName: "project_request_custom_sequencing_centers_project_request_id_idx", tableName: "project_request_custom_sequencing_centers") {
            column(name: "project_request_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-17") {
        createIndex(indexName: "project_request_species_with_strain_project_request_species_with_strains_id_idx", tableName: "project_request_species_with_strain") {
            column(name: "project_request_species_with_strains_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-18") {
        createIndex(indexName: "project_request_species_with_strain_species_with_strain_id_idx", tableName: "project_request_species_with_strain") {
            column(name: "species_with_strain_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-19") {
        createIndex(indexName: "project_request_keywords_project_request_id_idx", tableName: "project_request_keywords") {
            column(name: "project_request_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-20") {
        createIndex(indexName: "project_request_custom_species_with_strains_project_request_id_idx", tableName: "project_request_custom_species_with_strains") {
            column(name: "project_request_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-21") {
        createIndex(indexName: "project_request_seq_center_project_request_sequencing_centers_id_idx", tableName: "project_request_seq_center") {
            column(name: "project_request_sequencing_centers_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-22") {
        createIndex(indexName: "project_request_seq_center_seq_center_id_idx", tableName: "project_request_seq_center") {
            column(name: "seq_center_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-23") {
        createIndex(indexName: "project_request_custom_seq_types_project_request_id_idx", tableName: "project_request_custom_seq_types") {
            column(name: "project_request_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-24") {
        createIndex(indexName: "reference_genome_selector_species_with_strain_reference_genome_selector_species_id_idx", tableName: "reference_genome_selector_species_with_strain") {
            column(name: "reference_genome_selector_species_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-25") {
        createIndex(indexName: "reference_genome_selector_species_with_strain_species_with_strain_id_idx", tableName: "reference_genome_selector_species_with_strain") {
            column(name: "species_with_strain_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-26") {
        createIndex(indexName: "workflow_reference_genome_workflow_allowed_reference_genomes_id_idx", tableName: "workflow_reference_genome") {
            column(name: "workflow_allowed_reference_genomes_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-27") {
        createIndex(indexName: "workflow_reference_genome_reference_genome_id_idx", tableName: "workflow_reference_genome") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-28") {
        createIndex(indexName: "workflow_seq_type_workflow_supported_seq_types_id_idx", tableName: "workflow_seq_type") {
            column(name: "workflow_supported_seq_types_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-29") {
        createIndex(indexName: "workflow_seq_type_seq_type_id_idx", tableName: "workflow_seq_type") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-30") {
        createIndex(indexName: "external_workflow_config_selector_library_preparation_kit_external_workflow_config_selector_library_preparation_kits_id_idx", tableName: "external_workflow_config_selector_library_preparation_kit") {
            column(name: "external_workflow_config_selector_library_preparation_kits_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-31") {
        createIndex(indexName: "external_workflow_config_selector_library_preparation_kit_library_preparation_kit_id_idx", tableName: "external_workflow_config_selector_library_preparation_kit") {
            column(name: "library_preparation_kit_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-32") {
        createIndex(indexName: "external_workflow_config_selector_project_external_workflow_config_selector_projects_id_idx", tableName: "external_workflow_config_selector_project") {
            column(name: "external_workflow_config_selector_projects_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-33") {
        createIndex(indexName: "external_workflow_config_selector_project_project_id_idx", tableName: "external_workflow_config_selector_project") {
            column(name: "project_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-34") {
        createIndex(indexName: "external_workflow_config_selector_reference_genome_external_workflow_config_selector_reference_genomes_id_idx", tableName: "external_workflow_config_selector_reference_genome") {
            column(name: "external_workflow_config_selector_reference_genomes_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-35") {
        createIndex(indexName: "external_workflow_config_selector_reference_genome_reference_genome_id_idx", tableName: "external_workflow_config_selector_reference_genome") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-36") {
        createIndex(indexName: "external_workflow_config_selector_seq_type_external_workflow_config_selector_seq_types_id_idx", tableName: "external_workflow_config_selector_seq_type") {
            column(name: "external_workflow_config_selector_seq_types_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-37") {
        createIndex(indexName: "external_workflow_config_selector_seq_type_seq_type_id_idx", tableName: "external_workflow_config_selector_seq_type") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-38") {
        createIndex(indexName: "external_workflow_config_selector_workflow_external_workflow_config_selector_workflows_id_idx", tableName: "external_workflow_config_selector_workflow") {
            column(name: "external_workflow_config_selector_workflows_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-39") {
        createIndex(indexName: "external_workflow_config_selector_workflow_workflow_id_idx", tableName: "external_workflow_config_selector_workflow") {
            column(name: "workflow_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-40") {
        createIndex(indexName: "external_workflow_config_selector_workflow_version_external_workflow_config_selector_workflow_versions_id_idx", tableName: "external_workflow_config_selector_workflow_version") {
            column(name: "external_workflow_config_selector_workflow_versions_id")
        }
    }

    changeSet(author: "", id: "1662710599131-m2m-41") {
        createIndex(indexName: "external_workflow_config_selector_workflow_version_workflow_version_id_idx", tableName: "external_workflow_config_selector_workflow_version") {
            column(name: "workflow_version_id")
        }
    }
}
