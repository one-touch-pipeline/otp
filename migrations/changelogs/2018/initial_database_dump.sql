CREATE TABLE public.abstract_bam_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    has_index_file boolean NOT NULL,
    type character varying(255),
    class character varying(255) NOT NULL,
    alignment_pass_id bigint,
    date_created timestamp with time zone,
    date_from_file_system timestamp with time zone,
    file_exists boolean,
    file_size bigint,
    has_coverage_plot boolean DEFAULT false NOT NULL,
    has_insert_size_plot boolean DEFAULT false NOT NULL,
    quality_assessment_status character varying(255) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    has_metrics_file boolean NOT NULL,
    merging_pass_id bigint,
    status character varying(255) NOT NULL,
    withdrawn boolean DEFAULT false NOT NULL,
    deletion_date timestamp with time zone,
    file_operation_status character varying(255),
    md5sum character varying(255),
    sha256sum character varying(255),
    coverage double precision,
    number_of_merged_lanes integer,
    coverage_withn double precision,
    file_name character varying(255),
    base_bam_file_id bigint,
    config_id bigint,
    work_package_id bigint,
    identifier integer,
    work_directory_name character varying(255),
    imported_from character varying(255),
    comment_id bigint,
    qc_traffic_light_status character varying(255),
    insert_size_file character varying(1000),
    mean_sequence_length integer
);

CREATE TABLE public.abstract_merging_work_package (
    id bigint NOT NULL,
    version bigint NOT NULL,
    processing_type character varying(255),
    sample_id bigint NOT NULL,
    seq_type_id bigint NOT NULL,
    reference_genome_id bigint NOT NULL,
    seq_platform_group_id bigint,
    pipeline_id bigint NOT NULL,
    needs_processing boolean,
    stat_size_file_name character varying(255),
    bam_file_in_project_folder_id bigint,
    library_preparation_kit_id bigint,
    class character varying(255) NOT NULL,
    antibody_target_id bigint
);

CREATE TABLE public.abstract_quality_assessment (
    id bigint NOT NULL,
    version bigint NOT NULL,
    duplicater1 bigint,
    duplicater2 bigint,
    duplicates bigint,
    end_read_aberration bigint,
    insert_size_mean double precision,
    insert_size_median double precision,
    insert_sizerms double precision,
    insert_sizesd double precision,
    mapped_low_qualityr1 bigint,
    mapped_low_qualityr2 bigint,
    mapped_quality_longr1 bigint,
    mapped_quality_longr2 bigint,
    mapped_shortr1 bigint,
    mapped_shortr2 bigint,
    not_mappedr1 bigint,
    not_mappedr2 bigint,
    paired_in_sequencing bigint,
    paired_read1 bigint,
    paired_read2 bigint,
    proper_pair_strand_conflict bigint,
    properly_paired bigint,
    qc_bases_mapped bigint,
    qc_failed_reads bigint,
    reference_agreement bigint,
    reference_agreement_strand_conflict bigint,
    reference_length bigint,
    singletons bigint,
    total_mapped_read_counter bigint,
    total_read_counter bigint,
    with_itself_and_mate_mapped bigint,
    with_mate_mapped_to_different_chr bigint,
    with_mate_mapped_to_different_chr_maq bigint,
    class character varying(255) NOT NULL,
    chromosome_name character varying(255),
    percent_incorrectpeorientation double precision,
    percent_read_pairs_map_to_diff_chrom double precision,
    quality_assessment_pass_id bigint,
    quality_assessment_merged_pass_id bigint,
    all_bases_mapped bigint,
    on_target_mapped_bases bigint,
    chromosome character varying(255),
    genome_withoutncoverage_qc_bases double precision,
    insert_sizecv double precision,
    percentage_mates_on_different_chr double precision,
    seq_track_id bigint,
    library_directory_name character varying(255),
    threepnorm double precision,
    fivepnorm double precision,
    base_mismatch_rate double precision,
    end1percentage_sense double precision,
    end1mapping_rate double precision,
    end1mismatch_rate double precision,
    end1sense double precision,
    end2percentage_sense double precision,
    end2mapping_rate double precision,
    end2mismatch_rate double precision,
    end2sense double precision,
    exonic_rate double precision,
    expression_profiling_efficiency double precision,
    gap_percentage double precision,
    intragenic_rate double precision,
    intergenic_rate double precision,
    intronic_rate double precision,
    mapped_unique_rate_of_total double precision,
    mapping_rate double precision,
    meancv double precision,
    mean_per_base_cov double precision,
    unique_rateof_mapped double precision,
    rrnarate double precision,
    duplicates_rate double precision,
    properly_paired_percentage double precision,
    total_mapped_read_counter_percentage double precision,
    singletons_percentage double precision,
    alternative_alignments bigint,
    chimeric_pairs bigint,
    cumul_gap_length bigint,
    end1antisense bigint,
    end2antisense bigint,
    estimated_library_size bigint,
    failed_vendorqccheck bigint,
    genes_detected bigint,
    mapped bigint,
    mapped_pairs bigint,
    mapped_unique bigint,
    no_covered5p bigint,
    num_gaps bigint,
    read_length bigint,
    split_reads bigint,
    total_purity_filtered_reads_sequenced bigint,
    transcripts_detected bigint,
    unpaired_reads bigint,
    rrnareads bigint,
    mapped_read1 bigint,
    mapped_read2 bigint,
    secondary_alignments bigint,
    supplementary_alignments bigint
);

CREATE TABLE public.aceseq_instance_roddy_execution_directory_names (
    aceseq_instance_id bigint NOT NULL,
    roddy_execution_directory_names_string character varying(255) NOT NULL,
    roddy_execution_directory_names_idx integer NOT NULL
);

CREATE TABLE public.aceseq_qc (
    id bigint NOT NULL,
    version bigint NOT NULL,
    number integer NOT NULL,
    tcc double precision NOT NULL,
    ploidy_factor character varying NOT NULL,
    ploidy double precision NOT NULL,
    goodness_of_fit double precision NOT NULL,
    gender character varying NOT NULL,
    solution_possible integer NOT NULL,
    aceseq_instance_id bigint NOT NULL
);

CREATE TABLE public.acl_class (
    id bigint NOT NULL,
    class character varying(255) NOT NULL
);

CREATE TABLE public.acl_entry (
    id bigint NOT NULL,
    ace_order integer NOT NULL,
    acl_object_identity bigint NOT NULL,
    audit_failure boolean NOT NULL,
    audit_success boolean NOT NULL,
    granting boolean NOT NULL,
    mask integer NOT NULL,
    sid bigint NOT NULL
);

CREATE TABLE public.acl_object_identity (
    id bigint NOT NULL,
    object_id_class bigint NOT NULL,
    entries_inheriting boolean NOT NULL,
    object_id_identity bigint NOT NULL,
    owner_sid bigint,
    parent_object bigint
);

CREATE TABLE public.acl_sid (
    id bigint NOT NULL,
    principal boolean NOT NULL,
    sid character varying(255) NOT NULL
);

CREATE TABLE public.data_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    alignment_log_id bigint,
    date_created timestamp without time zone NOT NULL,
    date_executed timestamp without time zone,
    date_file_system timestamp without time zone,
    file_exists boolean NOT NULL,
    file_linked boolean NOT NULL,
    file_name character varying(255) NOT NULL,
    file_size bigint NOT NULL,
    file_type_id bigint NOT NULL,
    file_withdrawn boolean NOT NULL,
    md5sum character varying(255) NOT NULL,
    merging_log_id bigint,
    path_name character varying(255) NOT NULL,
    project_id bigint,
    run_id bigint NOT NULL,
    run_segment_id bigint NOT NULL,
    seq_track_id bigint,
    used boolean NOT NULL,
    vbp_file_name character varying(255) NOT NULL,
    mate_number integer,
    comment_id bigint,
    n_reads bigint,
    sequence_length character varying(255),
    initial_directory text NOT NULL
);

CREATE TABLE public.individual (
    id bigint NOT NULL,
    version bigint NOT NULL,
    mock_full_name character varying(255) NOT NULL,
    mock_pid character varying(255) NOT NULL,
    pid character varying(255) NOT NULL,
    project_id bigint NOT NULL,
    type character varying(255) NOT NULL,
    intern_identifier character varying(255),
    comment_id bigint
);

CREATE TABLE public.project (
    id bigint NOT NULL,
    version bigint NOT NULL,
    dir_name character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    project_group_id bigint,
    processing_priority smallint DEFAULT 0 NOT NULL,
    alignment_decider_bean_name character varying(255) NOT NULL,
    has_to_be_copied boolean NOT NULL,
    snv character varying(255) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    name_in_metadata_files character varying(255),
    comment_id bigint,
    mailing_list_name character varying(255),
    description text,
    dir_analysis text,
    unix_group character varying(255),
    finger_printing boolean NOT NULL,
    cost_center character varying(255),
    tumor_entity_id bigint,
    phabricator_alias character varying(255),
    realm_id bigint NOT NULL,
    custom_final_notification boolean NOT NULL
);

CREATE TABLE public.realm (
    id bigint NOT NULL,
    version bigint NOT NULL,
    host character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    default_job_submission_options character varying(255) NOT NULL,
    port integer NOT NULL,
    timeout integer NOT NULL,
    job_scheduler character varying(255)
);

CREATE TABLE public.run (
    id bigint NOT NULL,
    version bigint NOT NULL,
    blacklisted boolean NOT NULL,
    date_created timestamp without time zone NOT NULL,
    date_executed timestamp without time zone,
    name character varying(255) NOT NULL,
    seq_center_id bigint NOT NULL,
    seq_platform_id bigint NOT NULL
);

CREATE TABLE public.sample (
    id bigint NOT NULL,
    version bigint NOT NULL,
    individual_id bigint NOT NULL,
    sample_type_id bigint NOT NULL
);

CREATE TABLE public.sample_type (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    specific_reference_genome character varying(255) NOT NULL
);

CREATE TABLE public.seq_center (
    id bigint NOT NULL,
    version bigint NOT NULL,
    dir_name character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    auto_importable boolean NOT NULL
);

CREATE TABLE public.seq_platform (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    sequencing_kit_label_id bigint,
    seq_platform_model_label_id bigint,
    identifier_in_run_name character varying(255)
);

