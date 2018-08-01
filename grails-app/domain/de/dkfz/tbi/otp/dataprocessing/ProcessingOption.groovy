package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.configs.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class ProcessingOption implements Entity {

    enum OptionName {
        //pipeline
        PIPELINE_MIN_COVERAGE("minimum coverage to start a pipeline", null, TypeValidators.FLOAT),

        PIPELINE_RODDY_SNV_PLUGIN_NAME("Name of the Roddy SNV pipeline plugin", "SNVCallingWorkflow", TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_SNV_PLUGIN_VERSION("The version of the Roddy SNV pipeline plugin", "1.0.166-1", TypeValidators.PLUGIN),
        PIPELINE_RODDY_SNV_BASE_PROJECT_CONFIG("The base project file for Roddy SNV pipeline", null, TypeValidators.SINGLE_WORD_TEXT),

        PIPELINE_RODDY_INDEL_PLUGIN_NAME("Name of the Indel pipeline plugin", "IndelCallingWorkflow", TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_INDEL_PLUGIN_VERSION("The version of the Indel pipeline plugin", "1.0.167", TypeValidators.PLUGIN),
        PIPELINE_RODDY_INDEL_PLUGIN_CONFIG("The base project file for Indel pipeline", "otpIndelCallingWorkflow-1.0", TypeValidators.SINGLE_WORD_TEXT),

        PIPELINE_ACESEQ_PLUGIN_NAME("Name of the Aceseq pipeline plugin", "ACEseqWorkflow", TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_ACESEQ_PLUGIN_VERSION("The version of the Aceseq pipeline plugin", "1.2.6", TypeValidators.PLUGIN),
        PIPELINE_ACESEQ_BASE_PROJECT_CONFIG("The base project file for Aceseq pipeline", "otpACEseq-1.0", TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_ACESEQ_REFERENCE_GENOME("Name of reference genomes for Aceseq, comma-separated", '', TypeValidators.SINGLE_LINE_TEXT),

        PIPELINE_RUNYAPSA_DEFAULT_VERSION("runYapsa default version", '', TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RUNYAPSA_AVAILABLE_VERSIONS("runYapsa versions, comma-separated", '', TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RUNYAPSA_REFERENCE_GENOME("Name of reference genomes for runYapsa, comma-separated", '', TypeValidators.SINGLE_LINE_TEXT),

        PIPELINE_SOPHIA_PLUGIN_NAME("Name of the Sophia pipeline plugin", "SophiaWorkflow", TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_SOPHIA_PLUGIN_VERSIONS("The version of the Sophia pipeline plugin", "1.0.15", TypeValidators.PLUGIN),
        PIPELINE_SOPHIA_BASE_PROJECT_CONFIG("The base project file for Sophia pipeline", "otpSophia-1.0", TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_SOPHIA_REFERENCE_GENOME("Name of reference genomes for Sophia, comma-separated", '', TypeValidators.SINGLE_LINE_TEXT),

        PIPELINE_RODDY_ALIGNMENT_PLUGIN_NAME("Name of the alignment plugin, used in configure alignment", "AlignmentAndQCWorkflows", TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_ALIGNMENT_PLUGIN_VERSION("The version of the roddy alignment plugin", null, TypeValidators.PLUGIN),
        PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG("The base project file for Roddy alignment", null, TypeValidators.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME("Default reference genome", null, TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL("Default merge tool", "sambamba", TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_ALL_MERGED_TOOLS("All merge tools", null, TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT("Default version for alignment with bwa_mem", "0.7.15", TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE("Available versions for alignment with bwa_mem", null, TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT("Default version for merging and duplication marking with sambamba", "0.6.5", TypeValidators.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE("Available versions for merging and duplication marking with sambamba", null, TypeValidators.SINGLE_LINE_TEXT),
        @Deprecated
        PIPELINE_RODDY_ALIGNMENT_BWA_PATHS("Path to bwa_mem", null, TypeValidators.ABSOLUTE_PATH),
        @Deprecated
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_PATHS("Path to sambamba", null, TypeValidators.ABSOLUTE_PATH),
        PIPELINE_RODDY_ALIGNMENT_GENOME_STAR_INDEX("Default genome star index", null, TypeValidators.SINGLE_LINE_TEXT),

        PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT("Quality assessment Command and parameters template", null, TypeValidators.SINGLE_LINE_TEXT),

        COMMAND_BWA("BWA command for pairing and sorting", "", TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_CONVEY_BWA("BWA convey command for alignment", "", TypeValidators.SINGLE_LINE_TEXT),

        // modules and commands for OTP workflows
        COMMAND_LOAD_MODULE_LOADER("command to load the module system (executed in Bash, may be empty if not required)", null, TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_ACTIVATION_FASTQC("command to enable the module containing fastqc (executed in Bash, may be empty if not required)", "", TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_FASTQC("command for fastqc", null, TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_ACTIVATION_SAMTOOLS("command to enable the module containing samtools (executed in Bash, may be empty if not required)", "", TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_SAMTOOLS("command for samtools", null, TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_ACTIVATION_GROOVY("command to enable the module containing groovy (executed in Bash, may be empty if not required)", "", TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_GROOVY("command for groovy", null, TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_ACTIVATION_R("command to enable the module containing R (executed in Bash, may be empty if not required)", "", TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_R("command for R", null, TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_ACTIVATION_RUN_YAPSA_PREFIX("command to enable the module containing runYAPSA (executed in Bash, may be empty if not required)", "", TypeValidators.SINGLE_LINE_TEXT),
        COMMAND_RUN_YAPSA("command for runYAPSA", null, TypeValidators.SINGLE_LINE_TEXT),

        //basePath
        BASE_PATH_REFERENCE_GENOME("Base path for reference genomes", null, TypeValidators.ABSOLUTE_PATH),

        //roddy
        RODDY_PATH("Path to the roddy.sh on the current cluster", null, TypeValidators.ABSOLUTE_PATH),
        RODDY_VERSION("Roddy version which is used currently to process Roddy-Pipelines", null, TypeValidators.SINGLE_LINE_TEXT),
        RODDY_BASE_CONFIGS_PATH("Path to the baseConfig-files which are needed to execute Roddy", null, TypeValidators.ABSOLUTE_PATH),
        RODDY_APPLICATION_INI("Path to the application.ini which is needed to execute Roddy", null, TypeValidators.ABSOLUTE_PATH),
        RODDY_FEATURE_TOGGLES_CONFIG_PATH("Path to featureToggles.ini which contains feature toggles for Roddy", null, TypeValidators.ABSOLUTE_PATH),

        //executionRestrictions
        MAXIMUM_NUMBER_OF_JOBS("max parallel jobs", null, TypeValidators.POSITIVE_NUMBER),
        MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK("", null, TypeValidators.POSITIVE_NUMBER),
        MAXIMUM_PARALLEL_SSH_CALLS("Maximum number of SSH channels / commands that can be executed in parallel", "30", TypeValidators.POSITIVE_NUMBER),
        MAXIMUM_SFTP_CONNECTIONS("Maximum number of SFTP connections that can be open in parallel", "5", TypeValidators.POSITIVE_NUMBER),
        MAXIMUM_EXECUTED_RODDY_PROCESSES("Maximum number of Roddy jobs that can be executed in parallel due to high memory usage", "10", TypeValidators.POSITIVE_NUMBER),

        //statistics
        STATISTICS_BASES_PER_BYTES_FASTQ("Bases Per Byte for FastQ file used to calculate count of bases before FastQC-WG is finished", null, TypeValidators.FLOAT),

        //ticketSystem
        TICKET_SYSTEM_URL("Ticket system URL", null, TypeValidators.SINGLE_LINE_TEXT),
        TICKET_SYSTEM_NUMBER_PREFIX("Prefix for the ticket number", null, TypeValidators.SINGLE_LINE_TEXT),
        TICKET_SYSTEM_AUTO_IMPORT_ENABLED("Enabled auto import from the ticket system", null, TypeValidators.BOOLEAN),


        // FileSystem
        FILESYSTEM_FASTQ_IMPORT("Realm to use for filesystem access when importing FASTQs, local is used if empty", null, TypeValidators.SINGLE_WORD_TEXT_OPTIONAL),
        FILESYSTEM_BAM_IMPORT("Realm to use for filesystem access when importing BAMs, local is used if empty", null, TypeValidators.SINGLE_WORD_TEXT_OPTIONAL),
        FILESYSTEM_PROCESSING_USE_REMOTE("Use remote (true) or local (false) filesystem when processing files", null, TypeValidators.BOOLEAN),
        FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE("Use remote (true) or local (false) filesystem for config file checks", null, TypeValidators.BOOLEAN),
        //realm
        REALM_DEFAULT_VALUE("The default realm", null, TypeValidators.SINGLE_WORD_TEXT),

        //email
        EMAIL_RECIPIENT_NOTIFICATION("", null, TypeValidators.MAIL),
        EMAIL_RECIPIENT_ERRORS("", null, TypeValidators.MAIL),
        EMAIL_SENDER("", null, TypeValidators.MAIL),
        EMAIL_SENDER_SALUTATION("The name and optionally email of the helpdesk team, to be used in the middle of sentences", null, TypeValidators.SINGLE_LINE_TEXT),
        EMAIL_LINUX_GROUP_ADMINISTRATION("The mail of the responsible entity to add users to linux groups", null, TypeValidators.MAIL),

        NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE("", null, TypeValidators.MULTI_LINE_TEXT),

        // cluster job scheduler
        CLUSTER_SUBMISSIONS_OPTION("", null, TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION),
        CLUSTER_SUBMISSIONS_FAST_TRACK_QUEUE("name of the queue used by fast track projects", null, TypeValidators.SINGLE_WORD_TEXT),

        //validator
        VALIDATOR_SAMPLE_IDENTIFIER_REGEX("regular expression for the sample id", null, TypeValidators.SINGLE_LINE_TEXT),

        //systemConfiguration
        TIME_ZONE("The default time zone", "Europe/Berlin", TypeValidators.SINGLE_LINE_TEXT),
        OTP_USER_LINUX_GROUP("linux group of the otp user", null, TypeValidators.SINGLE_LINE_TEXT),

        //gui
        GUI_TRACKING_PIWIK_URL("URL for Piwik Tracking", null, TypeValidators.SINGLE_LINE_TEXT),
        GUI_TRACKING_SITE_ID("Site id for Piwik Tracking", "3", TypeValidators.POSITIVE_NUMBER),
        GUI_TRACKING_ENABLED("enable (true) or disable (false) tracking", null, TypeValidators.BOOLEAN),
        GUI_CONTACT_DATA_SUPPORT_EMAIL("", null, TypeValidators.MAIL),
        GUI_CONTACT_DATA_PERSON_IN_CHARGE("", null, TypeValidators.SINGLE_LINE_TEXT),
        GUI_CONTACT_DATA_POSTAL_ADDRESS("", null, TypeValidators.MULTI_LINE_TEXT),
        GUI_CONTACT_DATA_POSTAL_ADDRESS_DATA_PROTECTION_OFFICER("", null, TypeValidators.MULTI_LINE_TEXT),
        GUI_CONTACT_DATA_OPERATED_BY("", null, TypeValidators.MULTI_LINE_TEXT),
        GUI_SHOW_PARTNERS("", null, TypeValidators.BOOLEAN),
        GUI_ABOUT_OTP("", null, TypeValidators.MULTI_LINE_TEXT),
        GUI_IMPRINT("", null, TypeValidators.MULTI_LINE_TEXT),
        GUI_LOGO("", "", TypeValidators.INSTANCE_LOGO),


        private final String description
        private final String defaultValue
        private final TypeValidators type

        OptionName(String description, String defaultValue, TypeValidators type) {
            this.description = description
            this.defaultValue = defaultValue
            this.type = type
        }

        String toString() {
            name()
        }

        String getDescription() {
            description
        }

        String getDefaultValue() {
            defaultValue
        }

        TypeValidators getType() {
            type
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
        type(nullable: true, blank: false)
        project(nullable: true)
        dateObsoleted(nullable: true)
        value validator: { val, obj ->
            val != null && obj.name && obj.name.type.validate(val)
        }
    }
}
