package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class ProcessingOption implements Entity {

    enum OptionName {
        //pipeline
        PIPELINE_MIN_COVERAGE(
                "Minimum coverage to start a pipeline",
                Necessity.OPTIONAL, "0.0", TypeValidators.FLOAT,
                TypeValidators.PIPELINE_TYPE
        ),

        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME(
                "Default name of the Roddy SNV pipeline plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "SNVCallingWorkflow", TypeValidators.SINGLE_WORD_TEXT,
        ),
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION(
                "Default version of the Roddy SNV pipeline plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Roddy SNV pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),

        PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_NAME(
                "Default name of the Indel pipeline plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "IndelCallingWorkflow", TypeValidators.SINGLE_WORD_TEXT,
        ),
        PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_VERSION(
                "Default version of the Indel pipeline plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_INDEL_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Indel pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),

        PIPELINE_ACESEQ_DEFAULT_PLUGIN_NAME(
                "Default name of the Aceseq pipeline plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "ACEseqWorkflow", TypeValidators.SINGLE_WORD_TEXT,
        ),
        PIPELINE_ACESEQ_DEFAULT_PLUGIN_VERSION(
                "Default version of the Aceseq pipeline plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_ACESEQ_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Aceseq pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_ACESEQ_REFERENCE_GENOME(
                "Name of reference genomes for Aceseq, comma-separated",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),

        PIPELINE_RUNYAPSA_DEFAULT_VERSION(
                "Default runYapsa version, used when configuring the pipeline",
                Necessity.OPTIONAL, "yapsa-devel/80f748e", TypeValidators.SINGLE_LINE_TEXT
        ),
        PIPELINE_RUNYAPSA_AVAILABLE_VERSIONS(
                "runYapsa versions, comma-separated",
                Necessity.OPTIONAL, "yapsa-devel/80f748e", TypeValidators.SINGLE_LINE_TEXT
        ),
        PIPELINE_RUNYAPSA_REFERENCE_GENOME(
                "Name of reference genomes for runYapsa, comma-separated",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),

        PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME(
                "Default name of the Sophia pipeline plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "SophiaWorkflow", TypeValidators.SINGLE_WORD_TEXT,
        ),
        PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS(
                "Default version of the Sophia pipeline plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Sophia pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_SOPHIA_REFERENCE_GENOME(
                "Name of reference genomes for Sophia, comma-separated",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),

        PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME(
                "Default name of the alignment plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "AlignmentAndQCWorkflows", TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION(
                "Default version of the roddy alignment plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Roddy alignment, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME(
                "Default reference genome, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL(
                "Default merge tool, used when configuring the pipeline",
                Necessity.OPTIONAL, "sambamba", TypeValidators.SINGLE_LINE_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS(
                "All merge tools, used when configuring the pipeline",
                Necessity.OPTIONAL, "picard,biobambam,sambamba", TypeValidators.SINGLE_LINE_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT(
                "Default version for alignment with bwa_mem, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.7.15", TypeValidators.SINGLE_LINE_TEXT
        ),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE(
                "Available versions for alignment with bwa_mem, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.7.8, 0.7.15, 0.7.17", TypeValidators.SINGLE_LINE_TEXT
        ),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT(
                "Default version for merging and duplication marking with sambamba, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.6.5", TypeValidators.SINGLE_LINE_TEXT
        ),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE(
                "Available versions for merging and duplication marking with sambamba, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.5.9, 0.6.5", TypeValidators.SINGLE_LINE_TEXT
        ),
        @Deprecated
        PIPELINE_RODDY_ALIGNMENT_BWA_PATHS(
                "Path to bwa_mem",
                Necessity.OPTIONAL, "", TypeValidators.ABSOLUTE_PATH
        ),
        @Deprecated
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_PATHS(
                "Path to sambamba",
                Necessity.OPTIONAL, "", TypeValidators.ABSOLUTE_PATH
        ),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_GENOME_STAR_INDEX(
                "Default genome star index, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),

        @Deprecated
        PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT(
                "Quality assessment Command and parameters template",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT
        ),

        @Deprecated
        COMMAND_BWA(
                "BWA command for pairing and sorting",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT
        ),
        @Deprecated
        COMMAND_CONVEY_BWA(
                "BWA convey command for alignment",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT
        ),

        // modules and commands for OTP workflows
        COMMAND_LOAD_MODULE_LOADER(
                "command to load the module system (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_ACTIVATION_FASTQC(
                "command to enable the module containing fastqc (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_FASTQC(
                "command for fastqc",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_ACTIVATION_SAMTOOLS(
                "command to enable the module containing samtools (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_SAMTOOLS(
                "command for samtools",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_ACTIVATION_GROOVY(
                "command to enable the module containing groovy (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_GROOVY(
                "command for groovy",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_ACTIVATION_R(
                "command to enable the module containing R (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_R(
                "command for R",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_ACTIVATION_RUN_YAPSA_PREFIX(
                "command to enable the module containing runYAPSA (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        COMMAND_RUN_YAPSA(
                "command for runYAPSA",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),

        //basePath
        BASE_PATH_REFERENCE_GENOME(
                "Base path for reference genomes",
                Necessity.REQUIRED, null, TypeValidators.ABSOLUTE_PATH
        ),

        //roddy
        RODDY_PATH(
                "Path to the roddy.sh on the current cluster",
                Necessity.REQUIRED, null, TypeValidators.ABSOLUTE_PATH
        ),
        @Deprecated
        RODDY_VERSION(
                "Roddy version which is used currently to process Roddy-Pipelines",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT
        ),
        RODDY_BASE_CONFIGS_PATH(
                "Path to the baseConfig-files which are needed to execute Roddy",
                Necessity.REQUIRED, null, TypeValidators.ABSOLUTE_PATH
        ),
        RODDY_APPLICATION_INI(
                "Path to the application.ini which is needed to execute Roddy",
                Necessity.REQUIRED, null, TypeValidators.ABSOLUTE_PATH
        ),
        RODDY_FEATURE_TOGGLES_CONFIG_PATH(
                "Path to featureToggles.ini which contains feature toggles for Roddy",
                Necessity.REQUIRED, null, TypeValidators.ABSOLUTE_PATH
        ),

        //executionRestrictions
        MAXIMUM_NUMBER_OF_JOBS(
                "Maximum number of parallel jobs",
                Necessity.OPTIONAL, "1", TypeValidators.POSITIVE_NUMBER,
                TypeValidators.WORKFLOW_NAME,
        ),
        MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK(
                "Maximum number of jobs that are reserved for fast track",
                Necessity.OPTIONAL, "0", TypeValidators.POSITIVE_NUMBER,
                TypeValidators.WORKFLOW_NAME,
        ),
        MAXIMUM_PARALLEL_SSH_CALLS(
                "Maximum number of SSH channels / commands that can be executed in parallel",
                Necessity.OPTIONAL, "30", TypeValidators.POSITIVE_NUMBER
        ),
        MAXIMUM_SFTP_CONNECTIONS(
                "Maximum number of SFTP connections that can be open in parallel",
                Necessity.OPTIONAL, "5", TypeValidators.POSITIVE_NUMBER
        ),
        MAXIMUM_EXECUTED_RODDY_PROCESSES(
                "Maximum number of Roddy jobs that can be executed in parallel due to high memory usage",
                Necessity.OPTIONAL, "10", TypeValidators.POSITIVE_NUMBER,
        ),

        //statistics
        STATISTICS_BASES_PER_BYTES_FASTQ(
                "Bases Per Byte for FastQ file used to calculate count of bases before FastQC-WG is finished",
                Necessity.OPTIONAL, "2339", TypeValidators.FLOAT
        ),

        //ticketSystem
        TICKET_SYSTEM_URL(
                "Ticket system URL",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        TICKET_SYSTEM_NUMBER_PREFIX(
                "Prefix for the ticket number",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),
        TICKET_SYSTEM_AUTO_IMPORT_ENABLED(
                "Enabled auto import from the ticket system",
                Necessity.OPTIONAL, "false", TypeValidators.BOOLEAN
        ),

        // FileSystem
        FILESYSTEM_FASTQ_IMPORT(
                "Realm to use for filesystem access when importing FASTQs, local is used if empty",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT_OPTIONAL
        ),
        FILESYSTEM_BAM_IMPORT(
                "Realm to use for filesystem access when importing BAMs, local is used if empty",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT_OPTIONAL
        ),
        FILESYSTEM_PROCESSING_USE_REMOTE(
                "Use remote (true) or local (false) filesystem when processing files",
                Necessity.REQUIRED, null, TypeValidators.BOOLEAN
        ),
        FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE(
                "Use remote (true) or local (false) filesystem for config file checks",
                Necessity.REQUIRED, null, TypeValidators.BOOLEAN
        ),
        //realm
        REALM_DEFAULT_VALUE(
                "The default realm",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT
        ),

        //email
        EMAIL_RECIPIENT_NOTIFICATION(
                "Additional recipient for notifications (use ticket system if available)",
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),
        EMAIL_RECIPIENT_ERRORS(
                "Recipient of error messages",
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),
        EMAIL_SENDER(
                "Email address that is used to send emails",
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),
        EMAIL_SENDER_SALUTATION(
                "The name and optionally email of the helpdesk team, to be used in the middle of sentences",
                Necessity.OPTIONAL, "OTP team", TypeValidators.SINGLE_LINE_TEXT
        ),
        EMAIL_LINUX_GROUP_ADMINISTRATION(
                "The mail of the responsible entity to add users to linux groups",
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),

        NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE(
                "Notes from the sequencing center that is sent with every notification mail",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT,
                TypeValidators.SEQ_CENTER_NAME
        ),

        // cluster job scheduler
        CLUSTER_SUBMISSIONS_OPTION(
                "Resource requests for job submission, as JSON map",
                Necessity.OPTIONAL, "", TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION,
                TypeValidators.JOB_NAME_SEQ_TYPE,
        ),
        CLUSTER_SUBMISSIONS_FAST_TRACK_QUEUE(
                "name of the queue used by fast track projects",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT
        ),

        //validator
        VALIDATOR_SAMPLE_IDENTIFIER_REGEX(
                "regular expression for the sample id",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT
        ),

        //systemConfiguration
        TIME_ZONE(
                "Local time zone",
                Necessity.OPTIONAL, "Europe/Berlin", TypeValidators.TIME_ZONE
        ),
        OTP_USER_LINUX_GROUP(
                "linux group of the otp user",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT
        ),

        //gui
        GUI_TRACKING_PIWIK_URL(
                "URL for Piwik Tracking",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT
        ),
        GUI_TRACKING_SITE_ID(
                "Site id for Piwik Tracking",
                Necessity.OPTIONAL, "1", TypeValidators.POSITIVE_NUMBER
        ),
        GUI_TRACKING_ENABLED(
                "Enable tracking with Piwik?",
                Necessity.OPTIONAL, "false", TypeValidators.BOOLEAN
        ),
        GUI_CONTACT_DATA_SUPPORT_EMAIL(
                "Email of the helpdesk team",
                Necessity.OPTIONAL, "", TypeValidators.MAIL
        ),
        GUI_CONTACT_DATA_PERSON_IN_CHARGE(
                "Person in charge, shown on contact page",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT
        ),
        GUI_CONTACT_DATA_POSTAL_ADDRESS(
                "Postal address, shown on contact page",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT
        ),
        GUI_CONTACT_DATA_POSTAL_ADDRESS_DATA_PROTECTION_OFFICER(
                "Name and address of the data protection officer (shown in privacy policy)",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT
        ),
        GUI_CONTACT_DATA_OPERATED_BY(
                "Entity running OTP, shown on the contact page",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT
        ),
        GUI_SHOW_PARTNERS(
                "Show the 'Partners' page?",
                Necessity.OPTIONAL, "false", TypeValidators.BOOLEAN
        ),
        GUI_ABOUT_OTP(
                "Text shown on the 'About OTP' page",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT
        ),
        GUI_IMPRINT(
                "Imprint",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT
        ),
        GUI_LOGO(
                "Location specific logo",
                Necessity.OPTIONAL, "NONE", TypeValidators.INSTANCE_LOGO
        ),


        private final String description
        private final Necessity necessity
        private final String defaultValue
        private final TypeValidators validatorForValue
        private final TypeValidators validatorForType

        OptionName(String description, Necessity necessity, String defaultValue, TypeValidators validatorForValue, TypeValidators validatorForType = null) {
            assert description
            assert necessity
            assert validatorForValue
            if (necessity == Necessity.REQUIRED) {
                assert defaultValue == null : "default value must be null for required values: ${name()}"
            } else {
                assert defaultValue != null : "default value must not be null for optional values: ${name()}"
            }

            this.description = description
            this.necessity = necessity
            this.defaultValue = defaultValue
            this.validatorForValue = validatorForValue
            this.validatorForType = validatorForType
        }

        String toString() {
            name()
        }

        String getDescription() {
            description
        }

        Necessity getNecessity() {
            necessity
        }

        String getDefaultValue() {
            defaultValue
        }

        TypeValidators getValidatorForValue() {
            validatorForValue
        }

        TypeValidators getValidatorForType() {
            validatorForType
        }
    }

    OptionName name
    String type
    String value
    Project project
    Date dateCreated = new Date()
    Date dateObsoleted

    static mapping = {
        value type: 'text'
    }

    static constraints = {
        name(blank: false)
        type nullable: true, validator: { val, obj ->
            if (obj.name.validatorForType) {
                return obj.name.validatorForType.validate(val)
            } else {
                return (val == null)
            }
        }
        project(nullable: true)
        dateObsoleted(nullable: true)
        value validator: { val, obj ->
            val != null && obj.name && obj.name.validatorForValue.validate(val)
        }
    }
}

enum Necessity {
    REQUIRED,
    OPTIONAL,
}