CREATE TABLE public.seq_platform_model_label (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.seq_track (
    id bigint NOT NULL,
    version bigint NOT NULL,
    has_original_bam boolean NOT NULL,
    insert_size integer NOT NULL,
    lane_id character varying(255) NOT NULL,
    n_base_pairs bigint,
    pipeline_version_id bigint NOT NULL,
    run_id bigint NOT NULL,
    sample_id bigint NOT NULL,
    seq_type_id bigint NOT NULL,
    fastqc_state character varying(255) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    quality_encoding character varying(255) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    library_preparation_kit_id bigint,
    class character varying(255) NOT NULL,
    kit_info_reliability character varying(255) NOT NULL,
    antibody character varying(255),
    antibody_target_id bigint,
    linked_externally boolean NOT NULL,
    library_name character varying(255),
    normalized_library_name character varying(255),
    data_installation_state character varying(255) NOT NULL,
    ilse_submission_id bigint,
    problem character varying(255)
);

CREATE TABLE public.seq_type (
    id bigint NOT NULL,
    version bigint NOT NULL,
    dir_name character varying(255) NOT NULL,
    library_layout character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    display_name character varying(255),
    roddy_name character varying(255)
);

CREATE TABLE public.sequencing_kit_label (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE VIEW public.aggregate_sequences AS
 SELECT (sum(st.n_base_pairs))::bigint AS sum_n_base_pairs,
    (floor((sum(st.n_base_pairs) / (1000000000)::numeric)))::bigint AS sum_n_base_pairs_gb,
    count(st.id) AS lane_count,
    st.seq_type_id,
    r.seq_platform_id,
    sp.seq_platform_model_label_id,
    sp.sequencing_kit_label_id,
    st.sample_id,
    r.seq_center_id,
    s.sample_type_id,
    s.individual_id,
    p.id AS project_id,
    re.id AS realm_id,
    sp.name AS seq_platform_name,
    spml.name AS seq_platform_model_label_name,
    skl.name AS sequencing_kit_label_name,
    seqtype.name AS seq_type_name,
    seqtype.display_name AS seq_type_display_name,
    seqtype.library_layout,
    seqtype.dir_name,
    sampletype.name AS sample_type_name,
    i.type,
    i.pid,
    i.mock_pid,
    i.mock_full_name,
    re.name AS realm_name,
    p.name AS project_name,
    p.dir_name AS project_dir_name,
    sc.name AS seq_center_name,
    sc.dir_name AS seq_center_dir_name
   FROM (((((((((((public.seq_track st
     JOIN public.run r ON ((r.id = st.run_id)))
     JOIN public.seq_platform sp ON ((sp.id = r.seq_platform_id)))
     JOIN public.seq_type seqtype ON ((seqtype.id = st.seq_type_id)))
     JOIN public.sample s ON ((s.id = st.sample_id)))
     JOIN public.sample_type sampletype ON ((s.sample_type_id = sampletype.id)))
     JOIN public.individual i ON ((s.individual_id = i.id)))
     JOIN public.project p ON ((i.project_id = p.id)))
     JOIN public.realm re ON ((p.realm_id = re.id)))
     JOIN public.seq_center sc ON ((r.seq_center_id = sc.id)))
     LEFT JOIN public.seq_platform_model_label spml ON ((sp.seq_platform_model_label_id = spml.id)))
     LEFT JOIN public.sequencing_kit_label skl ON ((sp.sequencing_kit_label_id = skl.id)))
  WHERE (NOT (st.id IN ( SELECT DISTINCT seq.id
           FROM (public.seq_track seq
             JOIN public.data_file df ON ((df.seq_track_id = seq.id)))
          WHERE (df.file_withdrawn <> false))))
  GROUP BY st.seq_type_id, r.seq_platform_id, sp.seq_platform_model_label_id, sp.sequencing_kit_label_id, st.sample_id, r.seq_center_id, s.sample_type_id, s.individual_id, p.id, re.id, sp.name, spml.name, skl.name, seqtype.name, seqtype.display_name, seqtype.library_layout, seqtype.dir_name, sampletype.name, i.type, i.pid, i.mock_pid, i.mock_full_name, p.name, p.dir_name, re.name, sc.name, sc.dir_name;

CREATE TABLE public.alignment_log (
    id bigint NOT NULL,
    version bigint NOT NULL,
    alignment_params_id bigint NOT NULL,
    executed_by character varying(255) NOT NULL,
    qc_state character varying(255) NOT NULL,
    seq_track_id bigint NOT NULL
);

CREATE TABLE public.alignment_params (
    id bigint NOT NULL,
    version bigint NOT NULL,
    genome character varying(255),
    params character varying(255),
    pipeline_id bigint NOT NULL
);

CREATE TABLE public.alignment_pass (
    id bigint NOT NULL,
    version bigint NOT NULL,
    identifier integer NOT NULL,
    seq_track_id bigint NOT NULL,
    work_package_id bigint NOT NULL,
    alignment_state character varying(255) NOT NULL
);

CREATE TABLE public.antibody_target (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.bam_file_pair_analysis (
    id bigint NOT NULL,
    version bigint NOT NULL,
    config_id bigint NOT NULL,
    processing_state character varying(255) NOT NULL,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL,
    instance_name character varying(255) NOT NULL,
    sample_type1bam_file_id bigint NOT NULL,
    sample_type2bam_file_id bigint NOT NULL,
    sample_pair_id bigint NOT NULL,
    class character varying(255) NOT NULL,
    withdrawn boolean NOT NULL,
    comment_id bigint,
    qc_traffic_light_status character varying(255)
);

CREATE TABLE public.bed_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    library_preparation_kit_id bigint NOT NULL,
    file_name character varying(255) NOT NULL,
    reference_genome_id bigint NOT NULL,
    target_size bigint NOT NULL,
    merged_target_size bigint NOT NULL
);

CREATE TABLE public.change_log (
    id bigint NOT NULL,
    version bigint NOT NULL,
    column_name character varying(255) NOT NULL,
    comment character varying(255) NOT NULL,
    date_created timestamp without time zone,
    from_value character varying(255) NOT NULL,
    referenced_class_id bigint,
    row_id bigint NOT NULL,
    source character varying(255) NOT NULL,
    table_name character varying(255) NOT NULL,
    to_value character varying(255) NOT NULL
);

CREATE TABLE public.cluster_job (
    id bigint NOT NULL,
    version bigint NOT NULL,
    validated boolean NOT NULL,
    processing_step_id bigint NOT NULL,
    realm_id bigint NOT NULL,
    cluster_job_id character varying(255) NOT NULL,
    cluster_job_name character varying(255) NOT NULL,
    exit_status character varying(255),
    exit_code integer,
    requested_cores integer,
    requested_memory bigint,
    used_memory bigint,
    job_class character varying(255) NOT NULL,
    seq_type_id bigint,
    used_cores integer,
    queued bigint NOT NULL,
    started bigint,
    ended bigint,
    cpu_time bigint,
    requested_walltime bigint,
    multiplexing boolean,
    xten boolean,
    file_size bigint,
    n_bases bigint,
    n_reads bigint,
    bases_per_bytes_fastq real,
    user_name character varying(255) NOT NULL,
    account_name character varying(255),
    eligible bigint,
    node character varying(255),
    start_count integer,
    used_swap integer,
    system_suspend_state_duration bigint,
    user_suspend_state_duration bigint,
    job_log text,
    individual_id bigint
);

CREATE TABLE public.cluster_job_cluster_job (
    cluster_job_dependencies_id bigint,
    cluster_job_id bigint
);

CREATE TABLE public.comment (
    id bigint NOT NULL,
    version bigint NOT NULL,
    comment text NOT NULL,
    author character varying(255) NOT NULL,
    modification_date timestamp with time zone NOT NULL
);

CREATE TABLE public.config_per_project (
    id bigint NOT NULL,
    version bigint NOT NULL,
    project_id bigint NOT NULL,
    seq_type_id bigint,
    configuration text,
    obsolete_date timestamp with time zone,
    previous_config_id bigint,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL,
    class character varying(255) NOT NULL,
    external_script_version character varying(255),
    pipeline_id bigint NOT NULL,
    config_file_path character varying(255),
    plugin_version character varying(255),
    config_version character varying(255),
    individual_id bigint,
    adapter_trimming_needed boolean NOT NULL,
    name_used_in_config character varying(255)
);

CREATE TABLE public.consistency_check (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date timestamp with time zone NOT NULL
);

CREATE TABLE public.consistency_status (
    id bigint NOT NULL,
    version bigint NOT NULL,
    consistency_check_id bigint NOT NULL,
    data_file_id bigint NOT NULL,
    resolved_date timestamp with time zone,
    status character varying(255) NOT NULL
);

CREATE TABLE public.contact_person (
    id bigint NOT NULL,
    version bigint NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    aspera character varying(255)
);

CREATE TABLE public.contact_person_role (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255)
);

CREATE TABLE public.decision_mapping (
    id bigint NOT NULL,
    version bigint NOT NULL,
    decision_id bigint NOT NULL,
    definition_id bigint NOT NULL
);

CREATE TABLE public.document (
    id bigint NOT NULL,
    version bigint NOT NULL,
    content bytea NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);

CREATE TABLE public.external_script (
    id bigint NOT NULL,
    version bigint NOT NULL,
    author character varying(255) NOT NULL,
    comment character varying(255),
    deprecated_date timestamp with time zone,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL,
    file_path character varying(255) NOT NULL,
    script_identifier character varying(255) NOT NULL,
    script_version character varying(255) NOT NULL,
    CONSTRAINT external_script_author_check CHECK (((author)::text <> ''::text))
);

CREATE TABLE public.externally_processed_merged_bam_file_further_files (
    externally_processed_merged_bam_file_id bigint NOT NULL,
    further_files_string character varying(255) NOT NULL
);

CREATE TABLE public.fastqc_processed_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    content_uploaded boolean NOT NULL,
    data_file_id bigint NOT NULL,
    date_created timestamp with time zone NOT NULL,
    date_from_file_system timestamp with time zone,
    file_exists boolean NOT NULL,
    file_size bigint NOT NULL
);

CREATE TABLE public.file_system_changes (
    id bigint NOT NULL,
    version bigint NOT NULL,
    command text NOT NULL,
    executed boolean NOT NULL,
    swap_info_id bigint NOT NULL,
    file_system_changes_idx integer NOT NULL
);

CREATE TABLE public.file_type (
    id bigint NOT NULL,
    version bigint NOT NULL,
    signature character varying(255) NOT NULL,
    sub_type character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    vbp_path character varying(255) NOT NULL
);

CREATE TABLE public.gene_model (
    id bigint NOT NULL,
    version bigint NOT NULL,
    path character varying(255) NOT NULL,
    date_created timestamp without time zone NOT NULL,
    dex_seq_file_name character varying(255),
    exclude_file_name character varying(255) NOT NULL,
    file_name character varying(255) NOT NULL,
    gc_file_name character varying(255),
    last_updated timestamp without time zone NOT NULL,
    reference_genome_id bigint NOT NULL
);

CREATE TABLE public.groups (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    read_job_system boolean NOT NULL,
    read_project boolean NOT NULL,
    read_sequence_center boolean NOT NULL,
    role_id bigint NOT NULL,
    write_job_system boolean NOT NULL,
    write_project boolean NOT NULL,
    write_sequence_center boolean NOT NULL
);

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.ilse_submission (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ilse_number integer NOT NULL,
    comment_id bigint,
    warning boolean NOT NULL
);

CREATE TABLE public.import_process (
    id bigint NOT NULL,
    version bigint NOT NULL,
    state character varying(255) NOT NULL,
    replace_source_with_link boolean,
    trigger_analysis boolean
);

CREATE TABLE public.import_process_externally_processed_merged_bam_file (
    import_process_externally_processed_merged_bam_files_id bigint NOT NULL,
    externally_processed_merged_bam_file_id bigint NOT NULL
);

CREATE TABLE public.indel_calling_instance_roddy_execution_directory_names (
    indel_calling_instance_id bigint NOT NULL,
    roddy_execution_directory_names_string character varying(255) NOT NULL,
    roddy_execution_directory_names_idx integer NOT NULL
);

CREATE TABLE public.indel_quality_control (
    id bigint NOT NULL,
    version bigint NOT NULL,
    indel_calling_instance_id bigint NOT NULL,
    file text NOT NULL,
    num_indels integer NOT NULL,
    num_ins integer NOT NULL,
    num_dels integer NOT NULL,
    num_size1_3 integer NOT NULL,
    num_size4_10 integer NOT NULL,
    num_size11plus integer NOT NULL,
    num_ins_size1_3 integer NOT NULL,
    num_ins_size4_10 integer NOT NULL,
    num_ins_size11plus integer NOT NULL,
    num_dels_size1_3 integer NOT NULL,
    num_dels_size4_10 integer NOT NULL,
    num_dels_size11plus integer NOT NULL,
    percent_ins double precision NOT NULL,
    percent_dels double precision NOT NULL,
    percent_size1_3 double precision NOT NULL,
    percent_size4_10 double precision NOT NULL,
    percent_size11plus double precision NOT NULL,
    percent_ins_size1_3 double precision NOT NULL,
    percent_ins_size4_10 double precision NOT NULL,
    percent_ins_size11plus double precision NOT NULL,
    percent_dels_size1_3 double precision NOT NULL,
    percent_dels_size4_10 double precision NOT NULL,
    percent_dels_size11plus double precision NOT NULL
);

CREATE TABLE public.indel_sample_swap_detection (
    id bigint NOT NULL,
    version bigint NOT NULL,
    germlinesnvs_heterozygous_in_both integer NOT NULL,
    germlinesnvs_heterozygous_in_both_rare integer NOT NULL,
    germline_small_vars_heterozygous_in_both_rare integer NOT NULL,
    indel_calling_instance_id bigint NOT NULL,
    pid character varying(255) NOT NULL,
    somatic_small_vars_in_control integer NOT NULL,
    somatic_small_vars_in_control_common_in_gnomad integer NOT NULL,
    somatic_small_vars_in_control_common_in_gnomad_per integer NOT NULL,
    somatic_small_vars_in_control_common_in_gnomas_per integer NOT NULL,
    somatic_small_vars_in_control_in_bias integer NOT NULL,
    somatic_small_vars_in_control_in_bias_per integer NOT NULL,
    somatic_small_vars_in_control_pass integer NOT NULL,
    somatic_small_vars_in_control_pass_per integer NOT NULL,
    somatic_small_vars_in_tumor integer NOT NULL,
    somatic_small_vars_in_tumor_common_in_gnomadper integer NOT NULL,
    somatic_small_vars_in_tumor_common_in_gnomad integer NOT NULL,
    somatic_small_vars_in_tumor_common_in_gnomad_per integer NOT NULL,
    somatic_small_vars_in_tumor_in_bias integer NOT NULL,
    somatic_small_vars_in_tumor_in_bias_per double precision NOT NULL,
    somatic_small_vars_in_tumor_pass integer NOT NULL,
    somatic_small_vars_in_tumor_pass_per double precision NOT NULL,
    tinda_germline_rare_after_rescue integer NOT NULL,
    tinda_somatic_after_rescue integer NOT NULL,
    tinda_somatic_after_rescue_median_allele_freq_in_control double precision NOT NULL
);

CREATE TABLE public.job_decision (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255) NOT NULL,
    job_definition_id bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.job_definition (
    id bigint NOT NULL,
    version bigint NOT NULL,
    bean character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    next_id bigint,
    plan_id bigint,
    previous_id bigint,
    class character varying(255) NOT NULL,
    validator_for_id bigint
);

CREATE TABLE public.job_definition_parameter (
    job_definition_constant_parameters_id bigint,
    parameter_id bigint
);

CREATE TABLE public.job_error_definition (
    id bigint NOT NULL,
    version bigint NOT NULL,
    error_expression text NOT NULL,
    type character varying(255) NOT NULL,
    action character varying(255) NOT NULL
);

CREATE TABLE public.job_error_definition_job_definition (
    job_error_definition_job_definitions_id bigint NOT NULL,
    job_definition_id bigint NOT NULL
);

CREATE TABLE public.job_error_definition_job_error_definition (
    job_error_definition_id bigint NOT NULL,
    job_error_definition_check_further_job_errors_id bigint NOT NULL
);

CREATE TABLE public.job_execution_plan (
    id bigint NOT NULL,
    version bigint NOT NULL,
    enabled boolean NOT NULL,
    first_job_id bigint,
    name character varying(255) NOT NULL,
    obsoleted boolean NOT NULL,
    plan_version integer NOT NULL,
    previous_plan_id bigint,
    process_parameter_id bigint,
    start_job_id bigint
);

CREATE TABLE public.library_preparation_kit (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    short_display_name character varying(255) NOT NULL,
    adapter_file character varying(255),
    reverse_complement_adapter_sequence character varying(255)
);

CREATE TABLE public.library_preparation_kit_synonym (
    id bigint NOT NULL,
    version bigint NOT NULL,
    library_preparation_kit_id bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.log_message (
    id bigint NOT NULL,
    version bigint NOT NULL,
    message text NOT NULL,
    date_created timestamp with time zone NOT NULL
);

CREATE TABLE public.merged_alignment_data_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    created_date timestamp without time zone NOT NULL,
    file_exists boolean NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(255) NOT NULL,
    file_size bigint NOT NULL,
    file_system character varying(255) NOT NULL,
    file_system_date timestamp without time zone,
    index_file_exists boolean NOT NULL,
    merging_log_id bigint NOT NULL
);

CREATE TABLE public.merging_assignment (
    id bigint NOT NULL,
    version bigint NOT NULL,
    seq_scan_id bigint NOT NULL,
    seq_track_id bigint NOT NULL
);

CREATE TABLE public.merging_criteria (
    id bigint NOT NULL,
    version bigint NOT NULL,
    project_id bigint NOT NULL,
    seq_type_id bigint NOT NULL,
    lib_prep_kit boolean NOT NULL,
    seq_platform_group character varying(255) NOT NULL
);

CREATE TABLE public.merging_log (
    id bigint NOT NULL,
    version bigint NOT NULL,
    alignment_params_id bigint NOT NULL,
    executed_by character varying(255) NOT NULL,
    qc_state character varying(255) NOT NULL,
    seq_scan_id bigint NOT NULL,
    status character varying(255) NOT NULL
);

CREATE TABLE public.merging_pass (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255),
    identifier integer NOT NULL,
    merging_set_id bigint NOT NULL
);

CREATE TABLE public.merging_set (
    id bigint NOT NULL,
    version bigint NOT NULL,
    identifier integer NOT NULL,
    merging_work_package_id bigint NOT NULL,
    status character varying(255) NOT NULL
);

