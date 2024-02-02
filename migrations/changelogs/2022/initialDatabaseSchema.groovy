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
    changeSet(author: "otp", id: "intial-database-schema-1") {
        sql("""
CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.acl_class
(
    id    bigint       NOT NULL
        PRIMARY KEY,
    class varchar(255) NOT NULL
        CONSTRAINT uc_acl_classclass_col
            UNIQUE
);

CREATE TABLE public.acl_sid
(
    id        bigint       NOT NULL
        PRIMARY KEY,
    principal boolean      NOT NULL,
    sid       varchar(255) NOT NULL,
    UNIQUE (sid, principal),
    CONSTRAINT unique_principal
        UNIQUE (sid, principal)
);

CREATE TABLE public.acl_object_identity
(
    id                 bigint  NOT NULL
        PRIMARY KEY,
    object_id_class    bigint  NOT NULL
        CONSTRAINT fk2a2bb00970422cc5
            REFERENCES public.acl_class,
    entries_inheriting boolean NOT NULL,
    object_id_identity bigint  NOT NULL,
    owner_sid          bigint
        CONSTRAINT fk2a2bb00990ec1949
            REFERENCES public.acl_sid,
    parent_object      bigint
        CONSTRAINT fk2a2bb009a50290b8
            REFERENCES public.acl_object_identity,
    UNIQUE (object_id_class, object_id_identity),
    CONSTRAINT unique_object_id_identity
        UNIQUE (object_id_class, object_id_identity)
);

CREATE TABLE public.acl_entry
(
    id                  bigint  NOT NULL
        PRIMARY KEY,
    ace_order           integer NOT NULL,
    acl_object_identity bigint  NOT NULL
        CONSTRAINT fk5302d47db0d9dc4d
            REFERENCES public.acl_object_identity,
    audit_failure       boolean NOT NULL,
    audit_success       boolean NOT NULL,
    granting            boolean NOT NULL,
    mask                integer NOT NULL,
    sid                 bigint  NOT NULL
        CONSTRAINT fk5302d47d8fdb88d5
            REFERENCES public.acl_sid,
    UNIQUE (acl_object_identity, ace_order),
    CONSTRAINT unique_ace_order
        UNIQUE (acl_object_identity, ace_order)
);

CREATE TABLE public.realm
(
    id                             bigint                   NOT NULL
        PRIMARY KEY,
    version                        bigint                   NOT NULL,
    host                           varchar(255)             NOT NULL,
    name                           varchar(255)             NOT NULL
        CONSTRAINT unique_realm_name_constraint
            UNIQUE,
    default_job_submission_options varchar(255)             NOT NULL,
    port                           integer                  NOT NULL,
    timeout                        integer                  NOT NULL,
    job_scheduler                  varchar(255)             NOT NULL,
    date_created                   timestamp WITH TIME ZONE NOT NULL,
    last_updated                   timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.sample_type
(
    id                        bigint                   NOT NULL
        PRIMARY KEY,
    version                   bigint                   NOT NULL,
    name                      varchar(255)             NOT NULL
        UNIQUE,
    specific_reference_genome varchar(255)             NOT NULL,
    date_created              timestamp WITH TIME ZONE NOT NULL,
    last_updated              timestamp WITH TIME ZONE NOT NULL,
    legacy                    boolean                  NOT NULL
);

CREATE INDEX sample_type_name_idx
    ON public.sample_type (name);

CREATE TABLE public.seq_center
(
    id                 bigint                   NOT NULL
        PRIMARY KEY,
    version            bigint                   NOT NULL,
    dir_name           varchar(255)             NOT NULL
        UNIQUE,
    name               varchar(255)             NOT NULL
        UNIQUE,
    auto_importable    boolean                  NOT NULL,
    auto_import_dir    varchar(255)
        CONSTRAINT uc_seq_centerauto_import_dir_col
            UNIQUE,
    copy_metadata_file boolean                  NOT NULL,
    date_created       timestamp WITH TIME ZONE NOT NULL,
    last_updated       timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.seq_platform_model_label
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    legacy       boolean                  NOT NULL
);

CREATE TABLE public.seq_type
(
    id                  bigint                   NOT NULL
        PRIMARY KEY,
    version             bigint                   NOT NULL,
    dir_name            varchar(255)             NOT NULL,
    library_layout      varchar(255)             NOT NULL,
    name                varchar(255)             NOT NULL,
    display_name        varchar(255)             NOT NULL,
    roddy_name          varchar(255),
    single_cell         boolean                  NOT NULL,
    has_antibody_target boolean                  NOT NULL,
    date_created        timestamp WITH TIME ZONE NOT NULL,
    last_updated        timestamp WITH TIME ZONE NOT NULL,
    legacy              boolean                  NOT NULL,
    needs_bed_file      boolean                  NOT NULL,
    CONSTRAINT "UK39c048627a9699768034f800dfe3"
        UNIQUE (single_cell, library_layout, dir_name),
    CONSTRAINT "UKf73fa0246b0db85b4ab1817fa80a"
        UNIQUE (single_cell, library_layout, name),
    CONSTRAINT "UK5a0d148daef7ad278ef33a7aff20"
        UNIQUE (library_layout, dir_name)
);

CREATE INDEX seq_type__has_antibody_target_idx
    ON public.seq_type (has_antibody_target);

CREATE INDEX seq_type__dir_name_idx
    ON public.seq_type (dir_name);

CREATE INDEX seq_type__display_name_idx
    ON public.seq_type (display_name);

CREATE INDEX seq_type__library_layout_idx
    ON public.seq_type (library_layout);

CREATE INDEX seq_type__name_idx
    ON public.seq_type (name);

CREATE INDEX seq_type__single_cell_idx
    ON public.seq_type (single_cell);

CREATE TABLE public.sequencing_kit_label
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    legacy       boolean                  NOT NULL
);

CREATE TABLE public.seq_platform
(
    id                          bigint                   NOT NULL
        PRIMARY KEY,
    version                     bigint                   NOT NULL,
    name                        varchar(255)             NOT NULL,
    sequencing_kit_label_id     bigint
        CONSTRAINT "sequencing_kit_label_FK"
            REFERENCES public.sequencing_kit_label,
    seq_platform_model_label_id bigint
        CONSTRAINT "seq_platform_model_label_FK"
            REFERENCES public.seq_platform_model_label,
    date_created                timestamp WITH TIME ZONE NOT NULL,
    last_updated                timestamp WITH TIME ZONE NOT NULL,
    legacy                      boolean                  NOT NULL
);

CREATE TABLE public.run
(
    id              bigint                   NOT NULL
        PRIMARY KEY,
    version         bigint                   NOT NULL,
    blacklisted     boolean                  NOT NULL,
    date_created    timestamp                NOT NULL,
    date_executed   timestamp,
    name            varchar(255)             NOT NULL
        UNIQUE,
    seq_center_id   bigint                   NOT NULL
        CONSTRAINT fk1ba8becebfe73
            REFERENCES public.seq_center,
    seq_platform_id bigint                   NOT NULL
        CONSTRAINT fk1ba8beb6d4f73
            REFERENCES public.seq_platform,
    last_updated    timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX run_seq_center_idx
    ON public.run (seq_center_id);

CREATE INDEX run_seq_platform_idx
    ON public.run (seq_platform_id);

CREATE INDEX seq_platform_seq_platform_model_label_idx
    ON public.seq_platform (seq_platform_model_label_id);

CREATE INDEX seq_platform_sequencing_kit_label_idx
    ON public.seq_platform (sequencing_kit_label_id);

CREATE INDEX seq_platform_sequencing_name_idx
    ON public.seq_platform (name);

CREATE TABLE public.antibody_target
(
    id           bigint                   NOT NULL
        CONSTRAINT "antibody_targPK"
            PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        CONSTRAINT uc_antibody_targetname_col
            UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    legacy       boolean                  NOT NULL
);

CREATE TABLE public.antibody_target_import_alias
(
    antibody_target_id  bigint       NOT NULL
        CONSTRAINT "antibody_target_import_alias_antibody_target_FK"
            REFERENCES public.antibody_target,
    import_alias_string varchar(255) NOT NULL
        UNIQUE,
    PRIMARY KEY (antibody_target_id, import_alias_string)
);

CREATE TABLE public.comment
(
    id                bigint                   NOT NULL
        PRIMARY KEY,
    version           bigint                   NOT NULL,
    comment           text                     NOT NULL,
    author            varchar(255)             NOT NULL,
    modification_date timestamp WITH TIME ZONE NOT NULL,
    date_created      timestamp WITH TIME ZONE NOT NULL,
    last_updated      timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.file_type
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    signature    varchar(255)             NOT NULL,
    sub_type     varchar(255)             NOT NULL,
    type         varchar(255)             NOT NULL,
    vbp_path     varchar(255)             NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX file_type_type_idx
    ON public.file_type (type);

CREATE TABLE public.ilse_submission
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    ilse_number  integer                  NOT NULL
        CONSTRAINT uc_ilse_submissionilse_number_col
            UNIQUE,
    comment_id   bigint
        REFERENCES public.comment,
    warning      boolean                  NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.import_process
(
    id               bigint                   NOT NULL
        PRIMARY KEY,
    version          bigint                   NOT NULL,
    state            varchar(255)             NOT NULL,
    trigger_analysis boolean                  NOT NULL,
    date_created     timestamp WITH TIME ZONE NOT NULL,
    last_updated     timestamp WITH TIME ZONE NOT NULL,
    link_operation   varchar(255)             NOT NULL
);

CREATE INDEX import_process_state_idx
    ON public.import_process (state);

CREATE TABLE public.job_definition
(
    id               bigint                   NOT NULL
        PRIMARY KEY,
    version          bigint                   NOT NULL,
    bean             varchar(255)             NOT NULL,
    name             varchar(255)             NOT NULL,
    next_id          bigint
        CONSTRAINT fke70e271514744c7f
            REFERENCES public.job_definition,
    plan_id          bigint,
    previous_id      bigint
        CONSTRAINT fke70e27156a67edfb
            REFERENCES public.job_definition,
    class            varchar(255)             NOT NULL,
    validator_for_id bigint
        CONSTRAINT fke70e2715c81fe476
            REFERENCES public.job_definition,
    date_created     timestamp WITH TIME ZONE NOT NULL,
    last_updated     timestamp WITH TIME ZONE NOT NULL,
    UNIQUE (plan_id, name)
);

CREATE TABLE public.job_decision
(
    id                bigint                   NOT NULL
        PRIMARY KEY,
    version           bigint                   NOT NULL,
    description       varchar(255)             NOT NULL,
    job_definition_id bigint                   NOT NULL
        CONSTRAINT fk69f2aa1e4c8832f8
            REFERENCES public.job_definition,
    name              varchar(255)             NOT NULL,
    date_created      timestamp WITH TIME ZONE NOT NULL,
    last_updated      timestamp WITH TIME ZONE NOT NULL,
    UNIQUE (job_definition_id, name)
);

CREATE TABLE public.decision_mapping
(
    id            bigint                   NOT NULL
        PRIMARY KEY,
    version       bigint                   NOT NULL,
    decision_id   bigint                   NOT NULL
        UNIQUE
        CONSTRAINT fka364536b6a1d9abf
            REFERENCES public.job_decision,
    definition_id bigint                   NOT NULL
        CONSTRAINT fka364536b6272061f
            REFERENCES public.job_definition,
    date_created  timestamp WITH TIME ZONE NOT NULL,
    last_updated  timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX job_definition_bean_idx
    ON public.job_definition (bean);

CREATE INDEX job_definition_next_idx
    ON public.job_definition (next_id);

CREATE INDEX job_definition_plan_idx
    ON public.job_definition (plan_id);

CREATE INDEX job_definition_previous_idx
    ON public.job_definition (previous_id);

CREATE TABLE public.job_error_definition
(
    id               bigint                   NOT NULL
        PRIMARY KEY,
    version          bigint                   NOT NULL,
    error_expression text                     NOT NULL,
    type             varchar(255)             NOT NULL,
    action           varchar(255)             NOT NULL,
    date_created     timestamp WITH TIME ZONE NOT NULL,
    last_updated     timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.job_error_definition_job_definition
(
    job_error_definition_job_definitions_id bigint NOT NULL
        CONSTRAINT job_error_definition_job_defi_job_error_definition_job_def_fkey
            REFERENCES public.job_error_definition,
    job_definition_id                       bigint NOT NULL
        REFERENCES public.job_definition,
    PRIMARY KEY (job_error_definition_job_definitions_id, job_definition_id)
);

CREATE TABLE public.job_error_definition_job_error_definition
(
    job_error_definition_id                          bigint NOT NULL
        CONSTRAINT job_error_definition_job_error_def_job_error_definition_id_fkey
            REFERENCES public.job_error_definition,
    job_error_definition_check_further_job_errors_id bigint NOT NULL
        CONSTRAINT job_error_definition_job_erro_job_error_definition_check_f_fkey
            REFERENCES public.job_error_definition,
    PRIMARY KEY (job_error_definition_id, job_error_definition_check_further_job_errors_id)
);

CREATE TABLE public.job_execution_plan
(
    id                   bigint                   NOT NULL
        PRIMARY KEY,
    version              bigint                   NOT NULL,
    enabled              boolean                  NOT NULL,
    first_job_id         bigint
        CONSTRAINT fkb8aba8b29b07d3e4
            REFERENCES public.job_definition,
    name                 varchar(255)             NOT NULL,
    obsoleted            boolean                  NOT NULL,
    plan_version         integer                  NOT NULL,
    previous_plan_id     bigint
        CONSTRAINT fkb8aba8b22311a7a5
            REFERENCES public.job_execution_plan,
    process_parameter_id bigint,
    start_job_id         bigint
        CONSTRAINT fkb8aba8b2128f4660
            REFERENCES public.job_definition,
    date_created         timestamp WITH TIME ZONE NOT NULL,
    last_updated         timestamp WITH TIME ZONE NOT NULL,
    UNIQUE (plan_version, name)
);

ALTER TABLE public.job_definition
    ADD CONSTRAINT fke70e2715dec12ced
        FOREIGN KEY (plan_id) REFERENCES public.job_execution_plan;

CREATE INDEX job_execution_plan_first_job_idx
    ON public.job_execution_plan (first_job_id);

CREATE INDEX job_execution_plan_previous_plan_idx
    ON public.job_execution_plan (previous_plan_id);

CREATE INDEX job_execution_process_parameter_idx
    ON public.job_execution_plan (process_parameter_id);

CREATE INDEX job_execution_start_job_idx
    ON public.job_execution_plan (start_job_id);

CREATE TABLE public.library_preparation_kit
(
    id                                  bigint                   NOT NULL
        CONSTRAINT "exome_enrichmPK"
            PRIMARY KEY,
    version                             bigint                   NOT NULL,
    name                                varchar(255)             NOT NULL
        CONSTRAINT uc_library_preparation_kitname_col
            UNIQUE,
    short_display_name                  varchar(255)             NOT NULL
        UNIQUE,
    adapter_file                        varchar(255),
    reverse_complement_adapter_sequence varchar(255),
    date_created                        timestamp WITH TIME ZONE NOT NULL,
    last_updated                        timestamp WITH TIME ZONE NOT NULL,
    legacy                              boolean                  NOT NULL
);

CREATE TABLE public.library_preparation_kit_import_alias
(
    library_preparation_kit_id bigint       NOT NULL
        CONSTRAINT fkde8fc9c3aa9ddd9a
            REFERENCES public.library_preparation_kit,
    import_alias_string        varchar(255) NOT NULL
        CONSTRAINT library_preparation_kit_import_alias_import_alias_string_unique
            UNIQUE,
    PRIMARY KEY (library_preparation_kit_id, import_alias_string)
);

CREATE INDEX library_preparation_kit_synonym_library_preparation_kit_idx
    ON public.library_preparation_kit_import_alias (library_preparation_kit_id);

CREATE TABLE public.log_message
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    message      text                     NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.meta_data_key
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX name_uniq_1383572139560
    ON public.meta_data_key (name);

CREATE UNIQUE INDEX name_uniq_1415890817960
    ON public.meta_data_key (name);

CREATE INDEX unique_meta_data_key_name_idx
    ON public.meta_data_key (name);

CREATE TABLE public.otrs_ticket
(
    id                         bigint                   NOT NULL
        PRIMARY KEY,
    version                    bigint                   NOT NULL,
    ticket_number              varchar(255)             NOT NULL
        UNIQUE,
    date_created               timestamp WITH TIME ZONE NOT NULL,
    installation_started       timestamp WITH TIME ZONE,
    installation_finished      timestamp WITH TIME ZONE,
    fastqc_started             timestamp WITH TIME ZONE,
    fastqc_finished            timestamp WITH TIME ZONE,
    alignment_started          timestamp WITH TIME ZONE,
    alignment_finished         timestamp WITH TIME ZONE,
    snv_started                timestamp WITH TIME ZONE,
    snv_finished               timestamp WITH TIME ZONE,
    final_notification_sent    boolean                  NOT NULL,
    comment_id                 bigint
        REFERENCES public.comment,
    ticket_created             timestamp WITH TIME ZONE,
    submission_received_notice timestamp WITH TIME ZONE,
    seq_center_comment         text,
    automatic_notification     boolean                  NOT NULL,
    indel_started              timestamp WITH TIME ZONE,
    indel_finished             timestamp WITH TIME ZONE,
    aceseq_started             timestamp WITH TIME ZONE,
    aceseq_finished            timestamp WITH TIME ZONE,
    sophia_started             timestamp WITH TIME ZONE,
    sophia_finished            timestamp WITH TIME ZONE,
    run_yapsa_started          timestamp WITH TIME ZONE,
    run_yapsa_finished         timestamp WITH TIME ZONE,
    last_updated               timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.parameter_type
(
    id                bigint                   NOT NULL
        PRIMARY KEY,
    version           bigint                   NOT NULL,
    class_name        varchar(255),
    description       varchar(255),
    job_definition_id bigint                   NOT NULL
        CONSTRAINT fk438d7610ce53a51d
            REFERENCES public.job_definition,
    name              varchar(255)             NOT NULL,
    parameter_usage   varchar(255)             NOT NULL,
    date_created      timestamp WITH TIME ZONE NOT NULL,
    last_updated      timestamp WITH TIME ZONE NOT NULL,
    UNIQUE (parameter_usage, job_definition_id, name)
);

CREATE TABLE public.parameter
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    type_id      bigint                   NOT NULL
        CONSTRAINT fk747eb3a97a189915
            REFERENCES public.parameter_type,
    value        text                     NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.job_definition_parameter
(
    job_definition_constant_parameters_id bigint NOT NULL
        CONSTRAINT fk174bbeff8947b537
            REFERENCES public.job_definition,
    parameter_id                          bigint
        CONSTRAINT fk174bbeff1e4c5c6c
            REFERENCES public.parameter
);

CREATE INDEX parameter_type_idx
    ON public.parameter (type_id);

CREATE TABLE public.parameter_mapping
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    from_id      bigint                   NOT NULL
        CONSTRAINT fk8fc4ba388984faa5
            REFERENCES public.parameter_type,
    job_id       bigint                   NOT NULL
        CONSTRAINT fk8fc4ba3861880f75
            REFERENCES public.job_definition,
    to_id        bigint                   NOT NULL
        CONSTRAINT fk8fc4ba38b3884634
            REFERENCES public.parameter_type,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    UNIQUE (from_id, to_id),
    CONSTRAINT unique_to_id
        UNIQUE (from_id, to_id)
);

CREATE INDEX parameter_mapping_from_idx
    ON public.parameter_mapping (from_id);

CREATE INDEX parameter_mapping_job_idx
    ON public.parameter_mapping (job_id);

CREATE INDEX parameter_mapping_to_idx
    ON public.parameter_mapping (to_id);

CREATE INDEX parameter_type_class_name_idx
    ON public.parameter_type (class_name);

CREATE INDEX parameter_type_job_definition_idx
    ON public.parameter_type (job_definition_id);

CREATE INDEX parameter_type_parameter_usage_idx
    ON public.parameter_type (parameter_usage);

CREATE TABLE public.pipeline
(
    id           bigint                   NOT NULL
        CONSTRAINT workflow_pkey
            PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        CONSTRAINT workflow_name_unique
            UNIQUE,
    type         varchar(255)             NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.process
(
    id                           bigint                   NOT NULL
        PRIMARY KEY,
    version                      bigint                   NOT NULL,
    finished                     boolean                  NOT NULL,
    job_execution_plan_id        bigint                   NOT NULL
        CONSTRAINT fked8d1e6f4c5736c4
            REFERENCES public.job_execution_plan,
    start_job_class              varchar(255)             NOT NULL,
    started                      timestamp                NOT NULL,
    comment_id                   bigint
        CONSTRAINT process_comment_fk
            REFERENCES public.comment
        CONSTRAINT "FK504flhgywrloqkdr6u2gpjsrm"
            REFERENCES public.comment,
    operator_is_aware_of_failure boolean                  NOT NULL,
    restarted_id                 bigint
        UNIQUE
        REFERENCES public.process,
    date_created                 timestamp WITH TIME ZONE NOT NULL,
    last_updated                 timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX finished_idx
    ON public.process (finished);

CREATE INDEX job_execution_plan_idx
    ON public.process (job_execution_plan_id);

CREATE INDEX process_started_idx
    ON public.process (started);

CREATE TABLE public.process_parameter
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    class_name   varchar(255)             NOT NULL,
    process_id   bigint                   NOT NULL
        CONSTRAINT uc_process_parameterprocess_id_col
            UNIQUE
        CONSTRAINT fk9928cdd967afcb2c
            REFERENCES public.process,
    value        varchar(255)             NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

ALTER TABLE public.job_execution_plan
    ADD CONSTRAINT fkb8aba8b224d733a9
        FOREIGN KEY (process_parameter_id) REFERENCES public.process_parameter;

CREATE INDEX process_parameter_process_idx
    ON public.process_parameter (process_id);

CREATE TABLE public.processing_error
(
    id                     bigint                   NOT NULL
        PRIMARY KEY,
    version                bigint                   NOT NULL,
    error_message          text                     NOT NULL,
    stack_trace_identifier varchar(255),
    date_created           timestamp WITH TIME ZONE NOT NULL,
    last_updated           timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.processing_step
(
    id                bigint                   NOT NULL
        PRIMARY KEY,
    version           bigint                   NOT NULL,
    job_class         varchar(255),
    job_definition_id bigint                   NOT NULL
        CONSTRAINT fk6ab80478ce53a51d
            REFERENCES public.job_definition,
    next_id           bigint
        CONSTRAINT fk6ab804782816d254
            REFERENCES public.processing_step,
    previous_id       bigint
        CONSTRAINT fk6ab804787e0a73d0
            REFERENCES public.processing_step,
    process_id        bigint                   NOT NULL
        CONSTRAINT fk6ab8047867afcb2c
            REFERENCES public.process,
    class             varchar(255)             NOT NULL,
    original_id       bigint
        CONSTRAINT fk6ab804782133feb6
            REFERENCES public.processing_step,
    decision_id       bigint
        CONSTRAINT fk6ab804786a1d9abf
            REFERENCES public.job_decision,
    date_created      timestamp WITH TIME ZONE NOT NULL,
    last_updated      timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX processing_step_next_idx
    ON public.processing_step (next_id);

CREATE INDEX processing_step_process_idx
    ON public.processing_step (process_id);

CREATE INDEX processing_step_previous_idx
    ON public.processing_step (previous_id);

CREATE INDEX decision_processing_step_decision_idx
    ON public.processing_step (decision_id);

CREATE INDEX processing_step_job_definition_idx
    ON public.processing_step (job_definition_id);

CREATE INDEX restarted_processing_step_original_idx
    ON public.processing_step (original_id);

CREATE TABLE public.processing_step_parameter
(
    processing_step_input_id  bigint
        CONSTRAINT fkde767ca2a7ebf604
            REFERENCES public.processing_step,
    parameter_id              bigint
        CONSTRAINT fkde767ca21e4c5c6c
            REFERENCES public.parameter,
    processing_step_output_id bigint
        CONSTRAINT fkde767ca2a5a4aa7f
            REFERENCES public.processing_step
);

CREATE INDEX processing_step_parameter_processing_step_input_idx
    ON public.processing_step_parameter (processing_step_input_id);

CREATE INDEX processing_step_parameter_processing_step_output_idx
    ON public.processing_step_parameter (processing_step_output_id);

CREATE INDEX processing_step_parameter_parameter_idx
    ON public.processing_step_parameter (parameter_id);

CREATE TABLE public.processing_step_update
(
    id                 bigint                   NOT NULL
        PRIMARY KEY,
    version            bigint                   NOT NULL,
    date               timestamp                NOT NULL,
    error_id           bigint
        CONSTRAINT fk3bf8ba70d848db39
            REFERENCES public.processing_error,
    previous_id        bigint
        CONSTRAINT fk3bf8ba70869fd2f9
            REFERENCES public.processing_step_update,
    processing_step_id bigint                   NOT NULL
        CONSTRAINT fk3bf8ba70b6502eaf
            REFERENCES public.processing_step,
    state              varchar(255)             NOT NULL,
    date_created       timestamp WITH TIME ZONE NOT NULL,
    last_updated       timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX processing_step_update_date_idx
    ON public.processing_step_update (date);

CREATE INDEX processing_step_update_processing_step_idx
    ON public.processing_step_update (processing_step_id);

CREATE INDEX processing_step_update_state_idx
    ON public.processing_step_update (state);

CREATE INDEX processing_step_update_error_idx
    ON public.processing_step_update (error_id);

CREATE INDEX processing_step_update_previous_idx
    ON public.processing_step_update (previous_id);

CREATE TABLE public.project_group
(
    id           bigint                   NOT NULL
        CONSTRAINT "project_groupPK"
            PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        CONSTRAINT uc_project_groupname_col
            UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.project_role
(
    id           bigint                   NOT NULL
        CONSTRAINT project_role_pk
            PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        CONSTRAINT uc_project_rolename_col
            UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.reference_genome
(
    id                              bigint                   NOT NULL
        PRIMARY KEY,
    version                         bigint                   NOT NULL,
    name                            varchar(255)             NOT NULL
        CONSTRAINT uc_reference_genomename_col
            UNIQUE,
    file_name_prefix                varchar(255)             NOT NULL,
    length                          bigint                   NOT NULL,
    length_withoutn                 bigint                   NOT NULL,
    path                            varchar(255)             NOT NULL
        UNIQUE,
    length_ref_chromosomes          bigint                   NOT NULL,
    length_ref_chromosomes_withoutn bigint                   NOT NULL,
    cytosine_positions_index        varchar(255),
    chromosome_length_file_path     varchar(255),
    chromosome_prefix               varchar(255)             NOT NULL,
    chromosome_suffix               varchar(255)             NOT NULL,
    finger_printing_file_name       varchar(255),
    mappability_file                varchar(500),
    replication_time_file           varchar(500),
    gc_content_file                 varchar(255),
    genetic_map_file                varchar(500),
    known_haplotypes_file           varchar(500),
    known_haplotypes_legend_file    varchar(500),
    genetic_map_filex               varchar(500),
    known_haplotypes_filex          varchar(500),
    known_haplotypes_legend_filex   varchar(500),
    date_created                    timestamp WITH TIME ZONE NOT NULL,
    last_updated                    timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.bed_file
(
    id                         bigint                   NOT NULL
        CONSTRAINT "bed_filePK"
            PRIMARY KEY,
    version                    bigint                   NOT NULL,
    library_preparation_kit_id bigint                   NOT NULL
        CONSTRAINT fk5317da7aaa9ddd9a
            REFERENCES public.library_preparation_kit,
    file_name                  varchar(255)             NOT NULL,
    reference_genome_id        bigint                   NOT NULL
        CONSTRAINT fk5317da7ae9290d8b
            REFERENCES public.reference_genome,
    target_size                bigint                   NOT NULL,
    merged_target_size         bigint                   NOT NULL,
    date_created               timestamp WITH TIME ZONE NOT NULL,
    last_updated               timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK195a23ffbe4428ba7eaaccb47645"
        UNIQUE (library_preparation_kit_id, reference_genome_id),
    CONSTRAINT "UK90f1dddc247b084bf6ea7df25813"
        UNIQUE (reference_genome_id, file_name)
);

CREATE INDEX bed_file_library_preparation_kit_idx
    ON public.bed_file (library_preparation_kit_id);

CREATE INDEX bed_file_reference_genome_idx
    ON public.bed_file (reference_genome_id);

CREATE TABLE public.gene_model
(
    id                  bigint       NOT NULL
        PRIMARY KEY,
    version             bigint       NOT NULL,
    path                varchar(255) NOT NULL,
    date_created        timestamp    NOT NULL,
    dex_seq_file_name   varchar(255),
    exclude_file_name   varchar(255) NOT NULL,
    file_name           varchar(255) NOT NULL,
    gc_file_name        varchar(255),
    last_updated        timestamp    NOT NULL,
    reference_genome_id bigint       NOT NULL
        CONSTRAINT gene_model__reference_genome_fkey
            REFERENCES public.reference_genome,
    CONSTRAINT gene_model__path_reference_genome_id_unique
        UNIQUE (reference_genome_id, path)
);

CREATE INDEX gene_model_reference_genome_idx
    ON public.gene_model (reference_genome_id);

CREATE TABLE public.reference_genome_entry
(
    id                  bigint                   NOT NULL
        CONSTRAINT "reference_genPK"
            PRIMARY KEY,
    version             bigint                   NOT NULL,
    alias               varchar(255)             NOT NULL,
    classification      varchar(255)             NOT NULL,
    length              bigint                   NOT NULL,
    length_withoutn     bigint                   NOT NULL,
    name                varchar(255)             NOT NULL,
    reference_genome_id bigint                   NOT NULL
        CONSTRAINT fkcfc04f9ee9290d8b
            REFERENCES public.reference_genome,
    date_created        timestamp WITH TIME ZONE NOT NULL,
    last_updated        timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UKeed0aae143d64f14fcf94553c9ec"
        UNIQUE (reference_genome_id, name),
    CONSTRAINT "UKf4aa9c070dc37d9d875cb6f7e5ca"
        UNIQUE (reference_genome_id, alias)
);

CREATE INDEX reference_genome_entry_reference_genome_idx
    ON public.reference_genome_entry (reference_genome_id);

CREATE TABLE public.role
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    authority    varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.fastq_import_instance
(
    id             bigint                   NOT NULL
        CONSTRAINT run_segment_pkey
            PRIMARY KEY,
    version        bigint                   NOT NULL,
    otrs_ticket_id bigint
        CONSTRAINT fk_otrs_ticket_id
            REFERENCES public.otrs_ticket,
    import_mode    varchar(255)             NOT NULL,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.meta_data_file
(
    id                       bigint                   NOT NULL
        PRIMARY KEY,
    version                  bigint                   NOT NULL,
    date_created             timestamp                NOT NULL,
    file_name                varchar(255)             NOT NULL,
    file_path                varchar(255)             NOT NULL,
    fastq_import_instance_id bigint                   NOT NULL
        CONSTRAINT fkbdd9073711d99c9
            REFERENCES public.fastq_import_instance,
    md5sum                   varchar(255),
    last_updated             timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX meta_data_file_fastq_import_instance_idx
    ON public.meta_data_file (fastq_import_instance_id);

CREATE INDEX fastq_import_instance_otrs_ticket_idx
    ON public.fastq_import_instance (otrs_ticket_id);

CREATE TABLE public.seq_center_import_dirs_allow_linking
(
    seq_center_id                    bigint       NOT NULL
        REFERENCES public.seq_center,
    import_dirs_allow_linking_string varchar(255) NOT NULL,
    PRIMARY KEY (seq_center_id, import_dirs_allow_linking_string)
);

CREATE TABLE public.seq_platform_model_label_import_alias
(
    seq_platform_model_label_id bigint       NOT NULL
        CONSTRAINT "seq_platform_model_label_FK"
            REFERENCES public.seq_platform_model_label
        CONSTRAINT "FKamsibsqwqs8galdrsc7voj3q1"
            REFERENCES public.seq_platform_model_label,
    import_alias_string         varchar(255) NOT NULL
        CONSTRAINT seq_platform_model_label_import_alias_import_alias_string_uniqu
            UNIQUE,
    CONSTRAINT seq_platform_model_label_alias_pkey
        PRIMARY KEY (seq_platform_model_label_id, import_alias_string)
);

CREATE TABLE public.seq_type_import_alias
(
    seq_type_id         bigint       NOT NULL
        CONSTRAINT "seq_type_FK"
            REFERENCES public.seq_type
        CONSTRAINT "FKe38r5kt3fs5scywq181ha194d"
            REFERENCES public.seq_type,
    import_alias_string varchar(255) NOT NULL,
    CONSTRAINT seq_type_alias_pkey
        PRIMARY KEY (seq_type_id, import_alias_string)
);

CREATE TABLE public.sequencing_kit_label_import_alias
(
    sequencing_kit_label_id bigint       NOT NULL
        CONSTRAINT "sequencing_kit_label_FK"
            REFERENCES public.sequencing_kit_label
        CONSTRAINT "FK8r5xo3dh3dw7k4nl3qjwcnfiq"
            REFERENCES public.sequencing_kit_label,
    import_alias_string     varchar(255) NOT NULL
        CONSTRAINT sequencing_kit_label_import_alias_import_alias_string_unique
            UNIQUE,
    CONSTRAINT sequencing_kit_label_alias_pkey
        PRIMARY KEY (sequencing_kit_label_id, import_alias_string)
);
""")
    }
    changeSet(author: "otp", id: "intial-database-schema-2") {
        sql("""
CREATE TABLE public.software_tool
(
    id              bigint                   NOT NULL
        PRIMARY KEY,
    version         bigint                   NOT NULL,
    program_name    varchar(255)             NOT NULL,
    program_version varchar(255)             NOT NULL,
    type            varchar(255)             NOT NULL,
    date_created    timestamp WITH TIME ZONE NOT NULL,
    last_updated    timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK79e9db6b093294dfe0036ab264a4"
        UNIQUE (type, program_version, program_name)
);

CREATE TABLE public.alignment_params
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    genome       varchar(255),
    params       varchar(255),
    pipeline_id  bigint                   NOT NULL
        CONSTRAINT fk3dbeff6254649dfd
            REFERENCES public.software_tool,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.software_tool_identifier
(
    id               bigint                   NOT NULL
        PRIMARY KEY,
    version          bigint                   NOT NULL,
    name             varchar(255)             NOT NULL,
    software_tool_id bigint                   NOT NULL
        CONSTRAINT fk8a71b3184d664af
            REFERENCES public.software_tool,
    date_created     timestamp WITH TIME ZONE NOT NULL,
    last_updated     timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX software_tool_identifier_software_tool_idx
    ON public.software_tool_identifier (software_tool_id);

CREATE TABLE public.stat_size_file_name
(
    id                  bigint                   NOT NULL
        PRIMARY KEY,
    version             bigint                   NOT NULL,
    name                varchar(255)             NOT NULL,
    reference_genome_id bigint                   NOT NULL
        CONSTRAINT fk_kv0mt33uml6focn0ds5iyav3d
            REFERENCES public.reference_genome,
    date_created        timestamp WITH TIME ZONE NOT NULL,
    last_updated        timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK5907adb0f38a1b425be48b9e0357"
        UNIQUE (reference_genome_id, name)
);

CREATE INDEX unique_stat_size_file_name_name_reference_genome_id_idx
    ON public.stat_size_file_name (reference_genome_id, name);

CREATE INDEX stat_size_file_name_reference_genome_idx
    ON public.stat_size_file_name (reference_genome_id);

CREATE TABLE public.tool_name
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        CONSTRAINT tool_name__name_unique
            UNIQUE,
    path         varchar(255)             NOT NULL
        CONSTRAINT tool_name__path_unique
            UNIQUE,
    type         varchar(255)             NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.reference_genome_index
(
    id                  bigint                   NOT NULL
        PRIMARY KEY,
    version             bigint                   NOT NULL,
    index_tool_version  varchar(255)             NOT NULL,
    path                varchar(255)             NOT NULL,
    reference_genome_id bigint                   NOT NULL
        CONSTRAINT reference_genome_index__reference_genome_id_fkey
            REFERENCES public.reference_genome,
    tool_name_id        bigint                   NOT NULL
        CONSTRAINT reference_genome_index__tool_name_id_fkey
            REFERENCES public.tool_name,
    date_created        timestamp WITH TIME ZONE NOT NULL,
    last_updated        timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT reference_genome_index__reference_genome_id_tool_name_id_index_
        UNIQUE (reference_genome_id, tool_name_id, index_tool_version),
    CONSTRAINT reference_genome_index__reference_genome_id_tool_name_id_path_u
        UNIQUE (reference_genome_id, tool_name_id, path)
);

CREATE INDEX reference_genome_index_reference_genome_idx
    ON public.reference_genome_index (reference_genome_id);

CREATE INDEX reference_genome_index_tool_name_idx
    ON public.reference_genome_index (tool_name_id);

CREATE TABLE public.tumor_entity
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.users
(
    id                        bigint                   NOT NULL
        PRIMARY KEY,
    version                   bigint                   NOT NULL,
    email                     varchar(255)             NOT NULL,
    enabled                   boolean                  NOT NULL,
    password                  varchar(255)             NOT NULL,
    username                  varchar(255)
        UNIQUE,
    date_created              timestamp WITH TIME ZONE NOT NULL,
    last_updated              timestamp WITH TIME ZONE NOT NULL,
    accepted_privacy_policy   boolean                  NOT NULL,
    real_name                 varchar(255),
    planned_deactivation_date timestamp
);

CREATE TABLE public.audit_log
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    user_id      bigint                   NOT NULL
        REFERENCES public.users,
    timestamp    date                     NOT NULL,
    action       varchar(255)             NOT NULL,
    description  text,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.shutdown_information
(
    id              bigint                   NOT NULL
        PRIMARY KEY,
    version         bigint                   NOT NULL,
    canceled        timestamp,
    canceled_by_id  bigint
        CONSTRAINT fkdc5608c329604b3a
            REFERENCES public.users,
    initiated       timestamp                NOT NULL,
    initiated_by_id bigint                   NOT NULL
        CONSTRAINT fkdc5608c3a982820c
            REFERENCES public.users,
    reason          varchar(255)             NOT NULL,
    succeeded       timestamp,
    date_created    timestamp WITH TIME ZONE NOT NULL,
    last_updated    timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.user_role
(
    role_id      bigint                   NOT NULL
        CONSTRAINT fk143bf46aa4e7b28c
            REFERENCES public.role,
    user_id      bigint                   NOT NULL
        CONSTRAINT fk143bf46a4a12766c
            REFERENCES public.users,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    PRIMARY KEY (role_id, user_id)
);

CREATE TABLE public.document_type
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    description  text                     NOT NULL,
    title        varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    sort_order   integer                  NOT NULL
);

CREATE TABLE public.document
(
    id               bigint                   NOT NULL
        PRIMARY KEY,
    version          bigint                   NOT NULL,
    content          bytea,
    format_type      varchar(255)             NOT NULL,
    document_type_id bigint                   NOT NULL
        CONSTRAINT fk_document__document_type_id
            REFERENCES public.document_type,
    date_created     timestamp WITH TIME ZONE NOT NULL,
    last_updated     timestamp WITH TIME ZONE NOT NULL,
    link             varchar(255)
);

CREATE INDEX document__document_type_idx
    ON public.document (document_type_id);

CREATE INDEX document_type_title_idx
    ON public.document_type (title);

CREATE TABLE public.ega_library_selection
(
    id                         bigint                   NOT NULL
        PRIMARY KEY,
    version                    bigint                   NOT NULL,
    library_selection_ega_name varchar(255)             NOT NULL
        UNIQUE,
    date_created               timestamp WITH TIME ZONE NOT NULL,
    last_updated               timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.ega_library_selection_library_preparation_kit
(
    library_preparation_kit_id                        bigint NOT NULL
        CONSTRAINT ega_library_selection_library_p_library_preparation_kit_id_fkey
            REFERENCES public.library_preparation_kit,
    ega_library_selection_library_preparation_kits_id bigint NOT NULL
        CONSTRAINT "FKqnwi08tdiqy0vqnb8sk9s25d9"
            REFERENCES public.ega_library_selection,
    PRIMARY KEY (ega_library_selection_library_preparation_kits_id, library_preparation_kit_id)
);

CREATE TABLE public.ega_library_source
(
    id                      bigint                   NOT NULL
        PRIMARY KEY,
    version                 bigint                   NOT NULL,
    library_source_ega_name varchar(255)             NOT NULL
        UNIQUE,
    date_created            timestamp WITH TIME ZONE NOT NULL,
    last_updated            timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.ega_library_source_seq_type
(
    seq_type_id                     bigint NOT NULL
        REFERENCES public.seq_type,
    ega_library_source_seq_types_id bigint NOT NULL
        CONSTRAINT "FKc2x64x7h24c2m8dgo1arpyq49"
            REFERENCES public.ega_library_source,
    PRIMARY KEY (ega_library_source_seq_types_id, seq_type_id)
);

CREATE TABLE public.ega_library_strategy
(
    id                        bigint                   NOT NULL
        PRIMARY KEY,
    version                   bigint                   NOT NULL,
    library_strategy_ega_name varchar(255)             NOT NULL
        UNIQUE,
    date_created              timestamp WITH TIME ZONE NOT NULL,
    last_updated              timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.ega_library_strategy_seq_type
(
    seq_type_id                       bigint NOT NULL
        REFERENCES public.seq_type,
    ega_library_strategy_seq_types_id bigint NOT NULL
        CONSTRAINT "FKohsccqo6onn7g3mj0lrwtpmyr"
            REFERENCES public.ega_library_strategy,
    PRIMARY KEY (ega_library_strategy_seq_types_id, seq_type_id)
);

CREATE TABLE public.ega_platform_model
(
    id                      bigint                   NOT NULL
        PRIMARY KEY,
    version                 bigint                   NOT NULL,
    platform_model_ega_name varchar(255)             NOT NULL
        UNIQUE,
    date_created            timestamp WITH TIME ZONE NOT NULL,
    last_updated            timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.ega_platform_model_seq_platform_model_label
(
    seq_platform_model_label_id                     bigint NOT NULL
        CONSTRAINT ega_platform_model_seq_platfor_seq_platform_model_label_id_fkey
            REFERENCES public.seq_platform_model_label,
    ega_platform_model_seq_platform_model_labels_id bigint NOT NULL
        CONSTRAINT "FK7fwld2di8xuqrbwrgn3491mhm"
            REFERENCES public.ega_platform_model,
    PRIMARY KEY (ega_platform_model_seq_platform_model_labels_id, seq_platform_model_label_id)
);

CREATE TABLE public.strain
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.keyword
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        UNIQUE,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.omitted_message
(
    id           bigint                   NOT NULL
        CONSTRAINT skipped_message_pkey
            PRIMARY KEY,
    version      bigint                   NOT NULL,
    message      text                     NOT NULL,
    category     varchar(255)             NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.workflow
(
    id                     bigint                   NOT NULL
        CONSTRAINT workflow_pkey1
            PRIMARY KEY,
    version                bigint                   NOT NULL,
    name                   varchar(255)             NOT NULL
        CONSTRAINT workflow_name_unique_constraint
            UNIQUE,
    bean_name              varchar(255),
    enabled                boolean                  NOT NULL,
    deprecated_date        timestamp WITH TIME ZONE,
    comment_id             bigint
        REFERENCES public.comment,
    date_created           timestamp WITH TIME ZONE NOT NULL,
    last_updated           timestamp WITH TIME ZONE NOT NULL,
    priority               smallint                 NOT NULL,
    max_parallel_workflows smallint                 NOT NULL
);

CREATE INDEX workflow_comment_idx
    ON public.workflow (comment_id);

CREATE TABLE public.external_workflow_config_fragment
(
    id               bigint       NOT NULL
        CONSTRAINT "external_workflow_config_fragmentPK"
            PRIMARY KEY,
    version          bigint       NOT NULL,
    object_version   integer      NOT NULL,
    date_created     timestamp    NOT NULL,
    previous_id      bigint
        CONSTRAINT "FKo6gbflkmf1snoouep44se0dcw"
            REFERENCES public.external_workflow_config_fragment,
    last_updated     timestamp    NOT NULL,
    deprecation_date date,
    comment_id       bigint
        CONSTRAINT "FKfh915bfvqclb0wh281k7abrpd"
            REFERENCES public.comment,
    config_values    text         NOT NULL,
    name             varchar(255) NOT NULL
);

CREATE UNIQUE INDEX external_workflow_config_fragment_name_idx
    ON public.external_workflow_config_fragment (name)
    WHERE (deprecation_date IS NULL);

CREATE INDEX external_workflow_config_fragment_previous_idx
    ON public.external_workflow_config_fragment (previous_id);

CREATE TABLE public.external_workflow_config_selector
(
    id                                   bigint       NOT NULL
        CONSTRAINT "external_workflow_config_selectorPK"
            PRIMARY KEY,
    version                              bigint       NOT NULL,
    date_created                         timestamp    NOT NULL,
    last_updated                         timestamp    NOT NULL,
    priority                             integer      NOT NULL,
    name                                 varchar(255) NOT NULL
        CONSTRAINT uc_external_workflow_config_selectorname_col
            UNIQUE,
    external_workflow_config_fragment_id bigint       NOT NULL
        CONSTRAINT "FKfwuhqgqmdojj02p8ip5ivveao"
            REFERENCES public.external_workflow_config_fragment,
    selector_type                        varchar(255) NOT NULL
);

CREATE INDEX external_workflow_config_selector_external_workflow_config_frag
    ON public.external_workflow_config_selector (external_workflow_config_fragment_id);

CREATE INDEX external_workflow_config_selector_selector_type_idx
    ON public.external_workflow_config_selector (selector_type);

CREATE TABLE public.external_workflow_config_selector_library_preparation_kit
(
    external_workflow_config_selector_library_preparation_kits_id bigint NOT NULL
        CONSTRAINT "FKcv03ofbaf0j6lhhin5nyy1fep"
            REFERENCES public.external_workflow_config_selector,
    library_preparation_kit_id                                    bigint
        CONSTRAINT "FKj8qu0ub20hbgpcx10mjey7pen"
            REFERENCES public.library_preparation_kit
);

CREATE INDEX external_workflow_config_selector_library_preparation_kit_exter
    ON public.external_workflow_config_selector_library_preparation_kit (external_workflow_config_selector_library_preparation_kits_id);

CREATE INDEX external_workflow_config_selector_library_preparation_kit_libra
    ON public.external_workflow_config_selector_library_preparation_kit (library_preparation_kit_id);

CREATE TABLE public.external_workflow_config_selector_reference_genome
(
    external_workflow_config_selector_reference_genomes_id bigint NOT NULL
        CONSTRAINT "FKemd1ga88oa1krw8sb9sopn39f"
            REFERENCES public.external_workflow_config_selector,
    reference_genome_id                                    bigint
        CONSTRAINT "FKej0xflqol53cg0yok9kj0671q"
            REFERENCES public.reference_genome
);

CREATE INDEX external_workflow_config_selector_reference_genome_external_wor
    ON public.external_workflow_config_selector_reference_genome (external_workflow_config_selector_reference_genomes_id);

CREATE INDEX external_workflow_config_selector_reference_genome_reference_ge
    ON public.external_workflow_config_selector_reference_genome (reference_genome_id);

CREATE TABLE public.external_workflow_config_selector_seq_type
(
    external_workflow_config_selector_seq_types_id bigint NOT NULL
        CONSTRAINT "FKlk5kih0j8my884aobmo8fphap"
            REFERENCES public.external_workflow_config_selector,
    seq_type_id                                    bigint
        CONSTRAINT "FKr74u0p64hvmhjgd9m3fx4k45f"
            REFERENCES public.seq_type
);

CREATE INDEX external_workflow_config_selector_seq_type_external_workflow_co
    ON public.external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id);

CREATE INDEX external_workflow_config_selector_seq_type_seq_type_id_idx
    ON public.external_workflow_config_selector_seq_type (seq_type_id);

CREATE TABLE public.external_workflow_config_selector_workflow
(
    external_workflow_config_selector_workflows_id bigint NOT NULL
        CONSTRAINT "FKo78141440cns5kp2fbxxdaja2"
            REFERENCES public.external_workflow_config_selector,
    workflow_id                                    bigint
        CONSTRAINT "FKjn7vkp6lnyq3y5ry9dwov0v1h"
            REFERENCES public.workflow
);

CREATE INDEX external_workflow_config_selector_workflow_external_workflow_co
    ON public.external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id);

CREATE INDEX external_workflow_config_selector_workflow_workflow_id_idx
    ON public.external_workflow_config_selector_workflow (workflow_id);

CREATE TABLE public.workflow_reference_genome
(
    workflow_allowed_reference_genomes_id bigint NOT NULL
        CONSTRAINT "FKdt5yxfdx2f93cs61ft48v9b6w"
            REFERENCES public.workflow,
    reference_genome_id                   bigint
        CONSTRAINT "FKk7iras1xy5c9veghgbndrybxq"
            REFERENCES public.reference_genome
);

CREATE INDEX workflow_reference_genome_workflow_allowed_reference_genomes_id
    ON public.workflow_reference_genome (workflow_allowed_reference_genomes_id);

CREATE INDEX workflow_reference_genome_reference_genome_id_idx
    ON public.workflow_reference_genome (reference_genome_id);

CREATE TABLE public.workflow_run_external_workflow_config_fragment
(
    workflow_run_configs_id              bigint NOT NULL,
    external_workflow_config_fragment_id bigint
        CONSTRAINT "FKmtrmx711o92jkvito9syjsyb6"
            REFERENCES public.external_workflow_config_fragment,
    configs_idx                          integer
);

CREATE TABLE public.workflow_seq_type
(
    workflow_supported_seq_types_id bigint NOT NULL
        CONSTRAINT "FKpgv079b145o8wxaikfutjn2pb"
            REFERENCES public.workflow,
    seq_type_id                     bigint
        CONSTRAINT "FKjvsn4l5v6lirixq0i9jv57obj"
            REFERENCES public.seq_type
);

CREATE INDEX workflow_seq_type_workflow_supported_seq_types_id_idx
    ON public.workflow_seq_type (workflow_supported_seq_types_id);

CREATE INDEX workflow_seq_type_seq_type_id_idx
    ON public.workflow_seq_type (seq_type_id);

CREATE TABLE public.workflow_version
(
    id               bigint       NOT NULL
        CONSTRAINT "workflow_versionPK"
            PRIMARY KEY,
    workflow_version varchar(255) NOT NULL,
    date_created     timestamp    NOT NULL,
    last_updated     timestamp    NOT NULL,
    workflow_id      bigint       NOT NULL
        CONSTRAINT "FKgjhy6w5q8buydj1hboch9dxl5"
            REFERENCES public.workflow,
    version          bigint       NOT NULL,
    CONSTRAINT "UKcb8bd959752a92ddf76fef03121d"
        UNIQUE (workflow_version, workflow_id)
);

CREATE TABLE public.external_workflow_config_selector_workflow_version
(
    external_workflow_config_selector_workflow_versions_id bigint NOT NULL
        CONSTRAINT "FK9lw1h4w9cp3na1s34uiwu2f5t"
            REFERENCES public.external_workflow_config_selector,
    workflow_version_id                                    bigint
        CONSTRAINT "FK8dissi4rwptvv84cp7gdon3ld"
            REFERENCES public.workflow_version
);

CREATE INDEX external_workflow_config_selector_workflow_version_external_wor
    ON public.external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id);

CREATE INDEX external_workflow_config_selector_workflow_version_workflow_ver
    ON public.external_workflow_config_selector_workflow_version (workflow_version_id);

CREATE INDEX workflow_version_workflow_idx
    ON public.workflow_version (workflow_id);

CREATE TABLE public.workflow_error
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    message      text                     NOT NULL,
    stacktrace   text                     NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.processing_priority
(
    id                             bigint       NOT NULL
        CONSTRAINT "processing_priorityPK"
            PRIMARY KEY,
    version                        bigint       NOT NULL,
    date_created                   timestamp    NOT NULL,
    last_updated                   timestamp    NOT NULL,
    roddy_config_suffix            varchar(255) NOT NULL,
    queue                          varchar(255) NOT NULL,
    allowed_parallel_workflow_runs integer      NOT NULL,
    priority                       integer      NOT NULL
        CONSTRAINT uc_processing_prioritypriority_col
            UNIQUE,
    name                           varchar(255) NOT NULL
        CONSTRAINT uc_processing_priorityname_col
            UNIQUE,
    error_mail_prefix              varchar(255) NOT NULL
);

CREATE TABLE public.project
(
    id                                 bigint                   NOT NULL
        PRIMARY KEY,
    version                            bigint                   NOT NULL,
    dir_name                           varchar(255)             NOT NULL
        CONSTRAINT uc_projectdir_name_col
            UNIQUE,
    name                               varchar(255)             NOT NULL
        UNIQUE,
    project_group_id                   bigint
        CONSTRAINT fked904b198281ea6d
            REFERENCES public.project_group,
    alignment_decider_bean_name        varchar(255)             NOT NULL,
    force_copy_files                   boolean                  NOT NULL,
    name_in_metadata_files             varchar(255),
    comment_id                         bigint
        CONSTRAINT project_comment_fk
            REFERENCES public.comment,
    description                        text,
    dir_analysis                       text,
    unix_group                         varchar(255)             NOT NULL,
    finger_printing                    boolean                  NOT NULL,
    tumor_entity_id                    bigint
        CONSTRAINT project_tumor_entity_fk
            REFERENCES public.tumor_entity,
    realm_id                           bigint                   NOT NULL
        REFERENCES public.realm,
    custom_final_notification          boolean                  NOT NULL,
    sample_identifier_parser_bean_name varchar(255)             NOT NULL,
    qc_threshold_handling              varchar(255)             NOT NULL,
    date_created                       timestamp WITH TIME ZONE NOT NULL,
    last_updated                       timestamp WITH TIME ZONE NOT NULL,
    processing_notification            boolean                  NOT NULL,
    qc_traffic_light_notification      boolean                  NOT NULL,
    closed                             boolean                  NOT NULL,
    individual_prefix                  varchar(255),
    end_date                           date,
    storage_until                      date,
    project_type                       varchar(255)             NOT NULL,
    related_projects                   varchar(255),
    internal_notes                     text,
    unique_individual_prefix           boolean                  NOT NULL,
    publicly_available                 boolean                  NOT NULL,
    processing_priority_id             bigint                   NOT NULL
        CONSTRAINT "FKly20vwdui47fy3awg5dwxkmfc"
            REFERENCES public.processing_priority,
    project_request_available          boolean                  NOT NULL,
    archived                           boolean                  NOT NULL
);

CREATE INDEX project_project_group_idx
    ON public.project (project_group_id);

CREATE INDEX project_name_idx
    ON public.project (name);

CREATE INDEX project_processing_priority_idx
    ON public.project (processing_priority_id);

CREATE INDEX project_name_in_metadata_files_idx
    ON public.project (name_in_metadata_files);

CREATE INDEX project_realm_idx
    ON public.project (realm_id);

CREATE INDEX project_unix_group_idx
    ON public.project (unix_group);

CREATE TABLE public.merging_criteria
(
    id                     bigint                   NOT NULL
        PRIMARY KEY,
    version                bigint                   NOT NULL,
    project_id             bigint                   NOT NULL
        CONSTRAINT fk_merging_criteria__project_id
            REFERENCES public.project,
    seq_type_id            bigint                   NOT NULL
        CONSTRAINT fk_merging_criteria__seq_type_id
            REFERENCES public.seq_type,
    use_lib_prep_kit       boolean                  NOT NULL,
    use_seq_platform_group varchar(255)             NOT NULL,
    date_created           timestamp WITH TIME ZONE NOT NULL,
    last_updated           timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT unique_project_id
        UNIQUE (seq_type_id, project_id)
);

CREATE TABLE public.processing_option
(
    id             bigint                   NOT NULL
        CONSTRAINT "processing_opPK"
            PRIMARY KEY,
    version        bigint                   NOT NULL,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    date_obsoleted timestamp WITH TIME ZONE,
    name           varchar(255)             NOT NULL,
    project_id     bigint
        CONSTRAINT fk95c3cb2151483d14
            REFERENCES public.project,
    type           varchar(255),
    value          text                     NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.processing_thresholds
(
    id              bigint                   NOT NULL
        CONSTRAINT "processing_thresholds_PK"
            PRIMARY KEY,
    version         bigint                   NOT NULL,
    project_id      bigint                   NOT NULL
        CONSTRAINT "project_FK"
            REFERENCES public.project
        CONSTRAINT "FKb1j34pggqajkqbuch7ybw2su9"
            REFERENCES public.project,
    seq_type_id     bigint                   NOT NULL
        CONSTRAINT "seq_type_FK"
            REFERENCES public.seq_type
        CONSTRAINT "FK8o7rmi93crkr1h2au59gx2u3p"
            REFERENCES public.seq_type,
    sample_type_id  bigint                   NOT NULL
        CONSTRAINT "sample_type_FK"
            REFERENCES public.sample_type,
    coverage        double precision,
    number_of_lanes integer,
    date_created    timestamp WITH TIME ZONE NOT NULL,
    last_updated    timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UKb7a01d2be3fdbf83d79c75252adb"
        UNIQUE (project_id, seq_type_id, sample_type_id)
);

CREATE INDEX processing_thresholds_project_idx
    ON public.processing_thresholds (project_id);

CREATE INDEX processing_thresholds_sample_type_idx
    ON public.processing_thresholds (sample_type_id);

CREATE INDEX processing_thresholds_seq_type_idx
    ON public.processing_thresholds (seq_type_id);

CREATE TABLE public.project_info
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    file_name    text                     NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    project_id   bigint                   NOT NULL
        CONSTRAINT "project_info_project_project_id_FK"
            REFERENCES public.project,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    comment      text,
    CONSTRAINT project_info_project_id_file_name_unique
        UNIQUE (project_id, file_name)
);

CREATE INDEX project_info_project_idx
    ON public.project_info (project_id);

CREATE TABLE public.qc_threshold
(
    id                      bigint                   NOT NULL
        PRIMARY KEY,
    version                 bigint                   NOT NULL,
    project_id              bigint
        CONSTRAINT "qc_threshold_project_FK"
            REFERENCES public.project,
    seq_type_id             bigint                   NOT NULL
        CONSTRAINT "qc_threshold_seqType_FK"
            REFERENCES public.seq_type,
    qc_property1            varchar(255)             NOT NULL,
    qc_property2            varchar(255),
    compare                 varchar(255)             NOT NULL,
    qc_class                varchar(255)             NOT NULL,
    warning_threshold_lower double precision,
    warning_threshold_upper double precision,
    error_threshold_lower   double precision,
    error_threshold_upper   double precision,
    date_created            timestamp WITH TIME ZONE NOT NULL,
    last_updated            timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX qc_threshold_project_idx
    ON public.qc_threshold (project_id);

CREATE INDEX qc_threshold_seqtype_idx
    ON public.qc_threshold (seq_type_id);

CREATE TABLE public.reference_genome_project_seq_type
(
    id                  bigint                   NOT NULL
        PRIMARY KEY,
    version             bigint                   NOT NULL,
    date_created        timestamp WITH TIME ZONE NOT NULL,
    deprecated_date     timestamp WITH TIME ZONE,
    project_id          bigint                   NOT NULL
        CONSTRAINT fka5af86d451483d14
            REFERENCES public.project,
    reference_genome_id bigint                   NOT NULL
        CONSTRAINT fka5af86d4e9290d8b
            REFERENCES public.reference_genome,
    seq_type_id         bigint                   NOT NULL
        CONSTRAINT fka5af86d45eeb3a93
            REFERENCES public.seq_type,
    sample_type_id      bigint
        CONSTRAINT fka5af86d4c4a27e75
            REFERENCES public.sample_type,
    stat_size_file_name varchar(255),
    last_updated        timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX reference_genome_project_seq_type_project_idx
    ON public.reference_genome_project_seq_type (project_id);

CREATE INDEX reference_genome_project_seq_type_reference_genome_idx
    ON public.reference_genome_project_seq_type (reference_genome_id);

CREATE INDEX reference_genome_project_seq_type_sample_type_idx
    ON public.reference_genome_project_seq_type (sample_type_id);

CREATE INDEX reference_genome_project_seq_type_seq_type_idx
    ON public.reference_genome_project_seq_type (seq_type_id);

CREATE TABLE public.reference_genome_project_seq_type_alignment_property
(
    id                                   bigint                   NOT NULL
        PRIMARY KEY,
    version                              bigint                   NOT NULL,
    reference_genome_project_seq_type_id bigint                   NOT NULL
        CONSTRAINT reference_genome_project_seq__reference_genome_project_seq_fkey
            REFERENCES public.reference_genome_project_seq_type,
    name                                 varchar(255)             NOT NULL,
    value                                varchar(500)             NOT NULL,
    date_created                         timestamp WITH TIME ZONE NOT NULL,
    last_updated                         timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK3a7102bd07e8f7e38140507ce831"
        UNIQUE (reference_genome_project_seq_type_id, name)
);

CREATE INDEX reference_genome_project_seq_type_alignment_property_reference_
    ON public.reference_genome_project_seq_type_alignment_property (reference_genome_project_seq_type_id);

CREATE TABLE public.sample_type_per_project
(
    id             bigint                   NOT NULL
        CONSTRAINT "sample_type_per_project_PK"
            PRIMARY KEY,
    version        bigint                   NOT NULL,
    project_id     bigint                   NOT NULL
        CONSTRAINT "project_FK"
            REFERENCES public.project
        CONSTRAINT "FKakoxk94oy68igcpbvumsdpnst"
            REFERENCES public.project,
    sample_type_id bigint                   NOT NULL
        CONSTRAINT "sample_type_FK"
            REFERENCES public.sample_type
        CONSTRAINT "FKdkn0ecyj5k85o5rwp1rfj8arm"
            REFERENCES public.sample_type,
    category       varchar(255)             NOT NULL,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UKa74a5644b48ab8b66ceb6d552ff2"
        UNIQUE (project_id, sample_type_id)
);

CREATE INDEX sample_type_per_project__category_idx
    ON public.sample_type_per_project (category);

CREATE INDEX sample_type_per_project__project__sample_type__category_idx
    ON public.sample_type_per_project (project_id, sample_type_id, category);

CREATE INDEX sample_type_per_project__sample_type_idx
    ON public.sample_type_per_project (sample_type_id);

CREATE TABLE public.seq_platform_group
(
    id                  bigint                   NOT NULL
        PRIMARY KEY,
    version             bigint                   NOT NULL,
    merging_criteria_id bigint
        CONSTRAINT fk_oerlov6l1h0pwc38mdqpdafc2
            REFERENCES public.merging_criteria,
    date_created        timestamp WITH TIME ZONE NOT NULL,
    last_updated        timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX seq_platform_group_merging_criteria_idx
    ON public.seq_platform_group (merging_criteria_id);

CREATE TABLE public.seq_platform_group_comment
(
    seq_platform_group_comments_id bigint
        CONSTRAINT seq_platform_group_comments_fkey
            REFERENCES public.seq_platform_group,
    comment_id                     bigint
        CONSTRAINT seq_platform_group_comments_comment_id_fkey
            REFERENCES public.comment,
    comments_idx                   integer
);

CREATE INDEX seq_platform_group_comments_id_idx
    ON public.seq_platform_group_comment (seq_platform_group_comments_id);

CREATE TABLE public.seq_platform_seq_platform_groups
(
    seq_platform_id       bigint NOT NULL
        CONSTRAINT fk_qyb85j8pdw41s1fut28gswos2
            REFERENCES public.seq_platform,
    seq_platform_group_id bigint NOT NULL
        CONSTRAINT fk_90hikoqdwqwbgnj973kthxn69
            REFERENCES public.seq_platform_group,
    PRIMARY KEY (seq_platform_id, seq_platform_group_id)
);

CREATE INDEX seq_platform_seq_platform_groups_seq_platform_group_id_idx
    ON public.seq_platform_seq_platform_groups (seq_platform_group_id);

CREATE TABLE public.user_project_role
(
    id                           bigint                   NOT NULL
        CONSTRAINT user_project_role_pk
            PRIMARY KEY,
    version                      bigint                   NOT NULL,
    project_id                   bigint                   NOT NULL
        CONSTRAINT project_fk
            REFERENCES public.project
        CONSTRAINT "FK9l1icyrvwmfc2lr9fskttk85b"
            REFERENCES public.project,
    user_id                      bigint                   NOT NULL
        CONSTRAINT user_fk
            REFERENCES public.users,
    enabled                      boolean                  NOT NULL,
    manage_users                 boolean                  NOT NULL,
    access_to_otp                boolean                  NOT NULL,
    access_to_files              boolean                  NOT NULL,
    manage_users_and_delegate    boolean                  NOT NULL,
    receives_notifications       boolean                  NOT NULL,
    date_created                 timestamp WITH TIME ZONE NOT NULL,
    last_updated                 timestamp WITH TIME ZONE NOT NULL,
    file_access_change_requested boolean                  NOT NULL,
    UNIQUE (user_id, project_id),
    CONSTRAINT "UK324f84fe9e2682e71201316081b9"
        UNIQUE (user_id, project_id)
);

CREATE INDEX user_project_role_project_idx
    ON public.user_project_role (project_id);

CREATE INDEX user_project_role_user_idx
    ON public.user_project_role (user_id);

CREATE TABLE public.ega_submission
(
    id              bigint                   NOT NULL
        CONSTRAINT submission_pkey
            PRIMARY KEY,
    version         bigint                   NOT NULL,
    ega_box         varchar(255)             NOT NULL,
    submission_name varchar(255)             NOT NULL,
    pub_med_id      varchar(255),
    state           varchar(255)             NOT NULL,
    study_abstract  text                     NOT NULL,
    study_name      varchar(255)             NOT NULL,
    study_type      varchar(255)             NOT NULL,
    project_id      bigint                   NOT NULL
        CONSTRAINT submission_project_id_fkey
            REFERENCES public.project,
    selection_state varchar(255)             NOT NULL,
    date_created    timestamp WITH TIME ZONE NOT NULL,
    last_updated    timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX ega_submission_project_idx
    ON public.ega_submission (project_id);

CREATE TABLE public.project_keywords
(
    project_id bigint NOT NULL
        REFERENCES public.project,
    keyword_id bigint NOT NULL
        REFERENCES public.keyword,
    PRIMARY KEY (project_id, keyword_id)
);
""")
    }
    changeSet(author: "otp", id: "intial-database-schema-3") {
        sql("""
CREATE TABLE public.workflow_run
(
    id                   bigint                   NOT NULL
        PRIMARY KEY,
    version              bigint                   NOT NULL,
    work_directory       text,
    combined_config      text                     NOT NULL,
    state                varchar(255)             NOT NULL,
    restarted_from_id    bigint
        CONSTRAINT uc_workflow_runrestarted_from_id_col
            UNIQUE
        REFERENCES public.workflow_run,
    omitted_message_id   bigint
        CONSTRAINT workflow_run_skipped_message_id_fkey
            REFERENCES public.omitted_message,
    workflow_id          bigint                   NOT NULL
        REFERENCES public.workflow,
    comment_id           bigint
        REFERENCES public.comment,
    date_created         timestamp WITH TIME ZONE NOT NULL,
    last_updated         timestamp WITH TIME ZONE NOT NULL,
    priority_id          bigint                   NOT NULL
        CONSTRAINT "FKmcntfdirb0cvtbkq6wmrqxpcw"
            REFERENCES public.processing_priority,
    project_id           bigint                   NOT NULL
        CONSTRAINT "FKa5td0udmh0u67gucn7dyk08md"
            REFERENCES public.project,
    display_name         varchar(255)             NOT NULL,
    job_can_be_restarted boolean                  NOT NULL,
    short_display_name   varchar(255)             NOT NULL,
    workflow_version_id  bigint
        CONSTRAINT "FKkd31d8by7f3xa95me1m6mvcm7"
            REFERENCES public.workflow_version
);

CREATE TABLE public.workflow_step
(
    id                 bigint                   NOT NULL
        PRIMARY KEY,
    version            bigint                   NOT NULL,
    date_created       timestamp WITH TIME ZONE NOT NULL,
    last_updated       timestamp WITH TIME ZONE NOT NULL,
    workflow_run_id    bigint                   NOT NULL
        CONSTRAINT workflow_step_workflow_run_id_fk
            REFERENCES public.workflow_run,
    bean_name          varchar(255)             NOT NULL,
    wes_identifier     varchar(255),
    state              varchar(255)             NOT NULL,
    workflow_error_id  bigint
        CONSTRAINT workflow_step_workflow_error_fk
            REFERENCES public.workflow_error,
    previous_id        bigint
        CONSTRAINT workflow_step_previous_fk
            REFERENCES public.workflow_step,
    restarted_from_id  bigint
        CONSTRAINT uc_workflow_steprestarted_from_id_col
            UNIQUE
        CONSTRAINT workflow_step_restarted_from_fk
            REFERENCES public.workflow_step,
    obsolete           boolean                  NOT NULL,
    comment_id         bigint
        CONSTRAINT workflow_step_comment_fk
            REFERENCES public.comment,
    workflow_steps_idx integer
);

CREATE INDEX workflow_step_workflow_run_id_idx
    ON public.workflow_step (workflow_run_id);

CREATE INDEX workflow_step_workflow_error_id_idx
    ON public.workflow_step (workflow_error_id);

CREATE INDEX workflow_step_previous_id_idx
    ON public.workflow_step (previous_id);

CREATE INDEX workflow_step_restarted_from_id_idx
    ON public.workflow_step (restarted_from_id);

CREATE INDEX workflow_step_comment_id_idx
    ON public.workflow_step (comment_id);

CREATE INDEX workflow_step_state_idx
    ON public.workflow_step (state);

CREATE INDEX workflow_run_restarted_from_idx
    ON public.workflow_run (restarted_from_id);

CREATE INDEX workflow_run_skipped_message_idx
    ON public.workflow_run (omitted_message_id);

CREATE INDEX workflow_run_workflow_idx
    ON public.workflow_run (workflow_id);

CREATE INDEX workflow_run_comment_idx
    ON public.workflow_run (comment_id);

CREATE INDEX workflow_run_priority_idx
    ON public.workflow_run (priority_id);

CREATE INDEX workflow_run_state_idx
    ON public.workflow_run (state);

CREATE INDEX workflow_run_project_idx
    ON public.workflow_run (project_id);

CREATE INDEX workflow_run_workflow_version_idx
    ON public.workflow_run (workflow_version_id);

CREATE TABLE public.workflow_artefact
(
    id                bigint                   NOT NULL
        PRIMARY KEY,
    version           bigint                   NOT NULL,
    produced_by_id    bigint
        REFERENCES public.workflow_run,
    state             varchar(255)             NOT NULL,
    withdrawn_date    timestamp WITH TIME ZONE,
    withdrawn_comment text,
    date_created      timestamp WITH TIME ZONE NOT NULL,
    last_updated      timestamp WITH TIME ZONE NOT NULL,
    output_role       varchar(255),
    display_name      varchar(255)             NOT NULL,
    artefact_type     varchar(255)             NOT NULL
);

CREATE INDEX workflow_artefact_produced_by_idx
    ON public.workflow_artefact (produced_by_id);

CREATE INDEX workflow_artefact_state_idx
    ON public.workflow_artefact (state);

CREATE INDEX workflow_artefact_artefact_type_idx
    ON public.workflow_artefact (artefact_type);

CREATE TABLE public.external_workflow_config_selector_project
(
    external_workflow_config_selector_projects_id bigint NOT NULL
        CONSTRAINT "FKqljoam7w0pu6pm67e1abtuw7y"
            REFERENCES public.external_workflow_config_selector,
    project_id                                    bigint
        CONSTRAINT "FK7dfljpx4fmy14113gknnnvsak"
            REFERENCES public.project
);

CREATE INDEX external_workflow_config_selector_project_external_workflow_con
    ON public.external_workflow_config_selector_project (external_workflow_config_selector_projects_id);

CREATE INDEX external_workflow_config_selector_project_project_id_idx
    ON public.external_workflow_config_selector_project (project_id);

CREATE TABLE public.reference_genome_selector
(
    id                  bigint    NOT NULL
        CONSTRAINT "reference_genome_selectorPK"
            PRIMARY KEY,
    version             bigint    NOT NULL,
    reference_genome_id bigint    NOT NULL
        CONSTRAINT "FKbymwu5t4ajsm10gtqdo295j1x"
            REFERENCES public.reference_genome,
    date_created        timestamp NOT NULL,
    last_updated        timestamp NOT NULL,
    project_id          bigint    NOT NULL
        CONSTRAINT "FKkj2sds6v3e892cqoltmi5tmas"
            REFERENCES public.project,
    seq_type_id         bigint    NOT NULL
        CONSTRAINT "FKqukg045oa53b5m0qj54itn0yp"
            REFERENCES public.seq_type,
    workflow_id         bigint    NOT NULL
        CONSTRAINT "FKn5vw01rtunramasshavae0has"
            REFERENCES public.workflow
);

CREATE INDEX reference_genome_selector_project_idx
    ON public.reference_genome_selector (project_id);

CREATE INDEX reference_genome_selector_reference_genome_idx
    ON public.reference_genome_selector (reference_genome_id);

CREATE INDEX reference_genome_selector_seq_type_idx
    ON public.reference_genome_selector (seq_type_id);

CREATE INDEX reference_genome_selector_workflow_idx
    ON public.reference_genome_selector (workflow_id);

CREATE TABLE public.workflow_log
(
    id               bigint                   NOT NULL
        PRIMARY KEY,
    version          bigint                   NOT NULL,
    date_created     timestamp WITH TIME ZONE NOT NULL,
    last_updated     timestamp WITH TIME ZONE NOT NULL,
    workflow_step_id bigint                   NOT NULL
        CONSTRAINT "FKpmkyuwhu92v5g0ytbobqicdq9"
            REFERENCES public.workflow_step,
    created_by       varchar(255)             NOT NULL
);

CREATE INDEX workflow_log_workflow_step_idx
    ON public.workflow_log (workflow_step_id);

CREATE TABLE public.workflow_message_log
(
    id      bigint NOT NULL
        PRIMARY KEY
        REFERENCES public.workflow_log,
    message text   NOT NULL
);

CREATE TABLE public.workflow_command_log
(
    id        bigint NOT NULL
        PRIMARY KEY
        REFERENCES public.workflow_log,
    command   text   NOT NULL,
    exit_code bigint NOT NULL,
    stdout    text   NOT NULL,
    stderr    text   NOT NULL
);

CREATE TABLE public.workflow_run_input_artefact
(
    id                   bigint       NOT NULL
        CONSTRAINT "workflow_run_input_artefactPK"
            PRIMARY KEY,
    version              bigint       NOT NULL,
    date_created         timestamp    NOT NULL,
    last_updated         timestamp    NOT NULL,
    role                 varchar(255) NOT NULL,
    workflow_artefact_id bigint       NOT NULL
        CONSTRAINT "FKmr9b61qlilhcvk5ey3nxojtx9"
            REFERENCES public.workflow_artefact,
    workflow_run_id      bigint       NOT NULL
        CONSTRAINT "FK9fy0nd3op8q37e8fkralcq8gq"
            REFERENCES public.workflow_run,
    CONSTRAINT "UK6cb454c8bcac6b4d804f975198d6"
        UNIQUE (workflow_run_id, workflow_artefact_id),
    CONSTRAINT "UKcabae7dda3e0a9f765ad6d9d5453"
        UNIQUE (workflow_run_id, role)
);

CREATE INDEX workflow_run_input_artefact_workflow_artefact_idx
    ON public.workflow_run_input_artefact (workflow_artefact_id);

CREATE INDEX workflow_run_input_workflow_run_idx
    ON public.workflow_run_input_artefact (workflow_run_id);

CREATE INDEX processing_priority_allowed_parallel_workflow_runs_idx
    ON public.processing_priority (allowed_parallel_workflow_runs);

CREATE TABLE public.abstract_field_definition
(
    id                                     bigint       NOT NULL
        CONSTRAINT "abstract_field_definitionPK"
            PRIMARY KEY,
    version                                bigint       NOT NULL,
    class                                  varchar(255) NOT NULL,
    date_created                           timestamp    NOT NULL,
    last_updated                           timestamp    NOT NULL,
    cardinality_type                       varchar(255) NOT NULL,
    name                                   varchar(255) NOT NULL
        CONSTRAINT uc_abstract_field_definitionname_col
            UNIQUE,
    description_config                     text         NOT NULL,
    description_request                    text         NOT NULL,
    field_use_for_sequencing_projects      varchar(255) NOT NULL,
    field_use_for_data_management_projects varchar(255) NOT NULL,
    source_of_data                         varchar(255) NOT NULL,
    project_display_on_config_page         varchar(255) NOT NULL,
    sort_number                            integer      NOT NULL,
    change_only_by_operator                boolean      NOT NULL,
    used_externally                        boolean      NOT NULL,
    legacy                                 boolean      NOT NULL,
    default_text_value                     text,
    default_integer_value                  integer,
    default_decimal_number_value           double precision,
    default_flag_value                     boolean,
    default_date_value                     date,
    regular_expression                     text,
    type_validator                         varchar(255),
    domain_class_name                      varchar(255),
    default_domain_reference_id            bigint,
    allow_custom_value                     boolean,
    regular_expression_error               varchar(255)
);

CREATE TABLE public.abstract_field_value
(
    id                                   bigint       NOT NULL
        CONSTRAINT "abstract_field_valuePK"
            PRIMARY KEY,
    version                              bigint       NOT NULL,
    class                                varchar(255) NOT NULL,
    date_created                         timestamp    NOT NULL,
    last_updated                         timestamp    NOT NULL,
    definition_id                        bigint       NOT NULL
        CONSTRAINT "FKiffshu69us1d969pa1l9o0xcy"
            REFERENCES public.abstract_field_definition,
    text_value                           text,
    integer_value                        integer,
    decimal_number_value                 double precision,
    date_value                           date,
    flag_value                           boolean,
    domain_id                            bigint,
    cached_text_representation_of_domain varchar(255),
    custom_value                         varchar(255)
);

CREATE INDEX abstract_field_value_definition_idx
    ON public.abstract_field_value (definition_id);

CREATE TABLE public.date_field_definition_allowed_date_values
(
    date_field_definition_id       bigint
        CONSTRAINT "FKisqi717i5p94j02phpecgkd5m2"
            REFERENCES public.abstract_field_definition,
    allowed_date_values_local_date date,
    allowed_date_values_idx        integer
);

CREATE INDEX date_field_definition_allowed_date_values_date_field_definition
    ON public.date_field_definition_allowed_date_values (date_field_definition_id);

CREATE TABLE public.decimal_number_field_definition_allowed_decimal_number_values
(
    decimal_number_field_definition_id   bigint
        CONSTRAINT "FKisqi717i5p94j02phpecgkd5m3"
            REFERENCES public.abstract_field_definition,
    allowed_decimal_number_values_double double precision,
    allowed_decimal_number_values_idx    integer
);

CREATE INDEX decimal_number_field_definition_allowed_decimal_number_values_d
    ON public.decimal_number_field_definition_allowed_decimal_number_values (decimal_number_field_definition_id);

CREATE TABLE public.integer_field_definition_allowed_integer_values
(
    integer_field_definition_id    bigint
        CONSTRAINT "FKisqi717i5p94j02phpecgkd5m4"
            REFERENCES public.abstract_field_definition,
    allowed_integer_values_integer integer,
    allowed_integer_values_idx     integer
);

CREATE INDEX integer_field_definition_allowed_integer_values_integer_field_d
    ON public.integer_field_definition_allowed_integer_values (integer_field_definition_id);

CREATE TABLE public.project_abstract_field_value
(
    project_project_fields_id bigint NOT NULL
        CONSTRAINT "FKisqi717i5p94j02phpecgkd5m"
            REFERENCES public.project,
    abstract_field_value_id   bigint
        CONSTRAINT "FK8s9rw373nrs3lc0ps8btr92im"
            REFERENCES public.abstract_field_value
);

CREATE INDEX project_abstract_field_value_project_project_fields_idx
    ON public.project_abstract_field_value (project_project_fields_id);

CREATE INDEX project_abstract_field_value_abstract_field_value_idx
    ON public.project_abstract_field_value (abstract_field_value_id);

CREATE TABLE public.set_value_field_abstract_single_field_value
(
    set_value_field_values_id      bigint NOT NULL
        CONSTRAINT "FKh7fepy5j2qfhkou1olx9b8ggv"
            REFERENCES public.abstract_field_value,
    abstract_single_field_value_id bigint
        CONSTRAINT "FK26egcjsvpgxe5ldrkh299k3oa"
            REFERENCES public.abstract_field_value
);

CREATE INDEX set_value_field_abstract_single_field_value_abstract_single_fie
    ON public.set_value_field_abstract_single_field_value (abstract_single_field_value_id);

CREATE INDEX set_value_field_abstract_single_field_value_set_value_field_val
    ON public.set_value_field_abstract_single_field_value (set_value_field_values_id);

CREATE TABLE public.text_field_definition_allowed_text_values
(
    text_field_definition_id   bigint
        CONSTRAINT "FKisqi717i5p94j02phpecgkd5m5"
            REFERENCES public.abstract_field_definition,
    allowed_text_values_string varchar(255),
    allowed_text_values_idx    integer
);

CREATE INDEX text_field_definition_allowed_text_values_text_field_definition
    ON public.text_field_definition_allowed_text_values (text_field_definition_id);

CREATE TABLE public.user_project_role_project_role
(
    user_project_role_project_roles_id bigint NOT NULL
        CONSTRAINT user_project_role_project_rol_user_project_role_project_ro_fkey
            REFERENCES public.user_project_role,
    project_role_id                    bigint NOT NULL
        REFERENCES public.project_role,
    PRIMARY KEY (user_project_role_project_roles_id, project_role_id)
);

CREATE INDEX user_project_role_project_roles_id_idx
    ON public.user_project_role_project_role (user_project_role_project_roles_id);

CREATE INDEX project_role_id_idx
    ON public.user_project_role_project_role (project_role_id);

CREATE TABLE public.workflow_job_error_definition
(
    id                     bigint       NOT NULL
        CONSTRAINT "workflow_job_error_definitionPK"
            PRIMARY KEY,
    version                bigint       NOT NULL,
    allow_restarting_count integer      NOT NULL,
    date_created           timestamp    NOT NULL,
    job_bean_name          varchar(255) NOT NULL,
    last_updated           timestamp    NOT NULL,
    bean_to_restart        varchar(255),
    source_type            varchar(255) NOT NULL,
    error_expression       text         NOT NULL,
    mail_text              text,
    name                   varchar(255) NOT NULL,
    action                 varchar(255) NOT NULL
);

CREATE INDEX workflow_job_error_definition_job_bean_name_idx
    ON public.workflow_job_error_definition (job_bean_name);

CREATE TABLE public.project_request_user
(
    id                        bigint    NOT NULL
        CONSTRAINT "project_request_userPK"
            PRIMARY KEY,
    user_id                   bigint    NOT NULL
        CONSTRAINT "FKqsmw0ti8bgqvt5q102vgk2h7c"
            REFERENCES public.users,
    access_to_files           boolean   NOT NULL,
    manage_users              boolean   NOT NULL,
    manage_users_and_delegate boolean   NOT NULL,
    version                   bigint    NOT NULL,
    date_created              timestamp NOT NULL,
    last_updated              timestamp NOT NULL,
    access_to_otp             boolean   NOT NULL
);

CREATE INDEX project_request_user_user_idx
    ON public.project_request_user (user_id);

CREATE TABLE public.project_request_user_project_role
(
    project_request_user_project_roles_id bigint NOT NULL
        CONSTRAINT "FKp311t51nd2umdgshf83nhwcdk"
            REFERENCES public.project_request_user,
    project_role_id                       bigint NOT NULL
        CONSTRAINT "FKaoqfokobihuwwl8fifq3a9syn"
            REFERENCES public.project_role
);

CREATE INDEX project_request_user_project_roles_idx
    ON public.project_request_user_project_role (project_request_user_project_roles_id);

CREATE TABLE public.data_transfer_agreement
(
    id               bigint       NOT NULL
        CONSTRAINT "data_transfer_agreementPK"
            PRIMARY KEY,
    version          bigint       NOT NULL,
    date_created     timestamp    NOT NULL,
    last_updated     timestamp    NOT NULL,
    project_id       bigint       NOT NULL
        REFERENCES public.project,
    comment          text,
    dta_id           varchar(255),
    peer_institution varchar(255) NOT NULL,
    legal_basis      varchar(255) NOT NULL,
    validity_date    date
);

CREATE TABLE public.data_transfer
(
    id                         bigint       NOT NULL
        CONSTRAINT "data_transferPK"
            PRIMARY KEY,
    version                    bigint       NOT NULL,
    transfer_mode              varchar(10)  NOT NULL,
    date_created               timestamp    NOT NULL,
    last_updated               timestamp    NOT NULL,
    completion_date            timestamp,
    direction                  varchar(16)  NOT NULL,
    comment                    text,
    ticketid                   varchar(255),
    performing_user_id         bigint       NOT NULL
        REFERENCES public.users,
    transfer_date              timestamp    NOT NULL,
    requester                  varchar(255) NOT NULL,
    peer_person                varchar(255) NOT NULL,
    peer_account               varchar(255),
    data_transfer_agreement_id bigint       NOT NULL
        REFERENCES public.data_transfer_agreement
);

CREATE INDEX data_transfer_data_transfer_agreement_idx
    ON public.data_transfer (data_transfer_agreement_id);

CREATE INDEX data_transfer_agreement_project_idx
    ON public.data_transfer_agreement (project_id);

CREATE TABLE public.data_transfer_agreement_document
(
    id                         bigint    NOT NULL
        CONSTRAINT "data_transfer_agreement_documentPK"
            PRIMARY KEY,
    version                    bigint    NOT NULL,
    date_created               timestamp NOT NULL,
    last_updated               timestamp NOT NULL,
    data_transfer_agreement_id bigint    NOT NULL
        CONSTRAINT data_transfer_agreement_document_data_transfer_agreement_id_fke
            REFERENCES public.data_transfer_agreement,
    file_name                  text      NOT NULL,
    CONSTRAINT data_transfer_agreement_document_data_transfer_agreement_id_fil
        UNIQUE (data_transfer_agreement_id, file_name)
);

CREATE INDEX data_transfer_agreement_document_data_transfer_agreement_idx
    ON public.data_transfer_agreement_document (data_transfer_agreement_id);

CREATE TABLE public.data_transfer_document
(
    id               bigint    NOT NULL
        CONSTRAINT "data_transfer_documentPK"
            PRIMARY KEY,
    version          bigint    NOT NULL,
    date_created     timestamp NOT NULL,
    last_updated     timestamp NOT NULL,
    data_transfer_id bigint    NOT NULL
        REFERENCES public.data_transfer,
    file_name        text      NOT NULL,
    CONSTRAINT data_transfer_document_data_transfer_id_file_name_unique
        UNIQUE (data_transfer_id, file_name)
);

CREATE INDEX data_transfer_document_data_transfer_idx
    ON public.data_transfer_document (data_transfer_id);

CREATE TABLE public.species_common_name
(
    id           bigint       NOT NULL
        CONSTRAINT "species_common_namePK"
            PRIMARY KEY,
    version      bigint       NOT NULL,
    date_created timestamp    NOT NULL,
    last_updated timestamp    NOT NULL,
    name         varchar(255) NOT NULL
        CONSTRAINT uc_species_common_namename_col
            UNIQUE
);

CREATE TABLE public.species
(
    id                     bigint                   NOT NULL
        PRIMARY KEY,
    version                bigint                   NOT NULL,
    scientific_name        varchar(255)             NOT NULL
        UNIQUE,
    date_created           timestamp WITH TIME ZONE NOT NULL,
    last_updated           timestamp WITH TIME ZONE NOT NULL,
    species_common_name_id bigint                   NOT NULL
        CONSTRAINT "FK43n3q0scwt2bwpdlgii02iafi"
            REFERENCES public.species_common_name
);

CREATE TABLE public.species_with_strain
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    species_id   bigint                   NOT NULL
        CONSTRAINT fk_species_with_strain_id__species
            REFERENCES public.species,
    strain_id    bigint                   NOT NULL
        CONSTRAINT fk_species_with_strain_id__strain
            REFERENCES public.strain,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL,
    legacy       boolean                  NOT NULL,
    UNIQUE (species_id, strain_id)
);

CREATE TABLE public.individual
(
    id             bigint                   NOT NULL
        PRIMARY KEY,
    version        bigint                   NOT NULL,
    mock_full_name varchar(255)             NOT NULL,
    mock_pid       varchar(255)             NOT NULL,
    pid            varchar(255)             NOT NULL
        UNIQUE,
    project_id     bigint                   NOT NULL
        CONSTRAINT fkfd3da29951483d14
            REFERENCES public.project,
    type           varchar(255)             NOT NULL,
    comment_id     bigint
        CONSTRAINT process_comment_fk
            REFERENCES public.comment
        CONSTRAINT "FK69vjbfxq2vbvgbelt1co6vgu"
            REFERENCES public.comment,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL,
    species_id     bigint
        CONSTRAINT "FK1ldcl6hj0qkgm63i0843srg0u"
            REFERENCES public.species_with_strain
);

CREATE INDEX individual_project_idx
    ON public.individual (project_id);

CREATE INDEX individual_pid_idx
    ON public.individual (pid);

CREATE INDEX individual_mock_full_name_idx
    ON public.individual (mock_full_name);

CREATE INDEX individual_mock_pid_idx
    ON public.individual (mock_pid);

CREATE INDEX individual_species_idx
    ON public.individual (species_id);

CREATE TABLE public.sample
(
    id             bigint                   NOT NULL
        PRIMARY KEY,
    version        bigint                   NOT NULL,
    individual_id  bigint                   NOT NULL
        CONSTRAINT fkc9c775aa783af6e0
            REFERENCES public.individual,
    sample_type_id bigint                   NOT NULL
        CONSTRAINT fkc9c775aac4a27e75
            REFERENCES public.sample_type,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK3a9b2b49148a72946ea7922dec50"
        UNIQUE (individual_id, sample_type_id)
);

CREATE INDEX sample_sample_type_idx
    ON public.sample (sample_type_id);

CREATE INDEX sample_individual_idx
    ON public.sample (individual_id);

CREATE TABLE public.seq_track
(
    id                         bigint                   NOT NULL
        PRIMARY KEY,
    version                    bigint                   NOT NULL,
    has_original_bam           boolean                  NOT NULL,
    insert_size                integer                  NOT NULL,
    lane_id                    varchar(255)             NOT NULL,
    n_base_pairs               bigint,
    pipeline_version_id        bigint                   NOT NULL
        CONSTRAINT fk98e90d6b27f589c4
            REFERENCES public.software_tool,
    run_id                     bigint                   NOT NULL
        CONSTRAINT fk98e90d6bb6cbbcd4
            REFERENCES public.run,
    sample_id                  bigint                   NOT NULL
        CONSTRAINT fk98e90d6b86bca440
            REFERENCES public.sample,
    seq_type_id                bigint                   NOT NULL
        CONSTRAINT fk98e90d6b5eeb3a93
            REFERENCES public.seq_type,
    fastqc_state               varchar(255)             NOT NULL,
    quality_encoding           varchar(255)             NOT NULL,
    library_preparation_kit_id bigint
        CONSTRAINT fk98e90d6baa9ddd9a
            REFERENCES public.library_preparation_kit,
    kit_info_reliability       varchar(255)             NOT NULL,
    antibody                   varchar(255),
    antibody_target_id         bigint
        CONSTRAINT fk98e90d6bdc2c3789
            REFERENCES public.antibody_target,
    linked_externally          boolean                  NOT NULL,
    library_name               varchar(255),
    normalized_library_name    varchar(255),
    data_installation_state    varchar(255)             NOT NULL,
    ilse_submission_id         bigint
        REFERENCES public.ilse_submission,
    problem                    varchar(255),
    single_cell_well_label     varchar(255),
    date_created               timestamp WITH TIME ZONE NOT NULL,
    last_updated               timestamp WITH TIME ZONE NOT NULL,
    workflow_artefact_id       bigint
        CONSTRAINT "FKt90xeu1b0thcssepl007773v8"
            REFERENCES public.workflow_artefact,
    sample_identifier          varchar(255)             NOT NULL,
    swapped                    boolean                  NOT NULL
);

CREATE INDEX seq_track_antibody_target_idx
    ON public.seq_track (antibody_target_id);

CREATE INDEX seq_track_library_preparation_kit_idx
    ON public.seq_track (library_preparation_kit_id);

CREATE INDEX seq_track_normalized_library_name_idx
    ON public.seq_track (normalized_library_name);

CREATE INDEX seq_track_run_idx
    ON public.seq_track (run_id);

CREATE INDEX seq_track_sample_idx
    ON public.seq_track (sample_id);

CREATE INDEX seq_track_seq_type_idx
    ON public.seq_track (seq_type_id);

CREATE INDEX seq_track_workflow_artefact_idx
    ON public.seq_track (workflow_artefact_id);

CREATE INDEX seq_track_sample_identifier_idx
    ON public.seq_track (sample_identifier);

CREATE INDEX seq_track_ilse_submission_idx
    ON public.seq_track (ilse_submission_id);

CREATE INDEX seq_track_lane_id_idx
    ON public.seq_track (lane_id);

CREATE INDEX seq_track_pipeline_version_idx
    ON public.seq_track (pipeline_version_id);

CREATE TABLE public.alignment_log
(
    id                  bigint                   NOT NULL
        PRIMARY KEY,
    version             bigint                   NOT NULL,
    alignment_params_id bigint                   NOT NULL
        CONSTRAINT fk8b52cae8c38ea45b
            REFERENCES public.alignment_params,
    executed_by         varchar(255)             NOT NULL,
    qc_state            varchar(255)             NOT NULL,
    seq_track_id        bigint                   NOT NULL
        CONSTRAINT fk8b52cae8f27d81e1
            REFERENCES public.seq_track,
    date_created        timestamp WITH TIME ZONE NOT NULL,
    last_updated        timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.data_file
(
    id                       bigint                   NOT NULL
        PRIMARY KEY,
    version                  bigint                   NOT NULL,
    alignment_log_id         bigint
        CONSTRAINT fkea50f8f11c055679
            REFERENCES public.alignment_log,
    date_created             timestamp                NOT NULL,
    date_executed            timestamp,
    date_file_system         timestamp,
    file_exists              boolean                  NOT NULL,
    file_linked              boolean                  NOT NULL,
    file_name                varchar(255)             NOT NULL,
    file_size                bigint                   NOT NULL,
    file_type_id             bigint                   NOT NULL
        CONSTRAINT fkea50f8f129b68c99
            REFERENCES public.file_type,
    file_withdrawn           boolean                  NOT NULL,
    md5sum                   varchar(255)             NOT NULL,
    path_name                varchar(255)             NOT NULL,
    project_id               bigint
        CONSTRAINT fkea50f8f151483d14
            REFERENCES public.project,
    run_id                   bigint                   NOT NULL
        CONSTRAINT fkea50f8f1b6cbbcd4
            REFERENCES public.run,
    fastq_import_instance_id bigint                   NOT NULL
        CONSTRAINT fkea50f8f111d99c9
            REFERENCES public.fastq_import_instance,
    seq_track_id             bigint
        CONSTRAINT fkea50f8f1f27d81e1
            REFERENCES public.seq_track,
    used                     boolean                  NOT NULL,
    vbp_file_name            varchar(255)             NOT NULL,
    mate_number              integer,
    comment_id               bigint
        CONSTRAINT data_file_comment_fk
            REFERENCES public.comment,
    n_reads                  bigint,
    sequence_length          varchar(255),
    initial_directory        text                     NOT NULL,
    date_last_checked        timestamp WITH TIME ZONE,
    last_updated             timestamp WITH TIME ZONE NOT NULL,
    index_file               boolean                  NOT NULL,
    withdrawn_comment        text,
    withdrawn_date           timestamp
);

CREATE INDEX data_file_alignment_log_idx
    ON public.data_file (alignment_log_id);

CREATE INDEX data_file_date_last_checked_idx
    ON public.data_file (date_last_checked);

CREATE INDEX data_file_file_type_idx
    ON public.data_file (file_type_id);

CREATE INDEX data_file_run_idx
    ON public.data_file (run_id);

CREATE INDEX data_file_seq_track_idx
    ON public.data_file (seq_track_id);

CREATE INDEX data_file_md5sum_idx
    ON public.data_file (md5sum);

CREATE INDEX data_file_fastq_import_instance_idx
    ON public.data_file (fastq_import_instance_id);

CREATE INDEX data_file_project_idx
    ON public.data_file (project_id);

CREATE INDEX alignment_log_alignment_param_idx
    ON public.alignment_log (alignment_params_id);

CREATE INDEX alignment_log_seq_track_idx
    ON public.alignment_log (seq_track_id);

CREATE TABLE public.cluster_job
(
    id                            bigint                   NOT NULL
        CONSTRAINT "cluster_jobPK"
            PRIMARY KEY,
    version                       bigint                   NOT NULL,
    validated                     boolean                  NOT NULL,
    processing_step_id            bigint
        CONSTRAINT fkbbfa4ff8b6502eaf
            REFERENCES public.processing_step,
    realm_id                      bigint                   NOT NULL
        REFERENCES public.realm,
    cluster_job_id                varchar(255)             NOT NULL,
    cluster_job_name              varchar(255)             NOT NULL,
    exit_status                   varchar(255),
    exit_code                     integer,
    requested_cores               integer,
    requested_memory              bigint,
    used_memory                   bigint,
    job_class                     varchar(255)             NOT NULL,
    seq_type_id                   bigint
        CONSTRAINT fkbbfa4ff85eeb3a93
            REFERENCES public.seq_type,
    used_cores                    integer,
    queued                        bigint                   NOT NULL,
    started                       bigint,
    ended                         bigint,
    cpu_time                      bigint,
    requested_walltime            bigint,
    xten                          boolean,
    file_size                     bigint,
    n_bases                       bigint,
    n_reads                       bigint,
    bases_per_bytes_fastq         real,
    user_name                     varchar(255)             NOT NULL,
    account_name                  varchar(255),
    eligible                      bigint,
    node                          varchar(255),
    start_count                   integer,
    used_swap                     integer,
    system_suspend_state_duration bigint,
    user_suspend_state_duration   bigint,
    job_log                       text,
    individual_id                 bigint
        REFERENCES public.individual,
    check_status                  varchar(255)             NOT NULL,
    date_created                  timestamp WITH TIME ZONE NOT NULL,
    last_updated                  timestamp WITH TIME ZONE NOT NULL,
    old_system                    boolean                  NOT NULL,
    workflow_step_id              bigint
        CONSTRAINT "FKb09t20osg9k84vx49cvo9ud1u"
            REFERENCES public.workflow_step
);

CREATE INDEX cluster_job_cluster_job_id_idx
    ON public.cluster_job (cluster_job_id);

CREATE INDEX cluster_job_cluster_job_name_idx
    ON public.cluster_job (cluster_job_name);

CREATE INDEX cluster_job_processing_step_idx
    ON public.cluster_job (processing_step_id);

CREATE INDEX cluster_job_workflow_step_idx
    ON public.cluster_job (workflow_step_id);

CREATE INDEX cluster_job_individual_idx
    ON public.cluster_job (individual_id);

CREATE INDEX cluster_job_old_system_check_status_idx
    ON public.cluster_job (old_system, check_status);

CREATE INDEX cluster_job_realm_idx
    ON public.cluster_job (realm_id);

CREATE TABLE public.cluster_job_cluster_job
(
    cluster_job_dependencies_id bigint NOT NULL
        CONSTRAINT fk_qgvpm8hs17r7wy71ogr1497v0
            REFERENCES public.cluster_job,
    cluster_job_id              bigint NOT NULL
        CONSTRAINT fk_st7o5bylyi9l9ebxhmqas8jdd
            REFERENCES public.cluster_job
);

CREATE INDEX cluster_job_cluster_job__cluster_job_dependencies_id__idx
    ON public.cluster_job_cluster_job (cluster_job_dependencies_id);

CREATE INDEX cluster_job_cluster_job__cluster_job_id__idx
    ON public.cluster_job_cluster_job (cluster_job_id);

CREATE TABLE public.config_per_project_and_seq_type
(
    id                      bigint                   NOT NULL
        CONSTRAINT "config_per_project_and_seq_type_PK"
            PRIMARY KEY,
    version                 bigint                   NOT NULL,
    project_id              bigint                   NOT NULL
        CONSTRAINT "project_FK"
            REFERENCES public.project,
    seq_type_id             bigint
        CONSTRAINT "seq_type_FK"
            REFERENCES public.seq_type,
    configuration           text,
    obsolete_date           timestamp WITH TIME ZONE,
    previous_config_id      bigint
        CONSTRAINT "previous_config_FK"
            REFERENCES public.config_per_project_and_seq_type,
    date_created            timestamp WITH TIME ZONE NOT NULL,
    last_updated            timestamp WITH TIME ZONE NOT NULL,
    class                   varchar(255)             NOT NULL,
    pipeline_id             bigint                   NOT NULL
        CONSTRAINT config_per_project_workflow_id_fkey
            REFERENCES public.pipeline,
    config_file_path        text
        CONSTRAINT uc_config_per_project_and_seq_typeconfig_file_path_col
            UNIQUE,
    config_version          varchar(255),
    individual_id           bigint
        CONSTRAINT fk_config_per_project__individual_id
            REFERENCES public.individual,
    adapter_trimming_needed boolean,
    name_used_in_config     varchar(255),
    program_version         varchar(255)             NOT NULL,
    md5sum                  varchar(255)
);

CREATE TABLE public.abstract_bam_file
(
    id                        bigint                   NOT NULL
        CONSTRAINT "abstract_bam_PK"
            PRIMARY KEY,
    version                   bigint                   NOT NULL,
    has_index_file            boolean                  NOT NULL,
    type                      varchar(255),
    class                     varchar(255)             NOT NULL,
    alignment_pass_id         bigint,
    date_created              timestamp WITH TIME ZONE NOT NULL,
    date_from_file_system     timestamp WITH TIME ZONE,
    file_exists               boolean,
    file_size                 bigint,
    has_coverage_plot         boolean                  NOT NULL,
    has_insert_size_plot      boolean                  NOT NULL,
    quality_assessment_status varchar(255)             NOT NULL,
    has_metrics_file          boolean                  NOT NULL,
    merging_pass_id           bigint,
    status                    varchar(255)             NOT NULL,
    withdrawn                 boolean                  NOT NULL,
    file_operation_status     varchar(255),
    md5sum                    varchar(255),
    sha256sum                 varchar(255),
    coverage                  double precision,
    number_of_merged_lanes    integer,
    coverage_withn            double precision,
    file_name                 varchar(255),
    base_bam_file_id          bigint
        REFERENCES public.abstract_bam_file,
    config_id                 bigint
        REFERENCES public.config_per_project_and_seq_type,
    work_package_id           bigint,
    identifier                integer,
    work_directory_name       varchar(255),
    imported_from             varchar(255),
    comment_id                bigint
        CONSTRAINT fk_abstract_bam_file__comment_id
            REFERENCES public.comment,
    qc_traffic_light_status   varchar(255),
    insert_size_file          varchar(1000),
    maximum_read_length       integer,
    last_updated              timestamp WITH TIME ZONE NOT NULL,
    workflow_artefact_id      bigint
        CONSTRAINT "FKqnefpv9nfcc8no602wj76ky0u"
            REFERENCES public.workflow_artefact
);
""")
    }
    changeSet(author: "otp", id: "intial-database-schema-4") {
        sql("""
CREATE INDEX abstract_bam_file_alignment_pass_idx
    ON public.abstract_bam_file (alignment_pass_id);

CREATE INDEX abstract_bam_file_base_bam_file_idx
    ON public.abstract_bam_file (base_bam_file_id);

CREATE INDEX abstract_bam_file_class_idx
    ON public.abstract_bam_file (class);

CREATE INDEX abstract_bam_file_config_idx
    ON public.abstract_bam_file (config_id);

CREATE INDEX abstract_bam_file_merging_pass_idx
    ON public.abstract_bam_file (merging_pass_id);

CREATE INDEX abstract_bam_file_qc_traffic_light_status
    ON public.abstract_bam_file (qc_traffic_light_status);

CREATE INDEX abstract_bam_file_quality_assessment_status_idx
    ON public.abstract_bam_file (quality_assessment_status);

CREATE INDEX abstract_bam_file_withdrawn_idx
    ON public.abstract_bam_file (withdrawn);

CREATE INDEX abstract_bam_file_work_package_idx
    ON public.abstract_bam_file (work_package_id);

CREATE INDEX abstract_merged_bam_file_number_of_merged_lanes_idx
    ON public.abstract_bam_file (number_of_merged_lanes);

CREATE INDEX abstract_merged_bam_file_work_package_idx
    ON public.abstract_bam_file (work_package_id);

CREATE INDEX roddy_bam_file_base_bam_file_idx
    ON public.abstract_bam_file (base_bam_file_id);

CREATE INDEX roddy_bam_file_config_idx
    ON public.abstract_bam_file (config_id);

CREATE TABLE public.abstract_merging_work_package
(
    id                            bigint                   NOT NULL
        CONSTRAINT "merging_work_PK"
            PRIMARY KEY,
    version                       bigint                   NOT NULL,
    sample_id                     bigint                   NOT NULL
        CONSTRAINT fk85e80e2286bca440
            REFERENCES public.sample,
    seq_type_id                   bigint                   NOT NULL
        CONSTRAINT fk85e80e225eeb3a93
            REFERENCES public.seq_type,
    reference_genome_id           bigint                   NOT NULL
        REFERENCES public.reference_genome,
    seq_platform_group_id         bigint
        CONSTRAINT merging_work_package_seq_platform_group_id_fkey
            REFERENCES public.seq_platform_group,
    pipeline_id                   bigint                   NOT NULL
        CONSTRAINT merging_work_package_workflow_id_fkey
            REFERENCES public.pipeline,
    needs_processing              boolean,
    stat_size_file_name           varchar(255),
    bam_file_in_project_folder_id bigint
        CONSTRAINT fk85e80e22fb20cf05
            REFERENCES public.abstract_bam_file,
    library_preparation_kit_id    bigint
        CONSTRAINT merging_work_package_library_preparation_kit
            REFERENCES public.library_preparation_kit,
    class                         varchar(255)             NOT NULL,
    antibody_target_id            bigint
        REFERENCES public.antibody_target,
    config_id                     bigint
        REFERENCES public.config_per_project_and_seq_type,
    enforced_cells                bigint,
    expected_cells                bigint,
    date_created                  timestamp WITH TIME ZONE NOT NULL,
    last_updated                  timestamp WITH TIME ZONE NOT NULL,
    reference_genome_index_id     bigint
        REFERENCES public.reference_genome_index,
    status                        varchar(255),
    informed                      timestamp WITH TIME ZONE,
    requester_id                  bigint
        CONSTRAINT abstract_merging_work_package_requester_fk
            REFERENCES public.users
);

ALTER TABLE public.abstract_bam_file
    ADD FOREIGN KEY (work_package_id) REFERENCES public.abstract_merging_work_package;

CREATE INDEX abstract_merging_work_package_antibody_target_idx
    ON public.abstract_merging_work_package (antibody_target_id);

CREATE INDEX abstract_merging_work_package_bam_file_in_project_folder_idx
    ON public.abstract_merging_work_package (bam_file_in_project_folder_id);

CREATE INDEX abstract_merging_work_package_class_idx
    ON public.abstract_merging_work_package (class);

CREATE INDEX abstract_merging_work_package_reference_genome_idx
    ON public.abstract_merging_work_package (reference_genome_id);

CREATE INDEX abstract_merging_work_package_sample_seq_type_idx
    ON public.abstract_merging_work_package (sample_id, seq_type_id);

CREATE INDEX abstract_merging_work_package_seq_type_idx
    ON public.abstract_merging_work_package (seq_type_id);

CREATE INDEX merging_work_package_needs_processing_idx
    ON public.abstract_merging_work_package (needs_processing)
    WHERE (needs_processing = TRUE);

CREATE INDEX merging_work_package_workflow_idx
    ON public.abstract_merging_work_package (pipeline_id);

CREATE INDEX abstract_merging_work_package_library_preparation_kit_idx
    ON public.abstract_merging_work_package (library_preparation_kit_id);

CREATE TABLE public.alignment_pass
(
    id              bigint                   NOT NULL
        CONSTRAINT "alignment_pasPK"
            PRIMARY KEY,
    version         bigint                   NOT NULL,
    identifier      integer                  NOT NULL,
    seq_track_id    bigint                   NOT NULL
        CONSTRAINT fkdf0830edf27d81e1
            REFERENCES public.seq_track,
    work_package_id bigint                   NOT NULL
        REFERENCES public.abstract_merging_work_package,
    alignment_state varchar(255)             NOT NULL,
    date_created    timestamp WITH TIME ZONE NOT NULL,
    last_updated    timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK9f522e63c97479850e5d9b81286f"
        UNIQUE (seq_track_id, identifier)
);

ALTER TABLE public.abstract_bam_file
    ADD CONSTRAINT fk9ae4d28a78a3e52c
        FOREIGN KEY (alignment_pass_id) REFERENCES public.alignment_pass;

CREATE INDEX alignment_pass_alignment_state_idx
    ON public.alignment_pass (alignment_state)
    WHERE ((alignment_state)::text = 'NOT_STARTED'::text);

CREATE INDEX alignment_pass_seq_track_idx
    ON public.alignment_pass (seq_track_id);

CREATE INDEX alignment_pass_work_package_idx
    ON public.alignment_pass (work_package_id);

CREATE INDEX config_per_project_class_idx
    ON public.config_per_project_and_seq_type (class);

CREATE UNIQUE INDEX config_per_project_project_id_seq_type_id_obsolete_date_idx
    ON public.config_per_project_and_seq_type (project_id, seq_type_id, obsolete_date)
    WHERE ((obsolete_date IS NULL) AND ((class)::text = 'de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig'::text));

CREATE INDEX config_per_project_project_seqtype_pipeline_individual_idx
    ON public.config_per_project_and_seq_type (project_id, seq_type_id, pipeline_id, individual_id)
    WHERE ((class)::text = 'de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig'::text);

CREATE TABLE public.externally_processed_merged_bam_file_further_files
(
    externally_processed_merged_bam_file_id bigint       NOT NULL
        CONSTRAINT externally_processed_merged_b_externally_processed_merged__fkey
            REFERENCES public.abstract_bam_file,
    further_files_string                    varchar(255) NOT NULL
);

CREATE TABLE public.fastqc_processed_file
(
    id                    bigint                   NOT NULL
        CONSTRAINT "fastqc_procesPK"
            PRIMARY KEY,
    version               bigint                   NOT NULL,
    content_uploaded      boolean                  NOT NULL,
    data_file_id          bigint                   NOT NULL
        CONSTRAINT fastqc_processed_file_data_file_id_unique
            UNIQUE
        CONSTRAINT fk9454e8be5abdecb5
            REFERENCES public.data_file,
    date_created          timestamp WITH TIME ZONE NOT NULL,
    date_from_file_system timestamp WITH TIME ZONE,
    file_exists           boolean                  NOT NULL,
    file_size             bigint                   NOT NULL,
    last_updated          timestamp WITH TIME ZONE NOT NULL,
    workflow_artefact_id  bigint
        CONSTRAINT "FKoqfd6tyh64wy6jfr7nuf4ueye"
            REFERENCES public.workflow_artefact,
    work_directory_name   varchar(255)             NOT NULL
);

CREATE INDEX fastqc_processed_file_data_file_idx
    ON public.fastqc_processed_file (data_file_id);

CREATE TABLE public.import_process_externally_processed_merged_bam_file
(
    import_process_externally_processed_merged_bam_files_id bigint NOT NULL
        CONSTRAINT import_process_externally_pro_import_process_externally_pr_fkey
            REFERENCES public.import_process,
    externally_processed_merged_bam_file_id                 bigint NOT NULL
        CONSTRAINT import_process_externally_pro_externally_processed_merged__fkey
            REFERENCES public.abstract_bam_file
);

CREATE UNIQUE INDEX import_process_id_externally_processed_merged_bam_files_id_idx
    ON public.import_process_externally_processed_merged_bam_file (import_process_externally_processed_merged_bam_files_id,
                                                                   externally_processed_merged_bam_file_id);

CREATE TABLE public.merging_set
(
    id                      bigint                   NOT NULL
        CONSTRAINT "merging_setPK"
            PRIMARY KEY,
    version                 bigint                   NOT NULL,
    identifier              integer                  NOT NULL,
    merging_work_package_id bigint                   NOT NULL
        CONSTRAINT fkc67fc18cf07d60d
            REFERENCES public.abstract_merging_work_package,
    status                  varchar(255)             NOT NULL,
    date_created            timestamp WITH TIME ZONE NOT NULL,
    last_updated            timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK4fe362d3b753414fa9b1a80b6720"
        UNIQUE (merging_work_package_id, identifier)
);

CREATE TABLE public.merging_pass
(
    id             bigint                   NOT NULL
        CONSTRAINT "merging_passPK"
            PRIMARY KEY,
    version        bigint                   NOT NULL,
    description    varchar(255),
    identifier     integer                  NOT NULL,
    merging_set_id bigint                   NOT NULL
        CONSTRAINT fk80961b1b48d3f8c4
            REFERENCES public.merging_set,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UKef39ad3edbe91ab8ebbcfc0882ec"
        UNIQUE (merging_set_id, identifier)
);

ALTER TABLE public.abstract_bam_file
    ADD CONSTRAINT fk9ae4d28a2c1e7b10
        FOREIGN KEY (merging_pass_id) REFERENCES public.merging_pass;

CREATE INDEX merging_pass_merging_set_idx
    ON public.merging_pass (merging_set_id);

CREATE INDEX merging_set_merging_work_package_idx
    ON public.merging_set (merging_work_package_id);

CREATE TABLE public.merging_set_assignment
(
    id             bigint                   NOT NULL
        CONSTRAINT "merging_set_aPK"
            PRIMARY KEY,
    version        bigint                   NOT NULL,
    bam_file_id    bigint                   NOT NULL
        CONSTRAINT fk12b3e154abebb43e
            REFERENCES public.abstract_bam_file,
    merging_set_id bigint                   NOT NULL
        CONSTRAINT fk12b3e15448d3f8c4
            REFERENCES public.merging_set,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX merging_set_assignment_bam_file_idx
    ON public.merging_set_assignment (bam_file_id);

CREATE INDEX merging_set_assignment_merging_set_idx
    ON public.merging_set_assignment (merging_set_id);

CREATE TABLE public.merging_work_package_alignment_property
(
    id                      bigint                   NOT NULL
        PRIMARY KEY,
    version                 bigint                   NOT NULL,
    merging_work_package_id bigint                   NOT NULL
        CONSTRAINT merging_work_package_alignment_pro_merging_work_package_id_fkey
            REFERENCES public.abstract_merging_work_package,
    name                    varchar(255)             NOT NULL,
    value                   varchar(500)             NOT NULL,
    date_created            timestamp WITH TIME ZONE NOT NULL,
    last_updated            timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UKf0a2c354ea7a498e07eb5d008dd5"
        UNIQUE (merging_work_package_id, name)
);

CREATE INDEX merging_work_package_alignment_property_merging_work_package_id
    ON public.merging_work_package_alignment_property (merging_work_package_id);

CREATE TABLE public.merging_work_package_seq_track
(
    merging_work_package_seq_tracks_id bigint NOT NULL
        CONSTRAINT merging_work_package_seq_trac_merging_work_package_seq_tra_fkey
            REFERENCES public.abstract_merging_work_package,
    seq_track_id                       bigint NOT NULL
        REFERENCES public.seq_track
);

CREATE UNIQUE INDEX merging_work_package_seq_track_unique_idx
    ON public.merging_work_package_seq_track (merging_work_package_seq_tracks_id, seq_track_id);

CREATE TABLE public.meta_data_entry
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    data_file_id bigint                   NOT NULL
        CONSTRAINT fkfd3c2e175abdecb5
            REFERENCES public.data_file,
    key_id       bigint                   NOT NULL
        CONSTRAINT fkfd3c2e17a1c1c485
            REFERENCES public.meta_data_key,
    value        varchar(1000)            NOT NULL,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX meta_data_entry_data_file_idx
    ON public.meta_data_entry (data_file_id);

CREATE INDEX meta_data_entry_key_idx
    ON public.meta_data_entry (key_id);

CREATE TABLE public.picard_mark_duplicates_metrics
(
    id                           bigint                   NOT NULL
        CONSTRAINT "picard_mark_dPK"
            PRIMARY KEY,
    version                      bigint                   NOT NULL,
    abstract_bam_file_id         bigint                   NOT NULL
        CONSTRAINT fka1b0bd48131bef43
            REFERENCES public.abstract_bam_file,
    estimated_library_size       bigint                   NOT NULL,
    library                      varchar(255)             NOT NULL,
    metrics_class                varchar(255)             NOT NULL,
    percent_duplication          double precision         NOT NULL,
    read_pair_duplicates         bigint                   NOT NULL,
    read_pair_optical_duplicates bigint                   NOT NULL,
    read_pairs_examined          bigint                   NOT NULL,
    unmapped_reads               bigint                   NOT NULL,
    unpaired_read_duplicates     bigint                   NOT NULL,
    unpaired_reads_examined      bigint                   NOT NULL,
    date_created                 timestamp WITH TIME ZONE NOT NULL,
    last_updated                 timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX picard_mark_duplicates_metrics_abstract_bam_file_idx
    ON public.picard_mark_duplicates_metrics (abstract_bam_file_id);

CREATE TABLE public.processed_sai_file
(
    id                    bigint                   NOT NULL
        CONSTRAINT "processed_saiPK"
            PRIMARY KEY,
    version               bigint                   NOT NULL,
    alignment_pass_id     bigint                   NOT NULL
        CONSTRAINT fkce05e13178a3e52c
            REFERENCES public.alignment_pass,
    data_file_id          bigint                   NOT NULL
        CONSTRAINT fkce05e1315abdecb5
            REFERENCES public.data_file,
    date_created          timestamp WITH TIME ZONE NOT NULL,
    date_from_file_system timestamp WITH TIME ZONE,
    file_exists           boolean                  NOT NULL,
    file_size             bigint                   NOT NULL,
    last_updated          timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX processed_sai_file_alignemt_pass_idx
    ON public.processed_sai_file (alignment_pass_id);

CREATE INDEX processed_sai_file_data_file_idx
    ON public.processed_sai_file (data_file_id);

CREATE TABLE public.quality_assessment_merged_pass
(
    id                          bigint                   NOT NULL
        CONSTRAINT "quality_assMePK"
            PRIMARY KEY,
    version                     bigint                   NOT NULL,
    description                 varchar(255),
    identifier                  integer                  NOT NULL,
    abstract_merged_bam_file_id bigint                   NOT NULL
        CONSTRAINT fke70b46871a932a30
            REFERENCES public.abstract_bam_file,
    date_created                timestamp WITH TIME ZONE NOT NULL,
    last_updated                timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UK3c476766119bb42f2680ccfbe2b1"
        UNIQUE (abstract_merged_bam_file_id, identifier)
);

CREATE INDEX quality_assessment_merged_pass_abstract_merged_bam_file_idx
    ON public.quality_assessment_merged_pass (abstract_merged_bam_file_id);

CREATE TABLE public.quality_assessment_pass
(
    id                    bigint                   NOT NULL
        CONSTRAINT "quality_assesPK"
            PRIMARY KEY,
    version               bigint                   NOT NULL,
    description           varchar(255),
    identifier            integer                  NOT NULL,
    processed_bam_file_id bigint                   NOT NULL
        CONSTRAINT fkef60082e2dead8cd
            REFERENCES public.abstract_bam_file,
    date_created          timestamp WITH TIME ZONE NOT NULL,
    last_updated          timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UKcb6a815f64f1f4ab82d06a967434"
        UNIQUE (processed_bam_file_id, identifier)
);

CREATE TABLE public.abstract_quality_assessment
(
    id                                             bigint                   NOT NULL
        CONSTRAINT "abstract_qualPK"
            PRIMARY KEY,
    version                                        bigint                   NOT NULL,
    duplicater1                                    bigint,
    duplicater2                                    bigint,
    duplicates                                     bigint,
    end_read_aberration                            bigint,
    insert_size_mean                               double precision,
    insert_size_median                             double precision,
    insert_sizerms                                 double precision,
    insert_sizesd                                  double precision,
    mapped_low_qualityr1                           bigint,
    mapped_low_qualityr2                           bigint,
    mapped_quality_longr1                          bigint,
    mapped_quality_longr2                          bigint,
    mapped_shortr1                                 bigint,
    mapped_shortr2                                 bigint,
    not_mappedr1                                   bigint,
    not_mappedr2                                   bigint,
    paired_in_sequencing                           bigint,
    paired_read1                                   bigint,
    paired_read2                                   bigint,
    proper_pair_strand_conflict                    bigint,
    properly_paired                                bigint,
    qc_bases_mapped                                bigint,
    qc_failed_reads                                bigint,
    reference_agreement                            bigint,
    reference_agreement_strand_conflict            bigint,
    reference_length                               bigint,
    singletons                                     bigint,
    total_mapped_read_counter                      bigint,
    total_read_counter                             bigint,
    with_itself_and_mate_mapped                    bigint,
    with_mate_mapped_to_different_chr              bigint,
    with_mate_mapped_to_different_chr_maq          bigint,
    class                                          varchar(255)             NOT NULL,
    chromosome_name                                varchar(255),
    percent_incorrectpeorientation                 double precision,
    percent_read_pairs_map_to_diff_chrom           double precision,
    quality_assessment_pass_id                     bigint
        CONSTRAINT fk4525553f7c88f89
            REFERENCES public.quality_assessment_pass,
    quality_assessment_merged_pass_id              bigint
        CONSTRAINT fk4525553f40bbb6dc
            REFERENCES public.quality_assessment_merged_pass,
    all_bases_mapped                               bigint,
    on_target_mapped_bases                         bigint,
    chromosome                                     varchar(255),
    genome_withoutncoverage_qc_bases               double precision,
    insert_sizecv                                  double precision,
    percentage_mates_on_different_chr              double precision,
    seq_track_id                                   bigint
        REFERENCES public.seq_track,
    library_directory_name                         varchar(255),
    threepnorm                                     double precision,
    fivepnorm                                      double precision,
    base_mismatch_rate                             double precision,
    end1percentage_sense                           double precision,
    end1mapping_rate                               double precision,
    end1mismatch_rate                              double precision,
    end1sense                                      double precision,
    end2percentage_sense                           double precision,
    end2mapping_rate                               double precision,
    end2mismatch_rate                              double precision,
    end2sense                                      double precision,
    exonic_rate                                    double precision,
    expression_profiling_efficiency                double precision,
    gap_percentage                                 double precision,
    intragenic_rate                                double precision,
    intergenic_rate                                double precision,
    intronic_rate                                  double precision,
    mapped_unique_rate_of_total                    double precision,
    mapping_rate                                   double precision,
    meancv                                         double precision,
    mean_per_base_cov                              double precision,
    unique_rateof_mapped                           double precision,
    rrnarate                                       double precision,
    duplicates_rate                                double precision,
    properly_paired_percentage                     double precision,
    total_mapped_read_counter_percentage           double precision,
    singletons_percentage                          double precision,
    alternative_alignments                         bigint,
    chimeric_pairs                                 bigint,
    cumul_gap_length                               bigint,
    end1antisense                                  bigint,
    end2antisense                                  bigint,
    estimated_library_size                         bigint,
    failed_vendorqccheck                           bigint,
    genes_detected                                 bigint,
    mapped                                         bigint,
    mapped_pairs                                   bigint,
    mapped_unique                                  bigint,
    no_covered5p                                   bigint,
    num_gaps                                       bigint,
    read_length                                    bigint,
    split_reads                                    bigint,
    total_purity_filtered_reads_sequenced          bigint,
    transcripts_detected                           bigint,
    unpaired_reads                                 bigint,
    rrnareads                                      bigint,
    mapped_read1                                   bigint,
    mapped_read2                                   bigint,
    secondary_alignments                           bigint,
    supplementary_alignments                       bigint,
    estimated_number_of_cells                      double precision,
    mean_reads_per_cell                            double precision,
    median_genes_per_cell                          double precision,
    number_of_reads                                double precision,
    valid_barcodes                                 double precision,
    reads_mapped_confidently_to_transcriptome      double precision,
    reads_mapped_confidently_to_exonic_regions     double precision,
    reads_mapped_confidently_to_intronic_regions   double precision,
    reads_mapped_confidently_to_intergenic_regions double precision,
    sequencing_saturation                          double precision,
    q30bases_in_barcode                            double precision,
    q30bases_in_rna_read                           double precision,
    q30bases_in_umi                                double precision,
    fraction_reads_in_cells                        double precision,
    total_genes_detected                           double precision,
    median_umi_counts_per_cell                     double precision,
    reads_mapped_to_genome                         double precision,
    reads_mapped_confidently_to_genome             double precision,
    reads_mapped_antisense_to_gene                 double precision,
    date_created                                   timestamp WITH TIME ZONE NOT NULL,
    last_updated                                   timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX abstract_quality_assessment_chromosome_name_idx
    ON public.abstract_quality_assessment (chromosome_name);

CREATE INDEX abstract_quality_assessment_class_idx
    ON public.abstract_quality_assessment (class);

CREATE INDEX abstract_quality_assessment_quality_assessment_merged_pass_idx
    ON public.abstract_quality_assessment (quality_assessment_merged_pass_id);

CREATE INDEX abstract_quality_assessment_quality_assessment_pass_idx
    ON public.abstract_quality_assessment (quality_assessment_pass_id);

CREATE INDEX quality_assessment_pass_processed_bam_file_idx
    ON public.quality_assessment_pass (processed_bam_file_id);

CREATE TABLE public.roddy_bam_file_roddy_execution_directory_names
(
    roddy_bam_file_id                      bigint       NOT NULL
        CONSTRAINT roddy_bam_file_roddy_execution_directory_names_string_fkey
            REFERENCES public.abstract_bam_file,
    roddy_execution_directory_names_string varchar(255) NOT NULL,
    roddy_execution_directory_names_idx    integer
);

CREATE TABLE public.roddy_bam_file_seq_track
(
    roddy_bam_file_seq_tracks_id bigint NOT NULL
        REFERENCES public.abstract_bam_file,
    seq_track_id                 bigint NOT NULL
        REFERENCES public.seq_track,
    CONSTRAINT roddy_bam_file_seq_track_roddy_bam_file_seq_tracks_id_seq_t_key
        UNIQUE (roddy_bam_file_seq_tracks_id, seq_track_id)
);

CREATE INDEX roddy_bam_file_seq_track_seq_track_id_idx
    ON public.roddy_bam_file_seq_track (seq_track_id);

CREATE TABLE public.sample_identifier
(
    id           bigint                   NOT NULL
        PRIMARY KEY,
    version      bigint                   NOT NULL,
    name         varchar(255)             NOT NULL
        UNIQUE,
    sample_id    bigint                   NOT NULL
        CONSTRAINT fk50cc3e5e86bca440
            REFERENCES public.sample,
    date_created timestamp WITH TIME ZONE NOT NULL,
    last_updated timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX sample_identifier_sample_idx
    ON public.sample_identifier (sample_id);

CREATE TABLE public.sample_pair
(
    id                          bigint                   NOT NULL
        CONSTRAINT "sample_type_combination_per_individual_PK"
            PRIMARY KEY,
    version                     bigint                   NOT NULL,
    date_created                timestamp WITH TIME ZONE NOT NULL,
    last_updated                timestamp WITH TIME ZONE NOT NULL,
    snv_processing_status       varchar(255)             NOT NULL,
    merging_work_package1_id    bigint                   NOT NULL
        REFERENCES public.abstract_merging_work_package,
    merging_work_package2_id    bigint                   NOT NULL
        REFERENCES public.abstract_merging_work_package,
    indel_processing_status     varchar(255)             NOT NULL,
    aceseq_processing_status    varchar(255)             NOT NULL,
    sophia_processing_status    varchar(255)             NOT NULL,
    run_yapsa_processing_status varchar(255)             NOT NULL,
    CONSTRAINT unique_merging_work_package2_id
        UNIQUE (merging_work_package1_id, merging_work_package2_id)
);

CREATE TABLE public.bam_file_pair_analysis
(
    id                      bigint                   NOT NULL
        CONSTRAINT "snv_calling_instance_PK"
            PRIMARY KEY,
    version                 bigint                   NOT NULL,
    config_id               bigint                   NOT NULL
        CONSTRAINT "config_FK"
            REFERENCES public.config_per_project_and_seq_type,
    processing_state        varchar(255)             NOT NULL,
    date_created            timestamp WITH TIME ZONE NOT NULL,
    last_updated            timestamp WITH TIME ZONE NOT NULL,
    instance_name           varchar(255)             NOT NULL,
    sample_type1bam_file_id bigint                   NOT NULL
        CONSTRAINT fkb9beb3f46a43c390
            REFERENCES public.abstract_bam_file,
    sample_type2bam_file_id bigint                   NOT NULL
        CONSTRAINT fkb9beb3f471f568ef
            REFERENCES public.abstract_bam_file,
    sample_pair_id          bigint                   NOT NULL
        CONSTRAINT fkb9beb3f423e26796
            REFERENCES public.sample_pair,
    class                   varchar(255)             NOT NULL,
    withdrawn               boolean                  NOT NULL,
    comment_id              bigint
        CONSTRAINT fk_bam_file_pair_analysis__comment_id
            REFERENCES public.comment,
    qc_traffic_light_status varchar(255),
    CONSTRAINT "UK5e2770429f37f8f83af98ded5741"
        UNIQUE (sample_pair_id, instance_name)
);

CREATE TABLE public.aceseq_instance_roddy_execution_directory_names
(
    aceseq_instance_id                     bigint       NOT NULL
        CONSTRAINT aceseq_instance_roddy_execution_directo_aceseq_instance_id_fkey
            REFERENCES public.bam_file_pair_analysis,
    roddy_execution_directory_names_string varchar(255) NOT NULL,
    roddy_execution_directory_names_idx    integer      NOT NULL
);

CREATE UNIQUE INDEX aceseq_instance_idx_roddy_execution_directory_names_idx
    ON public.aceseq_instance_roddy_execution_directory_names (aceseq_instance_id, roddy_execution_directory_names_idx);

CREATE TABLE public.aceseq_qc
(
    id                 bigint                   NOT NULL
        CONSTRAINT "aceseq_qc_PK"
            PRIMARY KEY,
    version            bigint                   NOT NULL,
    number             integer                  NOT NULL,
    tcc                double precision         NOT NULL,
    ploidy_factor      varchar                  NOT NULL,
    ploidy             double precision         NOT NULL,
    goodness_of_fit    double precision         NOT NULL,
    gender             varchar                  NOT NULL,
    solution_possible  integer                  NOT NULL,
    aceseq_instance_id bigint                   NOT NULL
        CONSTRAINT "aceseq_instance_FK"
            REFERENCES public.bam_file_pair_analysis,
    date_created       timestamp WITH TIME ZONE NOT NULL,
    last_updated       timestamp WITH TIME ZONE NOT NULL,
    CONSTRAINT "UKa1bacb088b69505ab70013e79694"
        UNIQUE (aceseq_instance_id, number)
);

CREATE INDEX aceseq_qc_aceseq_instance_idx
    ON public.aceseq_qc (aceseq_instance_id);

CREATE INDEX bam_file_pair_analysis_qc_traffic_light_status_idx
    ON public.bam_file_pair_analysis (qc_traffic_light_status);

CREATE INDEX bam_file_pair_analysis_sample_pair_idx
    ON public.bam_file_pair_analysis (sample_pair_id);

CREATE INDEX bam_file_pair_analysis_sample_type_1_bam_file_idx
    ON public.bam_file_pair_analysis (sample_type1bam_file_id);

CREATE INDEX bam_file_pair_analysis_sample_type_2_bam_file_idx
    ON public.bam_file_pair_analysis (sample_type2bam_file_id);

CREATE INDEX bam_file_pair_analysis_config_idx
    ON public.bam_file_pair_analysis (config_id);

CREATE TABLE public.indel_calling_instance_roddy_execution_directory_names
(
    indel_calling_instance_id              bigint       NOT NULL
        CONSTRAINT indel_calling_instance_roddy_exe_indel_calling_instance_id_fkey
            REFERENCES public.bam_file_pair_analysis,
    roddy_execution_directory_names_string varchar(255) NOT NULL,
    roddy_execution_directory_names_idx    integer      NOT NULL
);

CREATE UNIQUE INDEX indel_calling_instance_idx_roddy_execution_directory_names_idx
    ON public.indel_calling_instance_roddy_execution_directory_names (indel_calling_instance_id, roddy_execution_directory_names_idx);

CREATE TABLE public.indel_quality_control
(
    id                        bigint                   NOT NULL
        PRIMARY KEY,
    version                   bigint                   NOT NULL,
    indel_calling_instance_id bigint                   NOT NULL
        CONSTRAINT uc_indel_quality_controlindel_calling_instance_id_col
            UNIQUE
        REFERENCES public.bam_file_pair_analysis,
    file                      text                     NOT NULL,
    num_indels                integer                  NOT NULL,
    num_ins                   integer                  NOT NULL,
    num_dels                  integer                  NOT NULL,
    num_size1_3               integer                  NOT NULL,
    num_size4_10              integer                  NOT NULL,
    num_size11plus            integer                  NOT NULL,
    num_ins_size1_3           integer                  NOT NULL,
    num_ins_size4_10          integer                  NOT NULL,
    num_ins_size11plus        integer                  NOT NULL,
    num_dels_size1_3          integer                  NOT NULL,
    num_dels_size4_10         integer                  NOT NULL,
    num_dels_size11plus       integer                  NOT NULL,
    percent_ins               double precision         NOT NULL,
    percent_dels              double precision         NOT NULL,
    percent_size1_3           double precision         NOT NULL,
    percent_size4_10          double precision         NOT NULL,
    percent_size11plus        double precision         NOT NULL,
    percent_ins_size1_3       double precision         NOT NULL,
    percent_ins_size4_10      double precision         NOT NULL,
    percent_ins_size11plus    double precision         NOT NULL,
    percent_dels_size1_3      double precision         NOT NULL,
    percent_dels_size4_10     double precision         NOT NULL,
    percent_dels_size11plus   double precision         NOT NULL,
    date_created              timestamp WITH TIME ZONE NOT NULL,
    last_updated              timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX unique_indel_quality_control_indel_calling_instance_id_idx
    ON public.indel_quality_control (indel_calling_instance_id);

CREATE TABLE public.indel_sample_swap_detection
(
    id                                                       bigint                   NOT NULL
        CONSTRAINT indel_sample_swap_pkey
            PRIMARY KEY,
    version                                                  bigint                   NOT NULL,
    germlinesnvs_heterozygous_in_both                        integer                  NOT NULL,
    germlinesnvs_heterozygous_in_both_rare                   integer                  NOT NULL,
    germline_small_vars_heterozygous_in_both_rare            integer                  NOT NULL,
    indel_calling_instance_id                                bigint                   NOT NULL
        CONSTRAINT uc_indel_sample_swap_detectionindel_calling_instance_id_col
            UNIQUE
        CONSTRAINT fk_osgyi47q5xjh1kkmvot8tiv4u
            REFERENCES public.bam_file_pair_analysis,
    pid                                                      varchar(255)             NOT NULL,
    somatic_small_vars_in_control                            integer                  NOT NULL,
    somatic_small_vars_in_control_common_in_gnomad           integer                  NOT NULL,
    somatic_small_vars_in_control_common_in_gnomad_per       integer                  NOT NULL,
    somatic_small_vars_in_control_common_in_gnomas_per       integer                  NOT NULL,
    somatic_small_vars_in_control_in_bias                    integer                  NOT NULL,
    somatic_small_vars_in_control_in_bias_per                integer                  NOT NULL,
    somatic_small_vars_in_control_pass                       integer                  NOT NULL,
    somatic_small_vars_in_control_pass_per                   integer                  NOT NULL,
    somatic_small_vars_in_tumor                              integer                  NOT NULL,
    somatic_small_vars_in_tumor_common_in_gnomadper          integer                  NOT NULL,
    somatic_small_vars_in_tumor_common_in_gnomad             integer                  NOT NULL,
    somatic_small_vars_in_tumor_common_in_gnomad_per         integer                  NOT NULL,
    somatic_small_vars_in_tumor_in_bias                      integer                  NOT NULL,
    somatic_small_vars_in_tumor_in_bias_per                  double precision         NOT NULL,
    somatic_small_vars_in_tumor_pass                         integer                  NOT NULL,
    somatic_small_vars_in_tumor_pass_per                     double precision         NOT NULL,
    tinda_germline_rare_after_rescue                         integer                  NOT NULL,
    tinda_somatic_after_rescue                               integer                  NOT NULL,
    tinda_somatic_after_rescue_median_allele_freq_in_control double precision         NOT NULL,
    date_created                                             timestamp WITH TIME ZONE NOT NULL,
    last_updated                                             timestamp WITH TIME ZONE NOT NULL,
    germline_small_vars_in_both_rare                         integer
);

CREATE TABLE public.roddy_snv_calling_instance_roddy_execution_directory_names
(
    roddy_snv_calling_instance_id          bigint       NOT NULL
        CONSTRAINT roddy_snv_calling_instance_ro_roddy_snv_calling_instance_i_fkey
            REFERENCES public.bam_file_pair_analysis,
    roddy_execution_directory_names_string varchar(255) NOT NULL,
    roddy_execution_directory_names_idx    integer      NOT NULL
);

CREATE INDEX sample_pair_aceseq_idx1
    ON public.sample_pair (aceseq_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_pair_indel_idx1
    ON public.sample_pair (indel_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_pair_snv_idx1
    ON public.sample_pair (snv_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_pair_sophia_idx1
    ON public.sample_pair (sophia_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_pair_runyapsa_idx1
    ON public.sample_pair (run_yapsa_processing_status, merging_work_package1_id, merging_work_package2_id);

CREATE INDEX sample_pair_merging_work_package_1_idx
    ON public.sample_pair (merging_work_package1_id);

CREATE INDEX sample_pair_merging_work_package_2_idx
    ON public.sample_pair (merging_work_package2_id);

CREATE TABLE public.seq_track_log_message
(
    seq_track_log_messages_id bigint  NOT NULL
        REFERENCES public.seq_track,
    log_message_id            bigint  NOT NULL
        REFERENCES public.log_message,
    log_messages_idx          integer NOT NULL,
    PRIMARY KEY (seq_track_log_messages_id, log_message_id)
);

CREATE TABLE public.sophia_instance_roddy_execution_directory_names
(
    sophia_instance_id                     bigint       NOT NULL
        CONSTRAINT sophia_instance_roddy_execution_directo_sophia_instance_id_fkey
            REFERENCES public.bam_file_pair_analysis,
    roddy_execution_directory_names_string varchar(255) NOT NULL,
    roddy_execution_directory_names_idx    integer      NOT NULL
);

CREATE UNIQUE INDEX sophia_instance_idx_roddy_execution_directory_names_idx
    ON public.sophia_instance_roddy_execution_directory_names (sophia_instance_id, roddy_execution_directory_names_idx);

CREATE TABLE public.sophia_qc
(
    id                                          bigint                   NOT NULL
        CONSTRAINT "sophia_qc_PK"
            PRIMARY KEY,
    version                                     bigint                   NOT NULL,
    control_massive_inv_prefiltering_level      integer                  NOT NULL,
    tumor_massive_inv_filtering_level           integer                  NOT NULL,
    rna_contaminated_genes_more_than_two_intron varchar                  NOT NULL,
    rna_contaminated_genes_count                integer                  NOT NULL,
    rna_decontamination_applied                 boolean                  NOT NULL,
    sophia_instance_id                          bigint                   NOT NULL
        CONSTRAINT uc_sophia_qcsophia_instance_id_col
            UNIQUE
        CONSTRAINT "sophia_instance_FK"
            REFERENCES public.bam_file_pair_analysis,
    date_created                                timestamp WITH TIME ZONE NOT NULL,
    last_updated                                timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.single_cell_bam_file_seq_track
(
    single_cell_bam_file_seq_tracks_id bigint NOT NULL
        CONSTRAINT single_cell_bam_file_seq_track_scbfst_id_fkey
            REFERENCES public.abstract_bam_file,
    seq_track_id                       bigint NOT NULL
        REFERENCES public.seq_track
);

CREATE UNIQUE INDEX single_cell_bam_file_seq_track_scbfst_id_unqiue_idx
    ON public.single_cell_bam_file_seq_track (single_cell_bam_file_seq_tracks_id, seq_track_id);

CREATE INDEX single_cell_bam_file_seq_track_seq_track_id_idx
    ON public.single_cell_bam_file_seq_track (seq_track_id);

CREATE TABLE public.sample_submission_object
(
    id             bigint                   NOT NULL
        PRIMARY KEY,
    version        bigint                   NOT NULL,
    sample_id      bigint                   NOT NULL
        REFERENCES public.sample,
    ega_alias_name varchar(255)
        CONSTRAINT ega_alias_name_key
            UNIQUE,
    seq_type_id    bigint                   NOT NULL
        REFERENCES public.seq_type,
    use_bam_file   boolean                  NOT NULL,
    use_fastq_file boolean                  NOT NULL,
    date_created   timestamp WITH TIME ZONE NOT NULL,
    last_updated   timestamp WITH TIME ZONE NOT NULL
);

CREATE TABLE public.bam_file_submission_object
(
    id                          bigint                   NOT NULL
        PRIMARY KEY,
    version                     bigint                   NOT NULL,
    bam_file_id                 bigint                   NOT NULL
        REFERENCES public.abstract_bam_file,
    ega_alias_name              varchar(255)
        CONSTRAINT bam_file_ega_alias_name_key
            UNIQUE,
    sample_submission_object_id bigint                   NOT NULL
        REFERENCES public.sample_submission_object,
    date_created                timestamp WITH TIME ZONE NOT NULL,
    last_updated                timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX bam_file_submission_object_bam_file_idx
    ON public.bam_file_submission_object (bam_file_id);

CREATE INDEX bam_file_submission_object_sample_submission_object_idx
    ON public.bam_file_submission_object (sample_submission_object_id);

CREATE TABLE public.data_file_submission_object
(
    id                          bigint                   NOT NULL
        PRIMARY KEY,
    version                     bigint                   NOT NULL,
    data_file_id                bigint                   NOT NULL
        REFERENCES public.data_file,
    ega_alias_name              varchar(255)
        CONSTRAINT data_file_ega_alias_name_key
            UNIQUE,
    sample_submission_object_id bigint                   NOT NULL
        REFERENCES public.sample_submission_object,
    date_created                timestamp WITH TIME ZONE NOT NULL,
    last_updated                timestamp WITH TIME ZONE NOT NULL
);

CREATE INDEX data_file_submission_object_data_file_idx
    ON public.data_file_submission_object (data_file_id);

CREATE INDEX data_file_submission_object_sample_submission_object_idx
    ON public.data_file_submission_object (sample_submission_object_id);

CREATE INDEX sample_submission_sample_idx
    ON public.sample_submission_object (sample_id);

CREATE INDEX sample_submission_seq_type_idx
    ON public.sample_submission_object (seq_type_id);

CREATE TABLE public.ega_submission_bam_file_submission_object
(
    ega_submission_bam_files_to_submit_id bigint NOT NULL
        CONSTRAINT submission_bam_file_submissio_submission_bam_files_to_subm_fkey
            REFERENCES public.ega_submission,
    bam_file_submission_object_id         bigint NOT NULL
        CONSTRAINT submission_bam_file_submissio_bam_file_submission_object_i_fkey
            REFERENCES public.bam_file_submission_object,
    CONSTRAINT submission_bam_file_submission_object_pkey
        PRIMARY KEY (ega_submission_bam_files_to_submit_id, bam_file_submission_object_id)
);

CREATE INDEX ega_submission_bam_file_submission_object_ega_submission_bam_fi
    ON public.ega_submission_bam_file_submission_object (ega_submission_bam_files_to_submit_id);

CREATE INDEX ega_submission_bam_file_submission_object_bam_file_submission_o
    ON public.ega_submission_bam_file_submission_object (bam_file_submission_object_id);

CREATE TABLE public.ega_submission_data_file_submission_object
(
    ega_submission_data_files_to_submit_id bigint NOT NULL
        CONSTRAINT submission_data_file_submissi_submission_data_files_to_sub_fkey
            REFERENCES public.ega_submission,
    data_file_submission_object_id         bigint NOT NULL
        CONSTRAINT submission_data_file_submissi_data_file_submission_object__fkey
            REFERENCES public.data_file_submission_object,
    CONSTRAINT submission_data_file_submission_object_pkey
        PRIMARY KEY (ega_submission_data_files_to_submit_id, data_file_submission_object_id)
);

CREATE INDEX ega_submission_data_file_submission_object_ega_submission_data_
    ON public.ega_submission_data_file_submission_object (ega_submission_data_files_to_submit_id);

CREATE INDEX ega_submission_data_file_submission_object_data_file_submission
    ON public.ega_submission_data_file_submission_object (data_file_submission_object_id);

CREATE TABLE public.ega_submission_sample_submission_object
(
    ega_submission_samples_to_submit_id bigint NOT NULL
        CONSTRAINT submission_sample_submission__submission_samples_to_submit_fkey
            REFERENCES public.ega_submission,
    sample_submission_object_id         bigint NOT NULL
        CONSTRAINT submission_sample_submission_o_sample_submission_object_id_fkey
            REFERENCES public.sample_submission_object,
    CONSTRAINT submission_sample_submission_object_pkey
        PRIMARY KEY (ega_submission_samples_to_submit_id, sample_submission_object_id)
);

CREATE INDEX ega_submission_sample_submission_object_ega_submission_samples_
    ON public.ega_submission_sample_submission_object (ega_submission_samples_to_submit_id);

CREATE INDEX ega_submission_sample_submission_object_sample_submission_objec
    ON public.ega_submission_sample_submission_object (sample_submission_object_id);

CREATE TABLE public.project_species_with_strain
(
    project_species_with_strains_id bigint NOT NULL
        CONSTRAINT "FKai6vvaxbkge30u9ca5xwbd4uj"
            REFERENCES public.project,
    species_with_strain_id          bigint
        CONSTRAINT "FK5yay274o3ruc5mvx6ylit8hop"
            REFERENCES public.species_with_strain
);

CREATE INDEX project_species_with_strain_project_species_with_strains_id_idx
    ON public.project_species_with_strain (project_species_with_strains_id);

CREATE INDEX project_species_with_strain_species_with_strain_id_idx
    ON public.project_species_with_strain (species_with_strain_id);

CREATE TABLE public.reference_genome_species_with_strain
(
    reference_genome_species_with_strain_id bigint NOT NULL
        CONSTRAINT "FKjqycwyqpntsq2hr69w7ugq1tg"
            REFERENCES public.reference_genome,
    species_with_strain_id                  bigint
        CONSTRAINT "FKnh6r0yntwr2v2xqqoqpgc3uig"
            REFERENCES public.species_with_strain
);
""")
    }
    changeSet(author: "otp", id: "intial-database-schema-5") {
        sql("""
CREATE INDEX reference_genome_species_with_strain_reference_genome_species_i
    ON public.reference_genome_species_with_strain (reference_genome_species_with_strain_id);

CREATE INDEX reference_genome_species_with_strain_species_with_strain_id_idx
    ON public.reference_genome_species_with_strain (species_with_strain_id);

CREATE TABLE public.sample_species_with_strain
(
    sample_mixed_in_species_id bigint NOT NULL
        CONSTRAINT "FKjofb5pxjfkcl2cvbutgn9a13e"
            REFERENCES public.sample,
    species_with_strain_id     bigint
        CONSTRAINT "FKodg8l0hs3mtu1ivpdq56ec45k"
            REFERENCES public.species_with_strain
);

CREATE INDEX sample_species_with_strain_sample_mixed_in_species_id_idx
    ON public.sample_species_with_strain (sample_mixed_in_species_id);

CREATE INDEX sample_species_with_strain_species_with_strain_id_idx
    ON public.sample_species_with_strain (species_with_strain_id);

CREATE TABLE public.species_with_strain_import_alias
(
    species_with_strain_id bigint NOT NULL
        CONSTRAINT "FKhplsh5fhlj4pnlqqig85hdndg"
            REFERENCES public.species_with_strain,
    import_alias_string    varchar(255)
);

CREATE TABLE public.project_request_persistent_state
(
    id               bigint       NOT NULL
        CONSTRAINT "project_request_persistent_statePK"
            PRIMARY KEY,
    version          bigint       NOT NULL,
    date_created     timestamp    NOT NULL,
    last_updated     timestamp    NOT NULL,
    bean_name        varchar(255) NOT NULL,
    current_owner_id bigint
        CONSTRAINT "FKfh8tyaq15y0e6ogvybg98a7ef"
            REFERENCES public.users
);

CREATE TABLE public.project_request
(
    id                   bigint       NOT NULL
        CONSTRAINT "project_requestPK"
            PRIMARY KEY,
    version              bigint       NOT NULL,
    date_created         timestamp    NOT NULL,
    storage_until        date,
    last_updated         timestamp    NOT NULL,
    end_date             date,
    requester_comment    text,
    project_type         varchar(255) NOT NULL,
    approx_no_of_samples integer,
    tumor_entity_id      bigint
        CONSTRAINT "FK36jpufmnlu5j8082lchis2rha"
            REFERENCES public.tumor_entity,
    name                 varchar(255) NOT NULL
        CONSTRAINT uc_project_requestname_col
            UNIQUE,
    requester_id         bigint       NOT NULL
        CONSTRAINT "FKnxky2b0avxjdej401wdmhyvt7"
            REFERENCES public.users,
    description          text         NOT NULL,
    project_id           bigint
        CONSTRAINT "FKhkhfpuxdw9l8j2oyo4nqspiy0"
            REFERENCES public.project,
    related_projects     varchar(255),
    state_id             bigint       NOT NULL
        CONSTRAINT "FK4h8bj6lbdujxmd9fids6g5yk4"
            REFERENCES public.project_request_persistent_state
);

CREATE INDEX project_request_requester_idx
    ON public.project_request (requester_id);

CREATE INDEX project_request_state_idx
    ON public.project_request (state_id);

CREATE TABLE public.project_request_keywords
(
    project_request_id bigint       NOT NULL
        CONSTRAINT "FKiy5k8aohe7a6qylnaejqiu5s9"
            REFERENCES public.project_request,
    keywords_string    varchar(255) NOT NULL
);

CREATE INDEX project_request_keywords_idx
    ON public.project_request_keywords (project_request_id);

CREATE INDEX project_request_keywords_project_request_id_idx
    ON public.project_request_keywords (project_request_id);

CREATE TABLE public.project_request_seq_type
(
    project_request_seq_types_id bigint NOT NULL
        CONSTRAINT "FK8mj8ac9ajfajmghv6aba5e69w"
            REFERENCES public.project_request,
    seq_type_id                  bigint NOT NULL
        CONSTRAINT "FK8s9kbr0ptwhtx2go7myalfgbg"
            REFERENCES public.seq_type
);

CREATE INDEX "project_request_seqTypes_idx"
    ON public.project_request_seq_type (project_request_seq_types_id);

CREATE INDEX project_request_seq_type_seq_type_id_idx
    ON public.project_request_seq_type (seq_type_id);

CREATE TABLE public.project_request_abstract_field_value
(
    project_request_project_fields_id bigint NOT NULL
        CONSTRAINT "FK9vqbddv6ksvwaqr8q36becdwh"
            REFERENCES public.project_request,
    abstract_field_value_id           bigint
        CONSTRAINT "FK8wqg2n8t1ua7xq22i3idobwxw"
            REFERENCES public.abstract_field_value
);

CREATE INDEX project_request_abstract_field_value_project_request_project_fi
    ON public.project_request_abstract_field_value (project_request_project_fields_id);

CREATE INDEX project_request_abstract_field_value_abstract_field_value_idx
    ON public.project_request_abstract_field_value (abstract_field_value_id);

CREATE TABLE public.project_request_project_request_user
(
    project_request_users_id bigint NOT NULL
        CONSTRAINT "FKp6pmp49c8ksnty0kvx7aywdo"
            REFERENCES public.project_request,
    project_request_user_id  bigint NOT NULL
        CONSTRAINT "FK494vryt8ugycu0d7ceryaqo5w"
            REFERENCES public.project_request_user
);

CREATE INDEX project_request_users_idx
    ON public.project_request_project_request_user (project_request_users_id);

CREATE INDEX project_request_project_request_user_project_request_user_id_id
    ON public.project_request_project_request_user (project_request_user_id);

CREATE TABLE public.project_request_species_with_strain
(
    project_request_species_with_strains_id bigint NOT NULL
        CONSTRAINT "FKbvi6kwe8sw97n826q7ls83kxm"
            REFERENCES public.project_request,
    species_with_strain_id                  bigint
        CONSTRAINT "FKqocijfvnd3ccgmp5y1h4pycp"
            REFERENCES public.species_with_strain
);

CREATE INDEX project_request_species_with_strain_project_request_species_wit
    ON public.project_request_species_with_strain (project_request_species_with_strains_id);

CREATE INDEX project_request_species_with_strain_species_with_strain_id_idx
    ON public.project_request_species_with_strain (species_with_strain_id);

CREATE INDEX project_request_persistent_state_current_owner_idx
    ON public.project_request_persistent_state (current_owner_id);

CREATE TABLE public.project_request_persistent_state_users_that_already_approved
(
    project_request_persistent_state_id bigint NOT NULL
        CONSTRAINT "FK2dp694rj6xruniv4l89bjmioh"
            REFERENCES public.project_request_persistent_state,
    user_id                             bigint
        CONSTRAINT "FK2r8ylaj7g9er3cgb9qy3dfl3k"
            REFERENCES public.users
);

CREATE INDEX project_request_persistent_state_users_that_already_approved_pr
    ON public.project_request_persistent_state_users_that_already_approved (project_request_persistent_state_id);

CREATE INDEX project_request_persistent_state_users_that_already_approved_us
    ON public.project_request_persistent_state_users_that_already_approved (user_id);

CREATE TABLE public.project_request_persistent_state_users_that_need_to_approve
(
    project_request_persistent_state_id bigint NOT NULL
        CONSTRAINT "FKe7l1y8pxcv20b8i597jntfsqt"
            REFERENCES public.project_request_persistent_state,
    user_id                             bigint
        CONSTRAINT "FKa5um74gr5jox4n1wyq5ejvrfn"
            REFERENCES public.users
);

CREATE INDEX project_request_persistent_state_users_that_need_to_approve_pro
    ON public.project_request_persistent_state_users_that_need_to_approve (project_request_persistent_state_id);

CREATE INDEX project_request_persistent_state_users_that_need_to_approve_use
    ON public.project_request_persistent_state_users_that_need_to_approve (user_id);

CREATE TABLE public.project_request_custom_species_with_strains
(
    project_request_id                 bigint NOT NULL
        CONSTRAINT "FKlqt24pl8r3387kelyrolg7r"
            REFERENCES public.project_request,
    custom_species_with_strains_string varchar(255)
);

CREATE INDEX project_request_custom_species_with_strains_project_request_id_
    ON public.project_request_custom_species_with_strains (project_request_id);

CREATE TABLE public.project_request_comment
(
    project_request_comments_id bigint NOT NULL,
    comment_id                  bigint
        CONSTRAINT "FKeirwxy4mlvsodx1717gsgtepp"
            REFERENCES public.comment,
    comments_idx                integer
);

CREATE TABLE public.project_request_custom_sequencing_centers
(
    project_request_id               bigint NOT NULL
        CONSTRAINT "FKbjg27pi2nchyepj8emqtqgpb7"
            REFERENCES public.project_request,
    custom_sequencing_centers_string varchar(255)
);

CREATE INDEX project_request_custom_sequencing_centers_project_request_id_id
    ON public.project_request_custom_sequencing_centers (project_request_id);

CREATE TABLE public.project_request_seq_center
(
    project_request_sequencing_centers_id bigint NOT NULL
        CONSTRAINT "FKnm65euhaauog4ip6tjauhqbsp"
            REFERENCES public.project_request,
    seq_center_id                         bigint
        CONSTRAINT "FKm2y52tjoby8xxtp9qykcrhbr3"
            REFERENCES public.seq_center
);

CREATE INDEX project_request_seq_center_project_request_sequencing_centers_i
    ON public.project_request_seq_center (project_request_sequencing_centers_id);

CREATE INDEX project_request_seq_center_seq_center_id_idx
    ON public.project_request_seq_center (seq_center_id);

CREATE TABLE public.project_request_custom_seq_types
(
    project_request_id      bigint NOT NULL
        CONSTRAINT "FKofffbb2qlnx4ytkmaejbkuyqk"
            REFERENCES public.project_request,
    custom_seq_types_string varchar(255)
);

CREATE INDEX project_request_custom_seq_types_project_request_id_idx
    ON public.project_request_custom_seq_types (project_request_id);

CREATE TABLE public.reference_genome_selector_species_with_strain
(
    reference_genome_selector_species_id bigint NOT NULL
        CONSTRAINT "FKmuasj0s0haworrxl19djdrrm9"
            REFERENCES public.reference_genome_selector,
    species_with_strain_id               bigint
        CONSTRAINT "FKdjtptiihx8m3ffjrcjkd3oct4"
            REFERENCES public.species_with_strain
);

CREATE INDEX reference_genome_selector_species_with_strain_reference_genome_
    ON public.reference_genome_selector_species_with_strain (reference_genome_selector_species_id);

CREATE INDEX reference_genome_selector_species_with_strain_species_with_stra
    ON public.reference_genome_selector_species_with_strain (species_with_strain_id);

CREATE TABLE public.workflow_version_selector
(
    id                  bigint    NOT NULL
        CONSTRAINT "workflow_version_selectorPK"
            PRIMARY KEY,
    version             bigint    NOT NULL,
    object_version      integer   NOT NULL,
    workflow_version_id bigint    NOT NULL
        CONSTRAINT "FKqbd60e12qp5r2p8ugxd1b9pkf"
            REFERENCES public.workflow_version,
    date_created        timestamp NOT NULL,
    previous_id         bigint
        CONSTRAINT "FKsnrrn3ni5l1bnb2i1tifs1ud0"
            REFERENCES public.workflow_version_selector,
    last_updated        timestamp NOT NULL,
    deprecation_date    date,
    seq_type_id         bigint
        CONSTRAINT "FKm751s3l7h0a8hkqr9d3lhkdtm"
            REFERENCES public.seq_type,
    project_id          bigint    NOT NULL
        CONSTRAINT "FK9em5y65pma55fbmctwppw37e6"
            REFERENCES public.project
);

CREATE INDEX workflow_version_selector_previous_idx
    ON public.workflow_version_selector (previous_id);

CREATE INDEX workflow_version_selector_project_idx
    ON public.workflow_version_selector (project_id);

CREATE INDEX workflow_version_selector_seq_type_idx
    ON public.workflow_version_selector (seq_type_id);

CREATE INDEX workflow_version_selector_workflow_version_idx
    ON public.workflow_version_selector (workflow_version_id);

CREATE TABLE public.reference_genome_species
(
    reference_genome_species_id bigint NOT NULL
        CONSTRAINT "FKs3jmeh2c4y3vp0slw10afnbds"
            REFERENCES public.reference_genome,
    species_id                  bigint
        CONSTRAINT "FKcos3fbb27hx4r5p0j185gqwu9"
            REFERENCES public.species
);
""")
    }
}