CREATE TABLE public.merging_set_assignment (
    id bigint NOT NULL,
    version bigint NOT NULL,
    bam_file_id bigint NOT NULL,
    merging_set_id bigint NOT NULL
);

CREATE TABLE public.merging_work_package_alignment_property (
    id bigint NOT NULL,
    version bigint NOT NULL,
    merging_work_package_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(500) NOT NULL
);

CREATE TABLE public.merging_work_package_seq_track (
    merging_work_package_seq_tracks_id bigint,
    seq_track_id bigint
);

CREATE TABLE public.meta_data_entry (
    id bigint NOT NULL,
    version bigint NOT NULL,
    data_file_id bigint NOT NULL,
    key_id bigint NOT NULL,
    source character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    value character varying(255) NOT NULL
);

CREATE TABLE public.meta_data_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp without time zone NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(255) NOT NULL,
    run_segment_id bigint NOT NULL,
    md5sum character varying(255)
);

CREATE TABLE public.meta_data_key (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.otrs_ticket (
    id bigint NOT NULL,
    version bigint NOT NULL,
    ticket_number character varying(255) NOT NULL,
    date_created timestamp with time zone NOT NULL,
    installation_started timestamp with time zone,
    installation_finished timestamp with time zone,
    fastqc_started timestamp with time zone,
    fastqc_finished timestamp with time zone,
    alignment_started timestamp with time zone,
    alignment_finished timestamp with time zone,
    snv_started timestamp with time zone,
    snv_finished timestamp with time zone,
    final_notification_sent boolean NOT NULL,
    comment_id bigint,
    ticket_created timestamp with time zone,
    submission_received_notice timestamp with time zone,
    seq_center_comment text,
    automatic_notification boolean NOT NULL,
    indel_started timestamp with time zone,
    indel_finished timestamp with time zone,
    aceseq_started timestamp with time zone,
    aceseq_finished timestamp with time zone,
    sophia_started timestamp with time zone,
    sophia_finished timestamp with time zone
);

CREATE TABLE public.parameter (
    id bigint NOT NULL,
    version bigint NOT NULL,
    type_id bigint NOT NULL,
    value text NOT NULL
);

CREATE TABLE public.parameter_mapping (
    id bigint NOT NULL,
    version bigint NOT NULL,
    from_id bigint NOT NULL,
    job_id bigint NOT NULL,
    to_id bigint NOT NULL
);

CREATE TABLE public.parameter_type (
    id bigint NOT NULL,
    version bigint NOT NULL,
    class_name character varying(255),
    description character varying(255),
    job_definition_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    parameter_usage character varying(255) NOT NULL
);

CREATE TABLE public.picard_mark_duplicates_metrics (
    id bigint NOT NULL,
    version bigint NOT NULL,
    abstract_bam_file_id bigint NOT NULL,
    estimated_library_size bigint NOT NULL,
    library character varying(255) NOT NULL,
    metrics_class character varying(255) NOT NULL,
    percent_duplication double precision NOT NULL,
    read_pair_duplicates bigint NOT NULL,
    read_pair_optical_duplicates bigint NOT NULL,
    read_pairs_examined bigint NOT NULL,
    unmapped_reads bigint NOT NULL,
    unpaired_read_duplicates bigint NOT NULL,
    unpaired_reads_examined bigint NOT NULL
);

CREATE TABLE public.pipeline (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);

CREATE TABLE public.process (
    id bigint NOT NULL,
    version bigint NOT NULL,
    finished boolean NOT NULL,
    job_execution_plan_id bigint NOT NULL,
    start_job_class character varying(255) NOT NULL,
    started timestamp without time zone NOT NULL,
    comment_id bigint,
    operator_is_aware_of_failure boolean NOT NULL,
    restarted_id bigint
);

CREATE TABLE public.process_parameter (
    id bigint NOT NULL,
    version bigint NOT NULL,
    class_name character varying(255) NOT NULL,
    process_id bigint NOT NULL,
    value character varying(255) NOT NULL
);

CREATE TABLE public.processed_sai_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    alignment_pass_id bigint NOT NULL,
    data_file_id bigint NOT NULL,
    date_created timestamp with time zone NOT NULL,
    date_from_file_system timestamp with time zone,
    file_exists boolean NOT NULL,
    file_size bigint NOT NULL,
    deletion_date timestamp with time zone
);

CREATE TABLE public.processing_error (
    id bigint NOT NULL,
    version bigint NOT NULL,
    error_message text NOT NULL,
    stack_trace_identifier character varying(255)
);

CREATE TABLE public.processing_option (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp with time zone NOT NULL,
    date_obsoleted timestamp with time zone,
    name character varying(255) NOT NULL,
    project_id bigint,
    type character varying(255),
    value text NOT NULL
);

CREATE TABLE public.processing_step (
    id bigint NOT NULL,
    version bigint NOT NULL,
    job_class character varying(255),
    job_definition_id bigint NOT NULL,
    next_id bigint,
    previous_id bigint,
    process_id bigint NOT NULL,
    class character varying(255) NOT NULL,
    original_id bigint,
    decision_id bigint
);

CREATE TABLE public.processing_step_parameter (
    processing_step_input_id bigint,
    parameter_id bigint,
    processing_step_output_id bigint
);

CREATE TABLE public.processing_step_update (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date timestamp without time zone NOT NULL,
    error_id bigint,
    previous_id bigint,
    processing_step_id bigint NOT NULL,
    state character varying(255) NOT NULL
);

CREATE TABLE public.processing_thresholds (
    id bigint NOT NULL,
    version bigint NOT NULL,
    project_id bigint NOT NULL,
    seq_type_id bigint NOT NULL,
    sample_type_id bigint NOT NULL,
    coverage double precision,
    number_of_lanes integer,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL
);

CREATE TABLE public.project_category (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.project_contact_person (
    contact_person_id bigint NOT NULL,
    project_id bigint NOT NULL,
    id bigint NOT NULL,
    version bigint NOT NULL,
    contact_person_role_id bigint
);

CREATE TABLE public.project_group (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.project_project_categories (
    project_id bigint NOT NULL,
    project_category_id bigint NOT NULL
);

CREATE TABLE public.qc_threshold (
    id bigint NOT NULL,
    version bigint NOT NULL,
    project_id bigint,
    seq_type_id bigint,
    qc_property1 character varying(255) NOT NULL,
    qc_property2 character varying(255),
    warning_threshold double precision NOT NULL,
    error_threshold double precision NOT NULL,
    compare character varying(255) NOT NULL,
    qc_class character varying(255) NOT NULL
);

CREATE TABLE public.quality_assessment_merged_pass (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255),
    identifier integer NOT NULL,
    abstract_merged_bam_file_id bigint NOT NULL
);

CREATE TABLE public.quality_assessment_pass (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255),
    identifier integer NOT NULL,
    processed_bam_file_id bigint NOT NULL
);

CREATE TABLE public.reference_genome (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    file_name_prefix character varying(255) NOT NULL,
    length bigint NOT NULL,
    length_withoutn bigint NOT NULL,
    path character varying(255) NOT NULL,
    length_ref_chromosomes bigint NOT NULL,
    length_ref_chromosomes_withoutn bigint NOT NULL,
    cytosine_positions_index character varying(255),
    chromosome_length_file_path character varying(255),
    chromosome_prefix character varying(255),
    chromosome_suffix character varying(255),
    finger_printing_file_name character varying(255),
    mappability_file character varying(500),
    replication_time_file character varying(500),
    gc_content_file character varying(255),
    genetic_map_file character varying(500),
    known_haplotypes_file character varying(500),
    known_haplotypes_legend_file character varying(500),
    genetic_map_filex character varying(500),
    known_haplotypes_filex character varying(500),
    known_haplotypes_legend_filex character varying(500)
);

CREATE TABLE public.reference_genome_entry (
    id bigint NOT NULL,
    version bigint NOT NULL,
    alias character varying(255) NOT NULL,
    classification character varying(255) NOT NULL,
    length bigint NOT NULL,
    length_withoutn bigint NOT NULL,
    name character varying(255) NOT NULL,
    reference_genome_id bigint NOT NULL
);

CREATE TABLE public.reference_genome_index (
    id bigint NOT NULL,
    version bigint NOT NULL,
    index_tool_version character varying(255) NOT NULL,
    path character varying(255) NOT NULL,
    reference_genome_id bigint NOT NULL,
    tool_name_id bigint NOT NULL
);

CREATE TABLE public.reference_genome_project_seq_type (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp with time zone NOT NULL,
    deprecated_date timestamp with time zone,
    project_id bigint NOT NULL,
    reference_genome_id bigint NOT NULL,
    seq_type_id bigint NOT NULL,
    sample_type_id bigint,
    stat_size_file_name character varying(255)
);

CREATE TABLE public.reference_genome_project_seq_type_alignment_property (
    id bigint NOT NULL,
    version bigint NOT NULL,
    reference_genome_project_seq_type_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(500) NOT NULL
);

CREATE TABLE public.referenced_class (
    id bigint NOT NULL,
    version bigint NOT NULL,
    class_name character varying(255) NOT NULL
);

CREATE TABLE public.roddy_bam_file_roddy_execution_directory_names (
    roddy_bam_file_id bigint NOT NULL,
    roddy_execution_directory_names_string character varying(255) NOT NULL,
    roddy_execution_directory_names_idx integer
);

CREATE TABLE public.roddy_bam_file_seq_track (
    roddy_bam_file_seq_tracks_id bigint NOT NULL,
    seq_track_id bigint NOT NULL
);

CREATE TABLE public.roddy_snv_calling_instance_roddy_execution_directory_names (
    roddy_snv_calling_instance_id bigint NOT NULL,
    roddy_execution_directory_names_string character varying(255) NOT NULL,
    roddy_execution_directory_names_idx integer NOT NULL
);

CREATE TABLE public.role (
    id bigint NOT NULL,
    version bigint NOT NULL,
    authority character varying(255) NOT NULL
);

CREATE TABLE public.run_segment (
    id bigint NOT NULL,
    version bigint NOT NULL,
    align boolean,
    otrs_ticket_id bigint,
    import_mode character varying(255) DEFAULT 'MANUAL'::character varying
);

CREATE TABLE public.sample_identifier (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    sample_id bigint NOT NULL
);

CREATE TABLE public.sample_pair (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL,
    snv_processing_status character varying(255) NOT NULL,
    merging_work_package1_id bigint NOT NULL,
    merging_work_package2_id bigint NOT NULL,
    relative_path character varying(255) NOT NULL,
    indel_processing_status character varying(255) NOT NULL,
    aceseq_processing_status character varying(255) NOT NULL,
    sophia_processing_status character varying(255) NOT NULL
);

CREATE TABLE public.sample_type_per_project (
    id bigint NOT NULL,
    version bigint NOT NULL,
    project_id bigint NOT NULL,
    sample_type_id bigint NOT NULL,
    category character varying(255) NOT NULL,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL
);

CREATE TABLE public.seed_me_checksum (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp without time zone NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    seed_name character varying(255) NOT NULL,
    checksum character varying(255)
);

CREATE TABLE public.seq_platform_group (
    id bigint NOT NULL,
    version bigint NOT NULL,
    merging_criteria_id bigint
);

CREATE TABLE public.seq_platform_group_comment (
    seq_platform_group_comments_id bigint,
    comment_id bigint,
    comments_idx integer
);

CREATE TABLE public.seq_platform_model_label_alias (
    seq_platform_model_label_id bigint,
    alias_string character varying(255)
);

CREATE TABLE public.seq_platform_seq_platform_groups (
    seq_platform_id bigint NOT NULL,
    seq_platform_group_id bigint NOT NULL
);

CREATE TABLE public.seq_scan (
    id bigint NOT NULL,
    version bigint NOT NULL,
    coverage double precision NOT NULL,
    date_created timestamp without time zone NOT NULL,
    insert_size character varying(255),
    n_base_pairs bigint,
    n_lanes integer NOT NULL,
    qc_state character varying(255) NOT NULL,
    sample_id bigint NOT NULL,
    seq_centers character varying(255) NOT NULL,
    seq_platform_id bigint NOT NULL,
    seq_type_id bigint NOT NULL,
    state character varying(255) NOT NULL
);

CREATE TABLE public.seq_track_log_message (
    seq_track_log_messages_id bigint NOT NULL,
    log_message_id bigint NOT NULL,
    log_messages_idx integer NOT NULL
);

CREATE TABLE public.seq_type_alias (
    seq_type_id bigint,
    alias_string character varying(255)
);

CREATE VIEW public.sequences AS
 SELECT st.id AS seq_track_id,
    st.seq_type_id,
    r.seq_platform_id,
    sp.seq_platform_model_label_id,
    sp.sequencing_kit_label_id,
    st.sample_id,
    st.run_id,
    st.pipeline_version_id,
    ilse.ilse_number AS ilse_id,
    r.seq_center_id,
    s.sample_type_id,
    s.individual_id,
    p.id AS project_id,
    re.id AS realm_id,
    st.quality_encoding,
    st.fastqc_state,
    st.n_base_pairs,
    st.lane_id,
    st.library_name,
    st.insert_size,
    st.has_original_bam,
    st.problem,
    lpk.short_display_name AS library_preparation_kit,
    r.name,
    r.date_executed,
    r.date_created,
    r.blacklisted,
    sp.name AS seq_platform_name,
    spml.name AS seq_platform_model_label_name,
    skl.name AS sequencing_kit_label_name,
    seqtype.name AS seq_type_name,
    seqtype.display_name AS seq_type_display_name,
    seqtype.library_layout,
    seqtype.dir_name,
    sampletype.name AS sample_type_name,
    i.type,
    i.pid,
    i.mock_pid,
    i.mock_full_name,
    re.name AS realm_name,
    p.name AS project_name,
    p.dir_name AS project_dir_name,
    sc.name AS seq_center_name,
    sc.dir_name AS seq_center_dir_name
   FROM (((((((((((((public.seq_track st
     JOIN public.run r ON ((r.id = st.run_id)))
     JOIN public.seq_platform sp ON ((sp.id = r.seq_platform_id)))
     JOIN public.seq_type seqtype ON ((seqtype.id = st.seq_type_id)))
     JOIN public.sample s ON ((s.id = st.sample_id)))
     JOIN public.sample_type sampletype ON ((s.sample_type_id = sampletype.id)))
     JOIN public.individual i ON ((s.individual_id = i.id)))
     JOIN public.project p ON ((i.project_id = p.id)))
     JOIN public.realm re ON ((p.realm_id = re.id)))
     JOIN public.seq_center sc ON ((r.seq_center_id = sc.id)))
     LEFT JOIN public.ilse_submission ilse ON ((st.ilse_submission_id = ilse.id)))
     LEFT JOIN public.seq_platform_model_label spml ON ((sp.seq_platform_model_label_id = spml.id)))
     LEFT JOIN public.sequencing_kit_label skl ON ((sp.sequencing_kit_label_id = skl.id)))
     LEFT JOIN public.library_preparation_kit lpk ON ((st.library_preparation_kit_id = lpk.id)));

CREATE TABLE public.sequencing_kit_label_alias (
    sequencing_kit_label_id bigint,
    alias_string character varying(255)
);

CREATE TABLE public.shutdown_information (
    id bigint NOT NULL,
    version bigint NOT NULL,
    canceled timestamp without time zone,
    canceled_by_id bigint,
    initiated timestamp without time zone NOT NULL,
    initiated_by_id bigint NOT NULL,
    reason character varying(255) NOT NULL,
    succeeded timestamp without time zone
);

CREATE TABLE public.snv_job_result (
    id bigint NOT NULL,
    version bigint NOT NULL,
    step character varying(255) NOT NULL,
    processing_state character varying(255) NOT NULL,
    input_result_id bigint,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone NOT NULL,
    withdrawn boolean NOT NULL,
    snv_calling_instance_id bigint NOT NULL,
    external_script_id bigint NOT NULL,
    chromosome_join_external_script_id bigint,
    md5sum character varying(255),
    file_size bigint
);

CREATE TABLE public.software_tool (
    id bigint NOT NULL,
    version bigint NOT NULL,
    program_name character varying(255) NOT NULL,
    program_version character varying(255),
    type character varying(255) NOT NULL
);

CREATE TABLE public.software_tool_identifier (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    software_tool_id bigint NOT NULL
);

CREATE TABLE public.sophia_instance_roddy_execution_directory_names (
    sophia_instance_id bigint NOT NULL,
    roddy_execution_directory_names_string character varying(255) NOT NULL,
    roddy_execution_directory_names_idx integer NOT NULL
);

CREATE TABLE public.sophia_qc (
    id bigint NOT NULL,
    version bigint NOT NULL,
    control_massive_inv_prefiltering_level integer NOT NULL,
    tumor_massive_inv_filtering_level integer NOT NULL,
    rna_contaminated_genes_more_than_two_intron character varying NOT NULL,
    rna_contaminated_genes_count integer NOT NULL,
    rna_decontamination_applied boolean,
    sophia_instance_id bigint NOT NULL
);

CREATE TABLE public.stat_size_file_name (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    reference_genome_id bigint NOT NULL
);

CREATE TABLE public.swap_info (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp with time zone NOT NULL,
    date_finished timestamp with time zone,
    comment text NOT NULL,
    description_of_changes text NOT NULL,
    user_id bigint
);

CREATE TABLE public.swap_info_seq_track (
    swap_info_seq_tracks_id bigint NOT NULL,
    seq_track_id bigint NOT NULL
);

CREATE TABLE public.tool_name (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    path character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);

CREATE TABLE public.tumor_entity (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE public.user_role (
    role_id bigint NOT NULL,
    user_id bigint NOT NULL
);

CREATE TABLE public.users (
    id bigint NOT NULL,
    version bigint NOT NULL,
    account_expired boolean NOT NULL,
    account_locked boolean NOT NULL,
    email character varying(255),
    enabled boolean NOT NULL,
    password character varying(255) NOT NULL,
    password_expired boolean NOT NULL,
    username character varying(255) NOT NULL,
    date_created timestamp with time zone NOT NULL,
    last_updated timestamp with time zone
);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT "abstract_bam_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.abstract_quality_assessment
    ADD CONSTRAINT "abstract_qualPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.aceseq_qc
    ADD CONSTRAINT "aceseq_qc_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.acl_class
    ADD CONSTRAINT acl_class_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.acl_entry
    ADD CONSTRAINT acl_entry_acl_object_identity_ace_order_key UNIQUE (acl_object_identity, ace_order);

ALTER TABLE ONLY public.acl_entry
    ADD CONSTRAINT acl_entry_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.acl_object_identity
    ADD CONSTRAINT acl_object_identity_object_id_class_object_id_identity_key UNIQUE (object_id_class, object_id_identity);

ALTER TABLE ONLY public.acl_object_identity
    ADD CONSTRAINT acl_object_identity_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.acl_sid
    ADD CONSTRAINT acl_sid_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.acl_sid
    ADD CONSTRAINT acl_sid_sid_principal_key UNIQUE (sid, principal);

ALTER TABLE ONLY public.alignment_log
    ADD CONSTRAINT alignment_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.alignment_params
    ADD CONSTRAINT alignment_params_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.alignment_pass
    ADD CONSTRAINT "alignment_pasPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.antibody_target
    ADD CONSTRAINT "antibody_targPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.bed_file
    ADD CONSTRAINT "bed_filePK" PRIMARY KEY (id);

ALTER TABLE ONLY public.change_log
    ADD CONSTRAINT change_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.cluster_job
    ADD CONSTRAINT "cluster_jobPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.comment
    ADD CONSTRAINT comment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.config_per_project
    ADD CONSTRAINT "config_per_project_and_seq_type_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.consistency_check
    ADD CONSTRAINT "consistency_cPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.consistency_status
    ADD CONSTRAINT "consistency_sPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.contact_person
    ADD CONSTRAINT contact_person_full_name_key UNIQUE (full_name);

ALTER TABLE ONLY public.contact_person
    ADD CONSTRAINT contact_person_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.contact_person_role
    ADD CONSTRAINT contact_person_role_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT data_file_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.decision_mapping
    ADD CONSTRAINT decision_mapping_decision_id_key UNIQUE (decision_id);

ALTER TABLE ONLY public.decision_mapping
    ADD CONSTRAINT decision_mapping_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.library_preparation_kit_synonym
    ADD CONSTRAINT "exome_enricIdPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.library_preparation_kit
    ADD CONSTRAINT "exome_enrichmPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.external_script
    ADD CONSTRAINT "external_script_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.external_script
    ADD CONSTRAINT external_script_file_path_key UNIQUE (file_path);

ALTER TABLE ONLY public.fastqc_processed_file
    ADD CONSTRAINT "fastqc_procesPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.fastqc_processed_file
    ADD CONSTRAINT fastqc_processed_file_data_file_id_unique UNIQUE (data_file_id);

ALTER TABLE ONLY public.file_system_changes
    ADD CONSTRAINT file_system_changes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.file_type
    ADD CONSTRAINT file_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.gene_model
    ADD CONSTRAINT gene_model__path_reference_genome_id_unique UNIQUE (reference_genome_id, path);

ALTER TABLE ONLY public.gene_model
    ADD CONSTRAINT gene_model_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_name_key UNIQUE (name);

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_role_id_key UNIQUE (role_id);

ALTER TABLE ONLY public.ilse_submission
    ADD CONSTRAINT ilse_submission_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.import_process
    ADD CONSTRAINT import_process_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.indel_quality_control
    ADD CONSTRAINT indel_quality_control_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.indel_sample_swap_detection
    ADD CONSTRAINT indel_sample_swap_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.individual
    ADD CONSTRAINT individual_pid_key UNIQUE (pid);

ALTER TABLE ONLY public.individual
    ADD CONSTRAINT individual_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.job_decision
    ADD CONSTRAINT job_decision_job_definition_id_name_key UNIQUE (job_definition_id, name);

ALTER TABLE ONLY public.job_decision
    ADD CONSTRAINT job_decision_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.job_definition
    ADD CONSTRAINT job_definition_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.job_definition
    ADD CONSTRAINT job_definition_plan_id_name_key UNIQUE (plan_id, name);

ALTER TABLE ONLY public.job_error_definition_job_definition
    ADD CONSTRAINT job_error_definition_job_definition_pkey PRIMARY KEY (job_error_definition_job_definitions_id, job_definition_id);

ALTER TABLE ONLY public.job_error_definition_job_error_definition
    ADD CONSTRAINT job_error_definition_job_error_definition_pkey PRIMARY KEY (job_error_definition_id, job_error_definition_check_further_job_errors_id);

ALTER TABLE ONLY public.job_error_definition
    ADD CONSTRAINT job_error_definition_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.job_execution_plan
    ADD CONSTRAINT job_execution_plan_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.job_execution_plan
    ADD CONSTRAINT job_execution_plan_plan_version_name_key UNIQUE (plan_version, name);

ALTER TABLE ONLY public.library_preparation_kit
    ADD CONSTRAINT library_preparation_kit_short_display_name_key UNIQUE (short_display_name);

ALTER TABLE ONLY public.log_message
    ADD CONSTRAINT log_message_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.merged_alignment_data_file
    ADD CONSTRAINT merged_alignment_data_file_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.merging_assignment
    ADD CONSTRAINT merging_assignment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.merging_criteria
    ADD CONSTRAINT merging_criteria_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.merging_log
    ADD CONSTRAINT merging_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.merging_pass
    ADD CONSTRAINT "merging_passPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.merging_set
    ADD CONSTRAINT "merging_setPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.merging_set_assignment
    ADD CONSTRAINT "merging_set_aPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT "merging_work_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.merging_work_package_alignment_property
    ADD CONSTRAINT merging_work_package_alignment_property_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.meta_data_entry
    ADD CONSTRAINT meta_data_entry_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.meta_data_file
    ADD CONSTRAINT meta_data_file_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.meta_data_key
    ADD CONSTRAINT meta_data_key_name_key UNIQUE (name);

ALTER TABLE ONLY public.meta_data_key
    ADD CONSTRAINT meta_data_key_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.otrs_ticket
    ADD CONSTRAINT otrs_ticket_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.otrs_ticket
    ADD CONSTRAINT otrs_ticket_ticket_number_key UNIQUE (ticket_number);

ALTER TABLE ONLY public.parameter_mapping
    ADD CONSTRAINT parameter_mapping_from_id_to_id_key UNIQUE (from_id, to_id);

ALTER TABLE ONLY public.parameter_mapping
    ADD CONSTRAINT parameter_mapping_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.parameter
    ADD CONSTRAINT parameter_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.parameter_type
    ADD CONSTRAINT parameter_type_parameter_usage_job_definition_id_name_key UNIQUE (parameter_usage, job_definition_id, name);

ALTER TABLE ONLY public.parameter_type
    ADD CONSTRAINT parameter_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.picard_mark_duplicates_metrics
    ADD CONSTRAINT "picard_mark_dPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.process_parameter
    ADD CONSTRAINT process_parameter_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.process
    ADD CONSTRAINT process_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.process
    ADD CONSTRAINT process_restarted_id_key UNIQUE (restarted_id);

ALTER TABLE ONLY public.processed_sai_file
    ADD CONSTRAINT "processed_saiPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.processing_error
    ADD CONSTRAINT processing_error_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.processing_option
    ADD CONSTRAINT "processing_opPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.processing_step
    ADD CONSTRAINT processing_step_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.processing_step_update
    ADD CONSTRAINT processing_step_update_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.processing_thresholds
    ADD CONSTRAINT "processing_thresholds_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.project_category
    ADD CONSTRAINT project_category_name_key UNIQUE (name);

ALTER TABLE ONLY public.project_category
    ADD CONSTRAINT project_category_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.project_contact_person
    ADD CONSTRAINT project_contact_person_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.project_group
    ADD CONSTRAINT "project_groupPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_name_key UNIQUE (name);

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.project_project_categories
    ADD CONSTRAINT project_project_categories_pkey PRIMARY KEY (project_id, project_category_id);

ALTER TABLE ONLY public.quality_assessment_merged_pass
    ADD CONSTRAINT "quality_assMePK" PRIMARY KEY (id);

ALTER TABLE ONLY public.quality_assessment_pass
    ADD CONSTRAINT "quality_assesPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT realm_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reference_genome_entry
    ADD CONSTRAINT "reference_genPK" PRIMARY KEY (id);

ALTER TABLE ONLY public.reference_genome_index
    ADD CONSTRAINT reference_genome_index__reference_genome_id_tool_name_id_index_ UNIQUE (reference_genome_id, tool_name_id, index_tool_version);

ALTER TABLE ONLY public.reference_genome_index
    ADD CONSTRAINT reference_genome_index__reference_genome_id_tool_name_id_path_u UNIQUE (reference_genome_id, tool_name_id, path);

ALTER TABLE ONLY public.reference_genome_index
    ADD CONSTRAINT reference_genome_index_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reference_genome
    ADD CONSTRAINT reference_genome_path_key UNIQUE (path);

ALTER TABLE ONLY public.reference_genome
    ADD CONSTRAINT reference_genome_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reference_genome_project_seq_type_alignment_property
    ADD CONSTRAINT reference_genome_project_seq_type_alignment_property_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reference_genome_project_seq_type
    ADD CONSTRAINT reference_genome_project_seq_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.referenced_class
    ADD CONSTRAINT referenced_class_class_name_key UNIQUE (class_name);

ALTER TABLE ONLY public.referenced_class
    ADD CONSTRAINT referenced_class_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roddy_bam_file_seq_track
    ADD CONSTRAINT roddy_bam_file_seq_track_roddy_bam_file_seq_tracks_id_seq_t_key UNIQUE (roddy_bam_file_seq_tracks_id, seq_track_id);

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_authority_key UNIQUE (authority);

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.run
    ADD CONSTRAINT run_name_key UNIQUE (name);

ALTER TABLE ONLY public.run
    ADD CONSTRAINT run_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.run_segment
    ADD CONSTRAINT run_segment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sample_identifier
    ADD CONSTRAINT sample_identifier_name_key UNIQUE (name);

ALTER TABLE ONLY public.sample_identifier
    ADD CONSTRAINT sample_identifier_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT sample_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sample_pair
    ADD CONSTRAINT "sample_type_combination_per_individual_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.sample_type
    ADD CONSTRAINT sample_type_name_key UNIQUE (name);

ALTER TABLE ONLY public.sample_type_per_project
    ADD CONSTRAINT "sample_type_per_project_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.sample_type
    ADD CONSTRAINT sample_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seed_me_checksum
    ADD CONSTRAINT seed_me_checksum_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seq_center
    ADD CONSTRAINT seq_center_dir_name_key UNIQUE (dir_name);

ALTER TABLE ONLY public.seq_center
    ADD CONSTRAINT seq_center_name_key UNIQUE (name);

ALTER TABLE ONLY public.seq_center
    ADD CONSTRAINT seq_center_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seq_platform_group
    ADD CONSTRAINT seq_platform_group_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seq_platform_model_label
    ADD CONSTRAINT seq_platform_model_label_name_key UNIQUE (name);

ALTER TABLE ONLY public.seq_platform_model_label
    ADD CONSTRAINT seq_platform_model_label_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seq_platform
    ADD CONSTRAINT seq_platform_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seq_platform_seq_platform_groups
    ADD CONSTRAINT seq_platform_seq_platform_groups_pkey PRIMARY KEY (seq_platform_id, seq_platform_group_id);

ALTER TABLE ONLY public.seq_scan
    ADD CONSTRAINT seq_scan_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seq_track_log_message
    ADD CONSTRAINT seq_track_log_message_pkey PRIMARY KEY (seq_track_log_messages_id, log_message_id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT seq_track_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.seq_type
    ADD CONSTRAINT seq_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sequencing_kit_label
    ADD CONSTRAINT sequencing_kit_label_name_key UNIQUE (name);

ALTER TABLE ONLY public.sequencing_kit_label
    ADD CONSTRAINT sequencing_kit_label_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.shutdown_information
    ADD CONSTRAINT shutdown_information_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.bam_file_pair_analysis
    ADD CONSTRAINT "snv_calling_instance_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.software_tool_identifier
    ADD CONSTRAINT software_tool_identifier_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.software_tool
    ADD CONSTRAINT software_tool_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sophia_qc
    ADD CONSTRAINT "sophia_qc_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.stat_size_file_name
    ADD CONSTRAINT stat_size_file_name_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.swap_info
    ADD CONSTRAINT swap_info_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.swap_info_seq_track
    ADD CONSTRAINT swap_info_seq_track_pkey PRIMARY KEY (swap_info_seq_tracks_id, seq_track_id);

ALTER TABLE ONLY public.tool_name
    ADD CONSTRAINT tool_name__name_unique UNIQUE (name);

ALTER TABLE ONLY public.tool_name
    ADD CONSTRAINT tool_name__path_unique UNIQUE (path);

ALTER TABLE ONLY public.tool_name
    ADD CONSTRAINT tool_name_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tumor_entity
    ADD CONSTRAINT tumor_entity_name_key UNIQUE (name);

ALTER TABLE ONLY public.tumor_entity
    ADD CONSTRAINT tumor_entity_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.acl_entry
    ADD CONSTRAINT unique_ace_order UNIQUE (acl_object_identity, ace_order);

ALTER TABLE ONLY public.external_script
    ADD CONSTRAINT unique_deprecated_date UNIQUE (script_version, script_identifier, deprecated_date);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT unique_identifier UNIQUE (work_package_id, identifier);

ALTER TABLE ONLY public.sample_pair
    ADD CONSTRAINT unique_merging_work_package2_id UNIQUE (merging_work_package1_id, merging_work_package2_id);

ALTER TABLE ONLY public.acl_object_identity
    ADD CONSTRAINT unique_object_id_identity UNIQUE (object_id_class, object_id_identity);

ALTER TABLE ONLY public.acl_sid
    ADD CONSTRAINT unique_principal UNIQUE (sid, principal);

ALTER TABLE ONLY public.merging_criteria
    ADD CONSTRAINT unique_project_id UNIQUE (seq_type_id, project_id);

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT unique_realm_name_constraint UNIQUE (name);

ALTER TABLE ONLY public.parameter_mapping
    ADD CONSTRAINT unique_to_id UNIQUE (from_id, to_id);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT unique_work_directory_name UNIQUE (work_package_id, work_directory_name);

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (role_id, user_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);

ALTER TABLE ONLY public.snv_job_result
    ADD CONSTRAINT "vcf_file_PK" PRIMARY KEY (id);

ALTER TABLE ONLY public.pipeline
    ADD CONSTRAINT workflow_name_type_key UNIQUE (name, type);

ALTER TABLE ONLY public.pipeline
    ADD CONSTRAINT workflow_pkey PRIMARY KEY (id);

CREATE INDEX abstract_bam_file_alignment_pass_idx ON public.abstract_bam_file USING btree (alignment_pass_id);

CREATE INDEX abstract_bam_file_base_bam_file_idx ON public.abstract_bam_file USING btree (base_bam_file_id);

CREATE INDEX abstract_bam_file_class_idx ON public.abstract_bam_file USING btree (class);

CREATE INDEX abstract_bam_file_config_idx ON public.abstract_bam_file USING btree (config_id);

CREATE INDEX abstract_bam_file_merging_pass_idx ON public.abstract_bam_file USING btree (merging_pass_id);

CREATE INDEX abstract_bam_file_qc_traffic_light_status ON public.abstract_bam_file USING btree (qc_traffic_light_status);

CREATE INDEX abstract_bam_file_quality_assessment_status_idx ON public.abstract_bam_file USING btree (quality_assessment_status);

CREATE INDEX abstract_bam_file_withdrawn_idx ON public.abstract_bam_file USING btree (withdrawn);

CREATE UNIQUE INDEX abstract_bam_file_work_directory_name_work_package_idx ON public.abstract_bam_file USING btree (work_directory_name, work_package_id);

CREATE INDEX abstract_bam_file_work_package_idx ON public.abstract_bam_file USING btree (work_package_id);

CREATE INDEX abstract_merged_bam_file_number_of_merged_lanes_idx ON public.abstract_bam_file USING btree (number_of_merged_lanes);

CREATE INDEX abstract_merged_bam_file_work_package_idx ON public.abstract_bam_file USING btree (work_package_id);

CREATE INDEX abstract_merging_work_package_antibody_target_idx ON public.abstract_merging_work_package USING btree (antibody_target_id);

CREATE INDEX abstract_merging_work_package_bam_file_in_project_folder_idx ON public.abstract_merging_work_package USING btree (bam_file_in_project_folder_id);

CREATE INDEX abstract_merging_work_package_class_idx ON public.abstract_merging_work_package USING btree (class);

CREATE INDEX abstract_merging_work_package_reference_genome_idx ON public.abstract_merging_work_package USING btree (reference_genome_id);

CREATE INDEX abstract_merging_work_package_sample_seq_type_idx ON public.abstract_merging_work_package USING btree (sample_id, seq_type_id);

CREATE INDEX abstract_merging_work_package_seq_type_idx ON public.abstract_merging_work_package USING btree (seq_type_id);

CREATE INDEX abstract_quality_assessment_chromosome_name_idx ON public.abstract_quality_assessment USING btree (chromosome_name);

CREATE INDEX abstract_quality_assessment_class_idx ON public.abstract_quality_assessment USING btree (class);

CREATE INDEX abstract_quality_assessment_quality_assessment_merged_pass_idx ON public.abstract_quality_assessment USING btree (quality_assessment_merged_pass_id);

CREATE INDEX abstract_quality_assessment_quality_assessment_pass_idx ON public.abstract_quality_assessment USING btree (quality_assessment_pass_id);

CREATE UNIQUE INDEX aceseq_instance_idx_roddy_execution_directory_names_idx ON public.aceseq_instance_roddy_execution_directory_names USING btree (aceseq_instance_id, roddy_execution_directory_names_idx);

CREATE UNIQUE INDEX aceseq_qc__number_aceseq_instance_id__unique ON public.aceseq_qc USING btree (aceseq_instance_id, number);

CREATE UNIQUE INDEX acl_class__class_idx ON public.acl_class USING btree (class);

CREATE INDEX alignment_log_alignment_param_idx ON public.alignment_log USING btree (alignment_params_id);

CREATE INDEX alignment_log_seq_track_idx ON public.alignment_log USING btree (seq_track_id);

CREATE INDEX alignment_pass_alignment_state_idx ON public.alignment_pass USING btree (alignment_state) WHERE ((alignment_state)::text = 'NOT_STARTED'::text);

CREATE UNIQUE INDEX alignment_pass_seq_track_id_identifier_unique ON public.alignment_pass USING btree (seq_track_id, identifier);

CREATE INDEX alignment_pass_seq_track_idx ON public.alignment_pass USING btree (seq_track_id);

CREATE INDEX alignment_pass_work_package_idx ON public.alignment_pass USING btree (work_package_id);

CREATE INDEX bam_file_pair_analysis_qc_traffic_light_status ON public.bam_file_pair_analysis USING btree (qc_traffic_light_status);

CREATE INDEX bam_file_pair_analysis_sample_pair_idx ON public.bam_file_pair_analysis USING btree (sample_pair_id);

CREATE INDEX bam_file_pair_analysis_sample_type_1_bam_file_idx ON public.bam_file_pair_analysis USING btree (sample_type1bam_file_id);

CREATE INDEX bam_file_pair_analysis_sample_type_2_bam_file_idx ON public.bam_file_pair_analysis USING btree (sample_type2bam_file_id);

CREATE INDEX bed_file_library_preparation_kit_idx ON public.bed_file USING btree (library_preparation_kit_id);

CREATE UNIQUE INDEX bed_file_reference_genome_id_file_name_idx ON public.bed_file USING btree (reference_genome_id, file_name);

CREATE INDEX bed_file_reference_genome_idx ON public.bed_file USING btree (reference_genome_id);

CREATE INDEX cluster_job_cluster_job__cluster_job_dependencies_id__idx ON public.cluster_job_cluster_job USING btree (cluster_job_dependencies_id);

CREATE INDEX cluster_job_cluster_job__cluster_job_id__idx ON public.cluster_job_cluster_job USING btree (cluster_job_id);

CREATE INDEX cluster_job_cluster_job_id_idx ON public.cluster_job USING btree (cluster_job_id);

CREATE INDEX cluster_job_cluster_job_name_idx ON public.cluster_job USING btree (cluster_job_name);

CREATE INDEX cluster_job_processing_step_idx ON public.cluster_job USING btree (processing_step_id);

CREATE UNIQUE INDEX config_per_project_config_file_path_idx ON public.config_per_project USING btree (config_file_path);

CREATE UNIQUE INDEX config_per_project_project_id_seq_type_id_obsolete_date_idx ON public.config_per_project USING btree (project_id, seq_type_id, obsolete_date) WHERE ((obsolete_date IS NULL) AND ((class)::text = 'de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig'::text));

CREATE INDEX config_per_project_project_seqtype_pipeline_individual_idx ON public.config_per_project USING btree (project_id, seq_type_id, pipeline_id, individual_id) WHERE ((class)::text = 'de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig'::text);

CREATE UNIQUE INDEX contact_person_role_name_key ON public.contact_person_role USING btree (name);

CREATE INDEX data_file_alignment_log_idx ON public.data_file USING btree (alignment_log_id);

CREATE INDEX data_file_file_type_idx ON public.data_file USING btree (file_type_id);

CREATE INDEX data_file_merging_log_idx ON public.data_file USING btree (merging_log_id);

CREATE INDEX data_file_run_idx ON public.data_file USING btree (run_id);

CREATE INDEX data_file_run_segment_idx ON public.data_file USING btree (run_segment_id);

CREATE INDEX data_file_seq_track_idx ON public.data_file USING btree (seq_track_id);

CREATE INDEX date_idx ON public.processing_step_update USING btree (date);

CREATE INDEX document__name_idx ON public.document USING btree (name);

CREATE UNIQUE INDEX external_script_script_identifier_script_version_deprecated_idx ON public.external_script USING btree (script_identifier, script_version, deprecated_date) WHERE (deprecated_date IS NULL);

CREATE INDEX fastqc_processed_file_data_file_idx ON public.fastqc_processed_file USING btree (data_file_id);

CREATE UNIQUE INDEX file_path_uniq_1412681982199 ON public.external_script USING btree (file_path);

CREATE INDEX finished_idx ON public.process USING btree (finished);

CREATE UNIQUE INDEX ilse_submission_ilse_number_idx ON public.ilse_submission USING btree (ilse_number);

CREATE UNIQUE INDEX import_process_id_externally_processed_merged_bam_files_id_idx ON public.import_process_externally_processed_merged_bam_file USING btree (import_process_externally_processed_merged_bam_files_id, externally_processed_merged_bam_file_id);

CREATE UNIQUE INDEX indel_calling_instance_idx_roddy_execution_directory_names_idx ON public.indel_calling_instance_roddy_execution_directory_names USING btree (indel_calling_instance_id, roddy_execution_directory_names_idx);

CREATE UNIQUE INDEX indel_sample_swap_detection_indel_calling_instance_idx ON public.indel_sample_swap_detection USING btree (indel_calling_instance_id);

CREATE INDEX individual_project_idx ON public.individual USING btree (project_id);

CREATE INDEX job_execution_plan_idx ON public.process USING btree (job_execution_plan_id);

CREATE INDEX library_preparation_kit_synonym_library_preparation_kit_idx ON public.library_preparation_kit_synonym USING btree (library_preparation_kit_id);

CREATE INDEX merged_alignment_data_file_merging_log_idx ON public.merged_alignment_data_file USING btree (merging_log_id);

CREATE INDEX merging_assignment_seq_track_id ON public.merging_assignment USING btree (seq_track_id);

CREATE INDEX merging_log_alignment_params_idx ON public.merging_log USING btree (alignment_params_id);

CREATE INDEX merging_log_seq_scan_idx ON public.merging_log USING btree (seq_scan_id);

CREATE INDEX merging_pass_merging_set_idx ON public.merging_pass USING btree (merging_set_id);

CREATE UNIQUE INDEX merging_pass_mergingset_id_identifier_unique ON public.merging_pass USING btree (merging_set_id, identifier);

CREATE INDEX merging_set_assignment_bam_file_idx ON public.merging_set_assignment USING btree (bam_file_id);

CREATE INDEX merging_set_assignment_merging_set_idx ON public.merging_set_assignment USING btree (merging_set_id);

CREATE UNIQUE INDEX merging_set_merging_work_package_id_identifier_unique ON public.merging_set USING btree (merging_work_package_id, identifier);

CREATE INDEX merging_set_merging_work_package_idx ON public.merging_set USING btree (merging_work_package_id);

CREATE UNIQUE INDEX merging_work_package_alignment_properties_unique_idx ON public.merging_work_package_alignment_property USING btree (merging_work_package_id, name);

CREATE INDEX merging_work_package_needs_processing_idx ON public.abstract_merging_work_package USING btree (needs_processing) WHERE (needs_processing = true);

CREATE UNIQUE INDEX merging_work_package_seq_track_unique_idx ON public.merging_work_package_seq_track USING btree (merging_work_package_seq_tracks_id, seq_track_id);

CREATE INDEX merging_work_package_workflow_idx ON public.abstract_merging_work_package USING btree (pipeline_id);

CREATE INDEX meta_data_entry_data_file_idx ON public.meta_data_entry USING btree (data_file_id);

CREATE INDEX meta_data_entry_key_idx ON public.meta_data_entry USING btree (key_id);

CREATE INDEX meta_data_file_run_segment_idx ON public.meta_data_file USING btree (run_segment_id);

CREATE UNIQUE INDEX name_uniq_1377853124899 ON public.reference_genome USING btree (name);

CREATE UNIQUE INDEX name_uniq_1383572139513 ON public.library_preparation_kit USING btree (name);

CREATE UNIQUE INDEX name_uniq_1383572139514 ON public.library_preparation_kit_synonym USING btree (name);

CREATE UNIQUE INDEX name_uniq_1383572139560 ON public.meta_data_key USING btree (name);

CREATE UNIQUE INDEX name_uniq_1386591410112 ON public.antibody_target USING btree (name);

CREATE UNIQUE INDEX name_uniq_1386591410234 ON public.project_group USING btree (name);

CREATE UNIQUE INDEX name_uniq_1415890817960 ON public.meta_data_key USING btree (name);

CREATE INDEX next_idx ON public.processing_step USING btree (next_id);

CREATE UNIQUE INDEX path_uniq_1377853124900 ON public.reference_genome USING btree (path);

CREATE INDEX picard_mark_duplicates_metrics_abstract_bam_file_idx ON public.picard_mark_duplicates_metrics USING btree (abstract_bam_file_id);

CREATE INDEX process_idx ON public.processing_step USING btree (process_id);

CREATE INDEX process_parameter_process_idx ON public.process_parameter USING btree (process_id);

CREATE INDEX processed_sai_file_alignemt_pass_idx ON public.processed_sai_file USING btree (alignment_pass_id);

CREATE INDEX processed_sai_file_data_file_idx ON public.processed_sai_file USING btree (data_file_id);

CREATE INDEX processing_step_idx ON public.processing_step_update USING btree (processing_step_id);

CREATE INDEX processing_thresholds_project_idx ON public.processing_thresholds USING btree (project_id);

CREATE INDEX processing_thresholds_project_seq_type_sample_type_idx ON public.processing_thresholds USING btree (project_id, seq_type_id, sample_type_id);

CREATE INDEX processing_thresholds_sample_type_idx ON public.processing_thresholds USING btree (sample_type_id);

CREATE INDEX processing_thresholds_seq_type_idx ON public.processing_thresholds USING btree (seq_type_id);

CREATE UNIQUE INDEX project__dir_name_idx ON public.project USING btree (dir_name);

CREATE UNIQUE INDEX project_contact_person_contact_person_idx ON public.project_contact_person USING btree (contact_person_id, project_id);

CREATE INDEX project_idx ON public.sample_type_per_project USING btree (project_id);

CREATE INDEX project_processing_priority_idx ON public.project USING btree (processing_priority);

CREATE INDEX project_project_group_idx ON public.project USING btree (project_group_id);

CREATE INDEX qc_threshold_project_idx ON public.qc_threshold USING btree (project_id);

CREATE INDEX qc_threshold_seqtype_idx ON public.qc_threshold USING btree (seq_type_id);

CREATE INDEX quality_assessment_merged_pass_abstract_merged_bam_file_idx ON public.quality_assessment_merged_pass USING btree (abstract_merged_bam_file_id);

CREATE UNIQUE INDEX quality_assessment_merged_pass_processed_merged_bam_file_id_ide ON public.quality_assessment_merged_pass USING btree (abstract_merged_bam_file_id, identifier);

CREATE UNIQUE INDEX quality_assessment_pass_processed_bam_file_id_identifier_unique ON public.quality_assessment_pass USING btree (processed_bam_file_id, identifier);

CREATE INDEX quality_assessment_pass_processed_bam_file_idx ON public.quality_assessment_pass USING btree (processed_bam_file_id);

CREATE INDEX reference_genome_entry_reference_genome_idx ON public.reference_genome_entry USING btree (reference_genome_id);

CREATE UNIQUE INDEX reference_genome_project_seq_type_alignment_properties_unique_i ON public.reference_genome_project_seq_type_alignment_property USING btree (reference_genome_project_seq_type_id, name);

CREATE INDEX reference_genome_project_seq_type_project_idx ON public.reference_genome_project_seq_type USING btree (project_id);

CREATE INDEX reference_genome_project_seq_type_reference_genome_idx ON public.reference_genome_project_seq_type USING btree (reference_genome_id);

CREATE INDEX reference_genome_project_seq_type_sample_type_idx ON public.reference_genome_project_seq_type USING btree (sample_type_id);

CREATE INDEX reference_genome_project_seq_type_seq_type_idx ON public.reference_genome_project_seq_type USING btree (seq_type_id);

CREATE INDEX roddy_bam_file_base_bam_file_idx ON public.abstract_bam_file USING btree (base_bam_file_id);

CREATE INDEX roddy_bam_file_config_idx ON public.abstract_bam_file USING btree (config_id);

CREATE INDEX run_segment_otrs_ticket_idx ON public.run_segment USING btree (otrs_ticket_id);

CREATE INDEX run_seq_center_idx ON public.run USING btree (seq_center_id);

CREATE INDEX run_seq_platform_idx ON public.run USING btree (seq_platform_id);

CREATE INDEX sample_identifier_sample_idx ON public.sample_identifier USING btree (sample_id);

CREATE UNIQUE INDEX sample_individual_id_sample_type_id_unique_idx ON public.sample USING btree (individual_id, sample_type_id);

CREATE INDEX sample_pair_aceseq_idx1 ON public.sample_pair USING btree (aceseq_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_pair_indel_idx1 ON public.sample_pair USING btree (indel_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE UNIQUE INDEX sample_pair_merging_work_package1_id_merging_work_package2__idx ON public.sample_pair USING btree (merging_work_package1_id, merging_work_package2_id);

CREATE UNIQUE INDEX sample_pair_relative_path_idx ON public.sample_pair USING btree (relative_path);

CREATE INDEX sample_pair_snv_idx1 ON public.sample_pair USING btree (snv_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_pair_sophia_idx1 ON public.sample_pair USING btree (sophia_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_sample_type_idx ON public.sample USING btree (sample_type_id);

CREATE INDEX sample_type_idx ON public.sample_type_per_project USING btree (sample_type_id);

CREATE UNIQUE INDEX seed_me_checksum_seed_name_idx ON public.seed_me_checksum USING btree (seed_name);

CREATE INDEX seq_platform_group_comments_id_idx ON public.seq_platform_group_comment USING btree (seq_platform_group_comments_id);

CREATE INDEX seq_platform_identifier_in_run_name_idx ON public.seq_platform USING btree (identifier_in_run_name);

CREATE UNIQUE INDEX seq_platform_name_sequencing_kit_label_id_seq_platform_mode_idx ON public.seq_platform USING btree (name, sequencing_kit_label_id, seq_platform_model_label_id);

CREATE INDEX seq_platform_seq_platform_model_label_idx ON public.seq_platform USING btree (seq_platform_model_label_id);

CREATE INDEX seq_platform_sequencing_kit_label_idx ON public.seq_platform USING btree (sequencing_kit_label_id);

CREATE INDEX seq_scan_sample_idx ON public.seq_scan USING btree (sample_id);

CREATE INDEX seq_scan_seq_platform_idx ON public.seq_scan USING btree (seq_platform_id);

CREATE INDEX seq_scan_seq_type_idx ON public.seq_scan USING btree (seq_type_id);

CREATE INDEX seq_track_antibody_target_idx ON public.seq_track USING btree (antibody_target_id);

CREATE INDEX seq_track_library_preparation_kit_idx ON public.seq_track USING btree (library_preparation_kit_id);

CREATE INDEX seq_track_normalized_library_name_idx ON public.seq_track USING btree (normalized_library_name);

CREATE INDEX seq_track_run_idx ON public.seq_track USING btree (run_id);

CREATE INDEX seq_track_sample_idx ON public.seq_track USING btree (sample_id);

CREATE INDEX seq_track_seq_type_idx ON public.seq_track USING btree (seq_type_id);

CREATE INDEX snv_job_result_snv_calling_instance_idx ON public.snv_job_result USING btree (snv_calling_instance_id);

CREATE INDEX software_tool_identifier_software_tool_idx ON public.software_tool_identifier USING btree (software_tool_id);

CREATE UNIQUE INDEX sophia_instance_idx_roddy_execution_directory_names_idx ON public.sophia_instance_roddy_execution_directory_names USING btree (sophia_instance_id, roddy_execution_directory_names_idx);

CREATE UNIQUE INDEX sophia_qc_sophia_instance_id_idx ON public.sophia_qc USING btree (sophia_instance_id);

CREATE INDEX state_idx ON public.processing_step_update USING btree (state);

CREATE UNIQUE INDEX unique_alias ON public.reference_genome_entry USING btree (reference_genome_id, alias);

CREATE UNIQUE INDEX unique_dir_name ON public.seq_type USING btree (library_layout, dir_name);

CREATE UNIQUE INDEX unique_instance_name ON public.bam_file_pair_analysis USING btree (sample_pair_id, instance_name);

CREATE UNIQUE INDEX unique_name ON public.reference_genome_entry USING btree (reference_genome_id, name);

CREATE UNIQUE INDEX unique_reference_genome_id ON public.bed_file USING btree (library_preparation_kit_id, reference_genome_id);

CREATE UNIQUE INDEX unique_sample_type_id ON public.sample_type_per_project USING btree (project_id, sample_type_id);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT abstract_bam_file_base_bam_file_id_fkey FOREIGN KEY (base_bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT abstract_bam_file_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.config_per_project(id);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT abstract_bam_file_work_package_id_fkey FOREIGN KEY (work_package_id) REFERENCES public.abstract_merging_work_package(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT abstract_merging_work_package_antibody_target_id_fkey FOREIGN KEY (antibody_target_id) REFERENCES public.antibody_target(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT abstract_merging_work_package_reference_genome_id_fkey FOREIGN KEY (reference_genome_id) REFERENCES public.reference_genome(id);

ALTER TABLE ONLY public.abstract_quality_assessment
    ADD CONSTRAINT abstract_quality_assessment_seq_track_id_fkey FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.aceseq_qc
    ADD CONSTRAINT "aceseq_instance_FK" FOREIGN KEY (aceseq_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.aceseq_instance_roddy_execution_directory_names
    ADD CONSTRAINT aceseq_instance_roddy_execution_directo_aceseq_instance_id_fkey FOREIGN KEY (aceseq_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.alignment_pass
    ADD CONSTRAINT alignment_pass_work_package_id_fkey FOREIGN KEY (work_package_id) REFERENCES public.abstract_merging_work_package(id);

ALTER TABLE ONLY public.cluster_job
    ADD CONSTRAINT cluster_job_individual_id_fkey FOREIGN KEY (individual_id) REFERENCES public.individual(id);

ALTER TABLE ONLY public.cluster_job
    ADD CONSTRAINT cluster_job_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id);

ALTER TABLE ONLY public.bam_file_pair_analysis
    ADD CONSTRAINT "config_FK" FOREIGN KEY (config_id) REFERENCES public.config_per_project(id);

ALTER TABLE ONLY public.config_per_project
    ADD CONSTRAINT config_per_project_workflow_id_fkey FOREIGN KEY (pipeline_id) REFERENCES public.pipeline(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT data_file_comment_fk FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.externally_processed_merged_bam_file_further_files
    ADD CONSTRAINT externally_processed_merged_b_externally_processed_merged__fkey FOREIGN KEY (externally_processed_merged_bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.file_system_changes
    ADD CONSTRAINT file_system_changes_swap_info_id_fkey FOREIGN KEY (swap_info_id) REFERENCES public.swap_info(id);

ALTER TABLE ONLY public.merging_set_assignment
    ADD CONSTRAINT fk12b3e15448d3f8c4 FOREIGN KEY (merging_set_id) REFERENCES public.merging_set(id);

ALTER TABLE ONLY public.merging_set_assignment
    ADD CONSTRAINT fk12b3e154abebb43e FOREIGN KEY (bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT fk143bf46a4a12766c FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT fk143bf46aa4e7b28c FOREIGN KEY (role_id) REFERENCES public.role(id);

ALTER TABLE ONLY public.job_definition_parameter
    ADD CONSTRAINT fk174bbeff1e4c5c6c FOREIGN KEY (parameter_id) REFERENCES public.parameter(id);

ALTER TABLE ONLY public.job_definition_parameter
    ADD CONSTRAINT fk174bbeff8947b537 FOREIGN KEY (job_definition_constant_parameters_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.run
    ADD CONSTRAINT fk1ba8beb6d4f73 FOREIGN KEY (seq_platform_id) REFERENCES public.seq_platform(id);

ALTER TABLE ONLY public.run
    ADD CONSTRAINT fk1ba8becebfe73 FOREIGN KEY (seq_center_id) REFERENCES public.seq_center(id);

ALTER TABLE ONLY public.consistency_status
    ADD CONSTRAINT fk1f8e63f95abdecb5 FOREIGN KEY (data_file_id) REFERENCES public.data_file(id);

ALTER TABLE ONLY public.consistency_status
    ADD CONSTRAINT fk1f8e63f9cccc6b66 FOREIGN KEY (consistency_check_id) REFERENCES public.consistency_check(id);

ALTER TABLE ONLY public.acl_object_identity
    ADD CONSTRAINT fk2a2bb00970422cc5 FOREIGN KEY (object_id_class) REFERENCES public.acl_class(id);

ALTER TABLE ONLY public.acl_object_identity
    ADD CONSTRAINT fk2a2bb00990ec1949 FOREIGN KEY (owner_sid) REFERENCES public.acl_sid(id);

ALTER TABLE ONLY public.acl_object_identity
    ADD CONSTRAINT fk2a2bb009a50290b8 FOREIGN KEY (parent_object) REFERENCES public.acl_object_identity(id);

ALTER TABLE ONLY public.processing_step_update
    ADD CONSTRAINT fk3bf8ba70869fd2f9 FOREIGN KEY (previous_id) REFERENCES public.processing_step_update(id);

ALTER TABLE ONLY public.processing_step_update
    ADD CONSTRAINT fk3bf8ba70b6502eaf FOREIGN KEY (processing_step_id) REFERENCES public.processing_step(id);

ALTER TABLE ONLY public.processing_step_update
    ADD CONSTRAINT fk3bf8ba70d848db39 FOREIGN KEY (error_id) REFERENCES public.processing_error(id);

ALTER TABLE ONLY public.alignment_params
    ADD CONSTRAINT fk3dbeff6254649dfd FOREIGN KEY (pipeline_id) REFERENCES public.software_tool(id);

ALTER TABLE ONLY public.parameter_type
    ADD CONSTRAINT fk438d7610ce53a51d FOREIGN KEY (job_definition_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.abstract_quality_assessment
    ADD CONSTRAINT fk4525553f40bbb6dc FOREIGN KEY (quality_assessment_merged_pass_id) REFERENCES public.quality_assessment_merged_pass(id);

ALTER TABLE ONLY public.abstract_quality_assessment
    ADD CONSTRAINT fk4525553f7c88f89 FOREIGN KEY (quality_assessment_pass_id) REFERENCES public.quality_assessment_pass(id);

ALTER TABLE ONLY public.seq_scan
    ADD CONSTRAINT fk4f40a65d5eeb3a93 FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.seq_scan
    ADD CONSTRAINT fk4f40a65d86bca440 FOREIGN KEY (sample_id) REFERENCES public.sample(id);

ALTER TABLE ONLY public.seq_scan
    ADD CONSTRAINT fk4f40a65deb6d4f73 FOREIGN KEY (seq_platform_id) REFERENCES public.seq_platform(id);

ALTER TABLE ONLY public.sample_identifier
    ADD CONSTRAINT fk50cc3e5e86bca440 FOREIGN KEY (sample_id) REFERENCES public.sample(id);

ALTER TABLE ONLY public.acl_entry
    ADD CONSTRAINT fk5302d47d8fdb88d5 FOREIGN KEY (sid) REFERENCES public.acl_sid(id);

ALTER TABLE ONLY public.acl_entry
    ADD CONSTRAINT fk5302d47db0d9dc4d FOREIGN KEY (acl_object_identity) REFERENCES public.acl_object_identity(id);

ALTER TABLE ONLY public.bed_file
    ADD CONSTRAINT fk5317da7aaa9ddd9a FOREIGN KEY (library_preparation_kit_id) REFERENCES public.library_preparation_kit(id);

ALTER TABLE ONLY public.bed_file
    ADD CONSTRAINT fk5317da7ae9290d8b FOREIGN KEY (reference_genome_id) REFERENCES public.reference_genome(id);

ALTER TABLE ONLY public.snv_job_result
    ADD CONSTRAINT fk5db4d82313b2e20a FOREIGN KEY (external_script_id) REFERENCES public.external_script(id);

ALTER TABLE ONLY public.snv_job_result
    ADD CONSTRAINT fk5db4d8234e725978 FOREIGN KEY (chromosome_join_external_script_id) REFERENCES public.external_script(id);

ALTER TABLE ONLY public.job_decision
    ADD CONSTRAINT fk69f2aa1e4c8832f8 FOREIGN KEY (job_definition_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.processing_step
    ADD CONSTRAINT fk6ab804782133feb6 FOREIGN KEY (original_id) REFERENCES public.processing_step(id);

ALTER TABLE ONLY public.processing_step
    ADD CONSTRAINT fk6ab804782816d254 FOREIGN KEY (next_id) REFERENCES public.processing_step(id);

ALTER TABLE ONLY public.processing_step
    ADD CONSTRAINT fk6ab8047867afcb2c FOREIGN KEY (process_id) REFERENCES public.process(id);

ALTER TABLE ONLY public.processing_step
    ADD CONSTRAINT fk6ab804786a1d9abf FOREIGN KEY (decision_id) REFERENCES public.job_decision(id);

ALTER TABLE ONLY public.processing_step
    ADD CONSTRAINT fk6ab804787e0a73d0 FOREIGN KEY (previous_id) REFERENCES public.processing_step(id);

ALTER TABLE ONLY public.processing_step
    ADD CONSTRAINT fk6ab80478ce53a51d FOREIGN KEY (job_definition_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.parameter
    ADD CONSTRAINT fk747eb3a97a189915 FOREIGN KEY (type_id) REFERENCES public.parameter_type(id);

ALTER TABLE ONLY public.merging_pass
    ADD CONSTRAINT fk80961b1b48d3f8c4 FOREIGN KEY (merging_set_id) REFERENCES public.merging_set(id);

ALTER TABLE ONLY public.change_log
    ADD CONSTRAINT fk80f28e35c87ce5ee FOREIGN KEY (referenced_class_id) REFERENCES public.referenced_class(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT fk85e80e225eeb3a93 FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT fk85e80e2286bca440 FOREIGN KEY (sample_id) REFERENCES public.sample(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT fk85e80e22fb20cf05 FOREIGN KEY (bam_file_in_project_folder_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.software_tool_identifier
    ADD CONSTRAINT fk8a71b3184d664af FOREIGN KEY (software_tool_id) REFERENCES public.software_tool(id);

ALTER TABLE ONLY public.alignment_log
    ADD CONSTRAINT fk8b52cae8c38ea45b FOREIGN KEY (alignment_params_id) REFERENCES public.alignment_params(id);

ALTER TABLE ONLY public.alignment_log
    ADD CONSTRAINT fk8b52cae8f27d81e1 FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.parameter_mapping
    ADD CONSTRAINT fk8fc4ba3861880f75 FOREIGN KEY (job_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.parameter_mapping
    ADD CONSTRAINT fk8fc4ba388984faa5 FOREIGN KEY (from_id) REFERENCES public.parameter_type(id);

ALTER TABLE ONLY public.parameter_mapping
    ADD CONSTRAINT fk8fc4ba38b3884634 FOREIGN KEY (to_id) REFERENCES public.parameter_type(id);

ALTER TABLE ONLY public.fastqc_processed_file
    ADD CONSTRAINT fk9454e8be5abdecb5 FOREIGN KEY (data_file_id) REFERENCES public.data_file(id);

ALTER TABLE ONLY public.processing_option
    ADD CONSTRAINT fk95c3cb2151483d14 FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT fk98e90d6b27f589c4 FOREIGN KEY (pipeline_version_id) REFERENCES public.software_tool(id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT fk98e90d6b5eeb3a93 FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT fk98e90d6b86bca440 FOREIGN KEY (sample_id) REFERENCES public.sample(id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT fk98e90d6baa9ddd9a FOREIGN KEY (library_preparation_kit_id) REFERENCES public.library_preparation_kit(id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT fk98e90d6bb6cbbcd4 FOREIGN KEY (run_id) REFERENCES public.run(id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT fk98e90d6bdc2c3789 FOREIGN KEY (antibody_target_id) REFERENCES public.antibody_target(id);

ALTER TABLE ONLY public.process_parameter
    ADD CONSTRAINT fk9928cdd967afcb2c FOREIGN KEY (process_id) REFERENCES public.process(id);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT fk9ae4d28a2c1e7b10 FOREIGN KEY (merging_pass_id) REFERENCES public.merging_pass(id);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT fk9ae4d28a78a3e52c FOREIGN KEY (alignment_pass_id) REFERENCES public.alignment_pass(id);

ALTER TABLE ONLY public.seq_platform_seq_platform_groups
    ADD CONSTRAINT fk_90hikoqdwqwbgnj973kthxn69 FOREIGN KEY (seq_platform_group_id) REFERENCES public.seq_platform_group(id);

ALTER TABLE ONLY public.abstract_bam_file
    ADD CONSTRAINT fk_abstract_bam_file__comment_id FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.bam_file_pair_analysis
    ADD CONSTRAINT fk_bam_file_pair_analysis__comment_id FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.config_per_project
    ADD CONSTRAINT fk_config_per_project__individual_id FOREIGN KEY (individual_id) REFERENCES public.individual(id);

ALTER TABLE ONLY public.stat_size_file_name
    ADD CONSTRAINT fk_kv0mt33uml6focn0ds5iyav3d FOREIGN KEY (reference_genome_id) REFERENCES public.reference_genome(id);

ALTER TABLE ONLY public.merging_criteria
    ADD CONSTRAINT fk_merging_criteria__project_id FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.merging_criteria
    ADD CONSTRAINT fk_merging_criteria__seq_type_id FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.seq_platform_group
    ADD CONSTRAINT fk_oerlov6l1h0pwc38mdqpdafc2 FOREIGN KEY (merging_criteria_id) REFERENCES public.merging_criteria(id);

ALTER TABLE ONLY public.indel_sample_swap_detection
    ADD CONSTRAINT fk_osgyi47q5xjh1kkmvot8tiv4u FOREIGN KEY (indel_calling_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.run_segment
    ADD CONSTRAINT fk_otrs_ticket_id FOREIGN KEY (otrs_ticket_id) REFERENCES public.otrs_ticket(id);

ALTER TABLE ONLY public.cluster_job_cluster_job
    ADD CONSTRAINT fk_qgvpm8hs17r7wy71ogr1497v0 FOREIGN KEY (cluster_job_dependencies_id) REFERENCES public.cluster_job(id);

ALTER TABLE ONLY public.seq_platform_seq_platform_groups
    ADD CONSTRAINT fk_qyb85j8pdw41s1fut28gswos2 FOREIGN KEY (seq_platform_id) REFERENCES public.seq_platform(id);

ALTER TABLE ONLY public.cluster_job_cluster_job
    ADD CONSTRAINT fk_st7o5bylyi9l9ebxhmqas8jdd FOREIGN KEY (cluster_job_id) REFERENCES public.cluster_job(id);

ALTER TABLE ONLY public.picard_mark_duplicates_metrics
    ADD CONSTRAINT fka1b0bd48131bef43 FOREIGN KEY (abstract_bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.decision_mapping
    ADD CONSTRAINT fka364536b6272061f FOREIGN KEY (definition_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.decision_mapping
    ADD CONSTRAINT fka364536b6a1d9abf FOREIGN KEY (decision_id) REFERENCES public.job_decision(id);

ALTER TABLE ONLY public.reference_genome_project_seq_type
    ADD CONSTRAINT fka5af86d451483d14 FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.reference_genome_project_seq_type
    ADD CONSTRAINT fka5af86d45eeb3a93 FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.reference_genome_project_seq_type
    ADD CONSTRAINT fka5af86d4c4a27e75 FOREIGN KEY (sample_type_id) REFERENCES public.sample_type(id);

ALTER TABLE ONLY public.reference_genome_project_seq_type
    ADD CONSTRAINT fka5af86d4e9290d8b FOREIGN KEY (reference_genome_id) REFERENCES public.reference_genome(id);

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT fkb63dd9d4a4e7b28c FOREIGN KEY (role_id) REFERENCES public.role(id);

ALTER TABLE ONLY public.job_execution_plan
    ADD CONSTRAINT fkb8aba8b2128f4660 FOREIGN KEY (start_job_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.job_execution_plan
    ADD CONSTRAINT fkb8aba8b22311a7a5 FOREIGN KEY (previous_plan_id) REFERENCES public.job_execution_plan(id);

ALTER TABLE ONLY public.job_execution_plan
    ADD CONSTRAINT fkb8aba8b224d733a9 FOREIGN KEY (process_parameter_id) REFERENCES public.process_parameter(id);

ALTER TABLE ONLY public.job_execution_plan
    ADD CONSTRAINT fkb8aba8b29b07d3e4 FOREIGN KEY (first_job_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.bam_file_pair_analysis
    ADD CONSTRAINT fkb9beb3f423e26796 FOREIGN KEY (sample_pair_id) REFERENCES public.sample_pair(id);

ALTER TABLE ONLY public.bam_file_pair_analysis
    ADD CONSTRAINT fkb9beb3f46a43c390 FOREIGN KEY (sample_type1bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.bam_file_pair_analysis
    ADD CONSTRAINT fkb9beb3f471f568ef FOREIGN KEY (sample_type2bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.cluster_job
    ADD CONSTRAINT fkbbfa4ff85eeb3a93 FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.cluster_job
    ADD CONSTRAINT fkbbfa4ff8b6502eaf FOREIGN KEY (processing_step_id) REFERENCES public.processing_step(id);

ALTER TABLE ONLY public.meta_data_file
    ADD CONSTRAINT fkbdd9073711d99c9 FOREIGN KEY (run_segment_id) REFERENCES public.run_segment(id);

ALTER TABLE ONLY public.merging_log
    ADD CONSTRAINT fkc67e2fa3aa4fb3 FOREIGN KEY (seq_scan_id) REFERENCES public.seq_scan(id);

ALTER TABLE ONLY public.merging_log
    ADD CONSTRAINT fkc67e2fac38ea45b FOREIGN KEY (alignment_params_id) REFERENCES public.alignment_params(id);

ALTER TABLE ONLY public.merging_set
    ADD CONSTRAINT fkc67fc18cf07d60d FOREIGN KEY (merging_work_package_id) REFERENCES public.abstract_merging_work_package(id);

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT fkc9c775aa783af6e0 FOREIGN KEY (individual_id) REFERENCES public.individual(id);

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT fkc9c775aac4a27e75 FOREIGN KEY (sample_type_id) REFERENCES public.sample_type(id);

ALTER TABLE ONLY public.processed_sai_file
    ADD CONSTRAINT fkce05e1315abdecb5 FOREIGN KEY (data_file_id) REFERENCES public.data_file(id);

ALTER TABLE ONLY public.processed_sai_file
    ADD CONSTRAINT fkce05e13178a3e52c FOREIGN KEY (alignment_pass_id) REFERENCES public.alignment_pass(id);

ALTER TABLE ONLY public.reference_genome_entry
    ADD CONSTRAINT fkcfc04f9ee9290d8b FOREIGN KEY (reference_genome_id) REFERENCES public.reference_genome(id);

ALTER TABLE ONLY public.merging_assignment
    ADD CONSTRAINT fkd6264e573aa4fb3 FOREIGN KEY (seq_scan_id) REFERENCES public.seq_scan(id);

ALTER TABLE ONLY public.merging_assignment
    ADD CONSTRAINT fkd6264e57f27d81e1 FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.shutdown_information
    ADD CONSTRAINT fkdc5608c329604b3a FOREIGN KEY (canceled_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.shutdown_information
    ADD CONSTRAINT fkdc5608c3a982820c FOREIGN KEY (initiated_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.processing_step_parameter
    ADD CONSTRAINT fkde767ca21e4c5c6c FOREIGN KEY (parameter_id) REFERENCES public.parameter(id);

ALTER TABLE ONLY public.processing_step_parameter
    ADD CONSTRAINT fkde767ca2a5a4aa7f FOREIGN KEY (processing_step_output_id) REFERENCES public.processing_step(id);

ALTER TABLE ONLY public.processing_step_parameter
    ADD CONSTRAINT fkde767ca2a7ebf604 FOREIGN KEY (processing_step_input_id) REFERENCES public.processing_step(id);

ALTER TABLE ONLY public.library_preparation_kit_synonym
    ADD CONSTRAINT fkde8fc9c3aa9ddd9a FOREIGN KEY (library_preparation_kit_id) REFERENCES public.library_preparation_kit(id);

ALTER TABLE ONLY public.alignment_pass
    ADD CONSTRAINT fkdf0830edf27d81e1 FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.merged_alignment_data_file
    ADD CONSTRAINT fke234f80276ee455 FOREIGN KEY (merging_log_id) REFERENCES public.merging_log(id);

ALTER TABLE ONLY public.quality_assessment_merged_pass
    ADD CONSTRAINT fke70b46871a932a30 FOREIGN KEY (abstract_merged_bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.job_definition
    ADD CONSTRAINT fke70e271514744c7f FOREIGN KEY (next_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.job_definition
    ADD CONSTRAINT fke70e27156a67edfb FOREIGN KEY (previous_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.job_definition
    ADD CONSTRAINT fke70e2715c81fe476 FOREIGN KEY (validator_for_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.job_definition
    ADD CONSTRAINT fke70e2715dec12ced FOREIGN KEY (plan_id) REFERENCES public.job_execution_plan(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT fkea50f8f111d99c9 FOREIGN KEY (run_segment_id) REFERENCES public.run_segment(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT fkea50f8f11c055679 FOREIGN KEY (alignment_log_id) REFERENCES public.alignment_log(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT fkea50f8f129b68c99 FOREIGN KEY (file_type_id) REFERENCES public.file_type(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT fkea50f8f151483d14 FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT fkea50f8f176ee455 FOREIGN KEY (merging_log_id) REFERENCES public.merging_log(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT fkea50f8f1b6cbbcd4 FOREIGN KEY (run_id) REFERENCES public.run(id);

ALTER TABLE ONLY public.data_file
    ADD CONSTRAINT fkea50f8f1f27d81e1 FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.process
    ADD CONSTRAINT fked8d1e6f4c5736c4 FOREIGN KEY (job_execution_plan_id) REFERENCES public.job_execution_plan(id);

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fked904b198281ea6d FOREIGN KEY (project_group_id) REFERENCES public.project_group(id);

ALTER TABLE ONLY public.quality_assessment_pass
    ADD CONSTRAINT fkef60082e2dead8cd FOREIGN KEY (processed_bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.meta_data_entry
    ADD CONSTRAINT fkfd3c2e175abdecb5 FOREIGN KEY (data_file_id) REFERENCES public.data_file(id);

ALTER TABLE ONLY public.meta_data_entry
    ADD CONSTRAINT fkfd3c2e17a1c1c485 FOREIGN KEY (key_id) REFERENCES public.meta_data_key(id);

ALTER TABLE ONLY public.individual
    ADD CONSTRAINT fkfd3da29951483d14 FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.gene_model
    ADD CONSTRAINT gene_model__reference_genome_fkey FOREIGN KEY (reference_genome_id) REFERENCES public.reference_genome(id);

ALTER TABLE ONLY public.ilse_submission
    ADD CONSTRAINT ilse_submission_comment_id_fkey FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.import_process_externally_processed_merged_bam_file
    ADD CONSTRAINT import_process_externally_pro_externally_processed_merged__fkey FOREIGN KEY (externally_processed_merged_bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.import_process_externally_processed_merged_bam_file
    ADD CONSTRAINT import_process_externally_pro_import_process_externally_pr_fkey FOREIGN KEY (import_process_externally_processed_merged_bam_files_id) REFERENCES public.import_process(id);

ALTER TABLE ONLY public.indel_calling_instance_roddy_execution_directory_names
    ADD CONSTRAINT indel_calling_instance_roddy_exe_indel_calling_instance_id_fkey FOREIGN KEY (indel_calling_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.indel_quality_control
    ADD CONSTRAINT indel_quality_control_indel_calling_instance_id_fkey FOREIGN KEY (indel_calling_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.snv_job_result
    ADD CONSTRAINT "input_result_FK" FOREIGN KEY (input_result_id) REFERENCES public.snv_job_result(id);

ALTER TABLE ONLY public.job_error_definition_job_definition
    ADD CONSTRAINT job_error_definition_job_defi_job_error_definition_job_def_fkey FOREIGN KEY (job_error_definition_job_definitions_id) REFERENCES public.job_error_definition(id);

ALTER TABLE ONLY public.job_error_definition_job_definition
    ADD CONSTRAINT job_error_definition_job_definition_job_definition_id_fkey FOREIGN KEY (job_definition_id) REFERENCES public.job_definition(id);

ALTER TABLE ONLY public.job_error_definition_job_error_definition
    ADD CONSTRAINT job_error_definition_job_erro_job_error_definition_check_f_fkey FOREIGN KEY (job_error_definition_check_further_job_errors_id) REFERENCES public.job_error_definition(id);

ALTER TABLE ONLY public.job_error_definition_job_error_definition
    ADD CONSTRAINT job_error_definition_job_error_def_job_error_definition_id_fkey FOREIGN KEY (job_error_definition_id) REFERENCES public.job_error_definition(id);

ALTER TABLE ONLY public.merging_work_package_alignment_property
    ADD CONSTRAINT merging_work_package_alignment_pro_merging_work_package_id_fkey FOREIGN KEY (merging_work_package_id) REFERENCES public.abstract_merging_work_package(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT merging_work_package_library_preparation_kit FOREIGN KEY (library_preparation_kit_id) REFERENCES public.library_preparation_kit(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT merging_work_package_seq_platform_group_id_fkey FOREIGN KEY (seq_platform_group_id) REFERENCES public.seq_platform_group(id);

ALTER TABLE ONLY public.merging_work_package_seq_track
    ADD CONSTRAINT merging_work_package_seq_trac_merging_work_package_seq_tra_fkey FOREIGN KEY (merging_work_package_seq_tracks_id) REFERENCES public.abstract_merging_work_package(id);

ALTER TABLE ONLY public.merging_work_package_seq_track
    ADD CONSTRAINT merging_work_package_seq_track_seq_track_id_fkey FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.abstract_merging_work_package
    ADD CONSTRAINT merging_work_package_workflow_id_fkey FOREIGN KEY (pipeline_id) REFERENCES public.pipeline(id);

ALTER TABLE ONLY public.otrs_ticket
    ADD CONSTRAINT otrs_ticket_comment_id_fkey FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.config_per_project
    ADD CONSTRAINT "previous_config_FK" FOREIGN KEY (previous_config_id) REFERENCES public.config_per_project(id);

ALTER TABLE ONLY public.process
    ADD CONSTRAINT process_comment_fk FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.individual
    ADD CONSTRAINT process_comment_fk FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.process
    ADD CONSTRAINT process_restarted_id_fkey FOREIGN KEY (restarted_id) REFERENCES public.process(id);

ALTER TABLE ONLY public.config_per_project
    ADD CONSTRAINT "project_FK" FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.processing_thresholds
    ADD CONSTRAINT "project_FK" FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.sample_type_per_project
    ADD CONSTRAINT "project_FK" FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_comment_fk FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.project_contact_person
    ADD CONSTRAINT project_contact_person_contact_person_role_id_fkey FOREIGN KEY (contact_person_role_id) REFERENCES public.contact_person_role(id);

ALTER TABLE ONLY public.project_contact_person
    ADD CONSTRAINT project_contact_persons_contact_person_id_fkey FOREIGN KEY (contact_person_id) REFERENCES public.contact_person(id);

ALTER TABLE ONLY public.project_contact_person
    ADD CONSTRAINT project_contact_persons_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.project_project_categories
    ADD CONSTRAINT project_project_categories__project_category_id_fkey FOREIGN KEY (project_category_id) REFERENCES public.project_category(id);

ALTER TABLE ONLY public.project_project_categories
    ADD CONSTRAINT project_project_categories__project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_realm_id_fkey FOREIGN KEY (realm_id) REFERENCES public.realm(id);

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_tumor_entity_fk FOREIGN KEY (tumor_entity_id) REFERENCES public.tumor_entity(id);

ALTER TABLE ONLY public.qc_threshold
    ADD CONSTRAINT "qc_threshold_project_FK" FOREIGN KEY (project_id) REFERENCES public.project(id);

ALTER TABLE ONLY public.qc_threshold
    ADD CONSTRAINT "qc_threshold_seqType_FK" FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.reference_genome_index
    ADD CONSTRAINT reference_genome_index__reference_genome_id_fkey FOREIGN KEY (reference_genome_id) REFERENCES public.reference_genome(id);

ALTER TABLE ONLY public.reference_genome_index
    ADD CONSTRAINT reference_genome_index__tool_name_id_fkey FOREIGN KEY (tool_name_id) REFERENCES public.tool_name(id);

ALTER TABLE ONLY public.reference_genome_project_seq_type_alignment_property
    ADD CONSTRAINT reference_genome_project_seq__reference_genome_project_seq_fkey FOREIGN KEY (reference_genome_project_seq_type_id) REFERENCES public.reference_genome_project_seq_type(id);

ALTER TABLE ONLY public.roddy_bam_file_roddy_execution_directory_names
    ADD CONSTRAINT roddy_bam_file_roddy_execution_directory_names_string_fkey FOREIGN KEY (roddy_bam_file_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.roddy_bam_file_seq_track
    ADD CONSTRAINT roddy_bam_file_seq_track_roddy_bam_file_seq_tracks_id_fkey FOREIGN KEY (roddy_bam_file_seq_tracks_id) REFERENCES public.abstract_bam_file(id);

ALTER TABLE ONLY public.roddy_bam_file_seq_track
    ADD CONSTRAINT roddy_bam_file_seq_track_seq_track_id_fkey FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.roddy_snv_calling_instance_roddy_execution_directory_names
    ADD CONSTRAINT roddy_snv_calling_instance_ro_roddy_snv_calling_instance_i_fkey FOREIGN KEY (roddy_snv_calling_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.sample_pair
    ADD CONSTRAINT sample_pair_merging_work_package1_id_fkey FOREIGN KEY (merging_work_package1_id) REFERENCES public.abstract_merging_work_package(id);

ALTER TABLE ONLY public.sample_pair
    ADD CONSTRAINT sample_pair_merging_work_package2_id_fkey FOREIGN KEY (merging_work_package2_id) REFERENCES public.abstract_merging_work_package(id);

ALTER TABLE ONLY public.processing_thresholds
    ADD CONSTRAINT "sample_type_FK" FOREIGN KEY (sample_type_id) REFERENCES public.sample_type(id);

ALTER TABLE ONLY public.sample_type_per_project
    ADD CONSTRAINT "sample_type_FK" FOREIGN KEY (sample_type_id) REFERENCES public.sample_type(id);

ALTER TABLE ONLY public.seq_platform_group_comment
    ADD CONSTRAINT seq_platform_group_comments_comment_id_fkey FOREIGN KEY (comment_id) REFERENCES public.comment(id);

ALTER TABLE ONLY public.seq_platform_group_comment
    ADD CONSTRAINT seq_platform_group_comments_fkey FOREIGN KEY (seq_platform_group_comments_id) REFERENCES public.seq_platform_group(id);

ALTER TABLE ONLY public.seq_platform_model_label_alias
    ADD CONSTRAINT "seq_platform_model_label_FK" FOREIGN KEY (seq_platform_model_label_id) REFERENCES public.seq_platform_model_label(id);

ALTER TABLE ONLY public.seq_platform
    ADD CONSTRAINT "seq_platform_model_label_FK" FOREIGN KEY (seq_platform_model_label_id) REFERENCES public.seq_platform_model_label(id);

ALTER TABLE ONLY public.seq_track
    ADD CONSTRAINT seq_track_ilse_submission_id_fkey FOREIGN KEY (ilse_submission_id) REFERENCES public.ilse_submission(id);

ALTER TABLE ONLY public.seq_track_log_message
    ADD CONSTRAINT seq_track_log_message_log_message_id_fkey FOREIGN KEY (log_message_id) REFERENCES public.log_message(id);

ALTER TABLE ONLY public.seq_track_log_message
    ADD CONSTRAINT seq_track_log_message_seq_track_log_messages_id_fkey FOREIGN KEY (seq_track_log_messages_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.config_per_project
    ADD CONSTRAINT "seq_type_FK" FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.processing_thresholds
    ADD CONSTRAINT "seq_type_FK" FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.seq_type_alias
    ADD CONSTRAINT "seq_type_FK" FOREIGN KEY (seq_type_id) REFERENCES public.seq_type(id);

ALTER TABLE ONLY public.seq_platform
    ADD CONSTRAINT "sequencing_kit_label_FK" FOREIGN KEY (sequencing_kit_label_id) REFERENCES public.sequencing_kit_label(id);

ALTER TABLE ONLY public.sequencing_kit_label_alias
    ADD CONSTRAINT "sequencing_kit_label_FK" FOREIGN KEY (sequencing_kit_label_id) REFERENCES public.sequencing_kit_label(id);

ALTER TABLE ONLY public.snv_job_result
    ADD CONSTRAINT "snv_calling_instance_FK" FOREIGN KEY (snv_calling_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.sophia_qc
    ADD CONSTRAINT "sophia_instance_FK" FOREIGN KEY (sophia_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.sophia_instance_roddy_execution_directory_names
    ADD CONSTRAINT sophia_instance_roddy_execution_directo_sophia_instance_id_fkey FOREIGN KEY (sophia_instance_id) REFERENCES public.bam_file_pair_analysis(id);

ALTER TABLE ONLY public.swap_info_seq_track
    ADD CONSTRAINT swap_info_seq_track_seq_track_id_fkey FOREIGN KEY (seq_track_id) REFERENCES public.seq_track(id);

ALTER TABLE ONLY public.swap_info_seq_track
    ADD CONSTRAINT swap_info_seq_track_swap_info_seq_tracks_id_fkey FOREIGN KEY (swap_info_seq_tracks_id) REFERENCES public.swap_info(id);

ALTER TABLE ONLY public.swap_info
    ADD CONSTRAINT swap_info_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);
