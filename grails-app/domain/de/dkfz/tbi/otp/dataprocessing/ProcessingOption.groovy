package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.validator.routines.*

class ProcessingOption implements Entity {

    enum OptionName {
        //pipeline
        PIPELINE_RODDY_SNV_PLUGIN_NAME("Name of the Roddy SNV pipeline plugin", "SNVCallingWorkflow", Type.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_SNV_PLUGIN_VERSION("The version of the Roddy SNV pipeline plugin", "1.0.166-1", Type.PLUGIN),
        PIPELINE_RODDY_SNV_BASE_PROJECT_CONFIG("The base project file for Roddy SNV pipeline", null, Type.SINGLE_WORD_TEXT),

        PIPELINE_RODDY_INDEL_PLUGIN_NAME("Name of the Indel pipeline plugin", "IndelCallingWorkflow", Type.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_INDEL_PLUGIN_VERSION("The version of the Indel pipeline plugin", "1.0.167", Type.PLUGIN),
        PIPELINE_RODDY_INDEL_PLUGIN_CONFIG("The base project file for Indel pipeline", "otpIndelCallingWorkflow-1.0", Type.SINGLE_WORD_TEXT),

        PIPELINE_ACESEQ_PLUGIN_NAME("Name of the Aceseq pipeline plugin", "ACEseqWorkflow", Type.SINGLE_WORD_TEXT),
        PIPELINE_ACESEQ_PLUGIN_VERSION("The version of the Aceseq pipeline plugin", "1.2.6", Type.PLUGIN),
        PIPELINE_ACESEQ_BASE_PROJECT_CONFIG("The base project file for Aceseq pipeline", "otpACEseq-1.0", Type.SINGLE_WORD_TEXT),
        PIPELINE_ACESEQ_REFERENCE_GENOME("Name of reference genomes for Aceseq", '', Type.SINGLE_LINE_TEXT),

        PIPELINE_SOPHIA_PLUGIN_NAME("Name of the Sophia pipeline plugin", "SophiaWorkflow", Type.SINGLE_WORD_TEXT),
        PIPELINE_SOPHIA_PLUGIN_VERSIONS("The version of the Sophia pipeline plugin", "1.0.15", Type.PLUGIN),
        PIPELINE_SOPHIA_BASE_PROJECT_CONFIG("The base project file for Sophia pipeline", "otpSophia-1.0", Type.SINGLE_WORD_TEXT),
        PIPELINE_SOPHIA_REFERENCE_GENOME("Name of reference genomes for Sophia", '', Type.SINGLE_LINE_TEXT),

        PIPELINE_RODDY_ALIGNMENT_PLUGIN_NAME("Name of the alignment plugin, used in configure alignment", "AlignmentAndQCWorkflows", Type.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_ALIGNMENT_PLUGIN_VERSION("The version of the roddy alignment plugin", null, Type.PLUGIN),
        PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG("The base project file for Roddy alignment", null, Type.SINGLE_WORD_TEXT),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_REFERENCE_GENOME_NAME("Default reference genome", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL("Default merge tool", "sambamba", Type.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_ALL_MERGED_TOOLS("All merge tools", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT("Default version for alignment with bwa_mem", "0.7.15", Type.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE("Available versions for alignment with bwa_mem", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT("Default version for merging and duplication marking with sambamba", "0.6.5", Type.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE("Available versions for merging and duplication marking with sambamba", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_RODDY_ALIGNMENT_BWA_PATHS("Path to bwa_mem", null, Type.PATH),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_PATHS("Path to sambamba", null, Type.PATH),
        PIPELINE_RODDY_ALIGNMENT_GENOME_STAR_INDEX("Default genome star index", null, Type.SINGLE_LINE_TEXT),

        PIPELINE_OTP_ALIGNMENT_BWA_NUMBER_OF_CORES("number of cores/threads", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_NUMBER_OF_CORES("number of cores/threads", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_QUALITY_ENCODING("option for quality encoding", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_SAMTOOLS_SORT_BUFFER("Buffer for samtools sorting", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_BWA_QUEUE_PARAMETER("quality threshold for 'soft' read trimming down to 35bp", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_PICARD_JAVA_SETTINGS("Java options for Picard", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT("Quality assessment Command and parameters template", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_PICARD_MDUP("picard option used in duplicates marking", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_NUMBER_OF_SAMPE_THREADS("number of threads to be used in processing with binary bwa sampe", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_NUMBER_OF_SAMTOOLS_SORT_THREADS("number of threads to be used in processing with samtools sort", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_MBUFFER_PAIRING_SORTING("size of buffer to be used for pairing with binary bwa sampe", null, Type.SINGLE_LINE_TEXT),
        PIPELINE_OTP_ALIGNMENT_INSERT_SIZE_CUT_OFF("BWA pairing insert size cut-off", null, Type.SINGLE_LINE_TEXT),

        //basePath
        BASE_PATH_REFERENCE_GENOME("Base path for reference genomes", null, Type.PATH),

        //roddy
        RODDY_PATH("Path to the roddy.sh on the current cluster", null, Type.PATH),
        RODDY_VERSION("Roddy version which is used currently to process Roddy-Pipelines", null, Type.SINGLE_LINE_TEXT),
        RODDY_BASE_CONFIGS_PATH("Path to the baseConfig-files which are needed to execute Roddy", null, Type.PATH),
        RODDY_APPLICATION_INI("Path to the application.ini which is needed to execute Roddy", null, Type.PATH),
        RODDY_FEATURE_TOGGLES_CONFIG_PATH("Path to featureToggles.ini which contains feature toggles for Roddy", null, Type.PATH),

        //commands
        COMMAND_CONVEY_BWA("BWA convey command for alignment", "cnybwa-0.6.2", Type.SINGLE_WORD_TEXT),
        COMMAND_SAMTOOLS("samtools (Tools for alignments in the SAM format)", "samtools-0.1.19", Type.SINGLE_WORD_TEXT),
        COMMAND_FASTQC("command for fastqc with java", "fastqc-0.10.1 --java /path/to/programs/jdk/jdk1.6.0_45/bin/java", Type.SINGLE_LINE_TEXT),
        COMMAND_PICARD_MDUP("command for versioned picard", "picard-1.61.sh MarkDuplicates", Type.SINGLE_LINE_TEXT),

        //executionRestrictions
        MAXIMUM_NUMBER_OF_JOBS("max parallel jobs", null, Type.POSITIVE_NUMBER),
        MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK("", null, Type.POSITIVE_NUMBER),
        MAXIMUM_PARALLEL_SSH_CALLS("Maximum number of ssh calls that can be executed in parallel", "30", Type.POSITIVE_NUMBER),
        MAXIMUM_EXECUTED_RODDY_PROCESSES("Maximum number of Roddy jobs that can be executed in parallel due to high memory usage", "10", Type.POSITIVE_NUMBER),

        //statistics
        STATISTICS_BASES_PER_BYTES_FASTQ("Bases Per Byte for FastQ file used to calculate count of bases before FastQC-WG is finished", null, Type.FLOAT),

        //ticketSystem
        TICKET_SYSTEM_URL("Ticket system URL", null, Type.SINGLE_LINE_TEXT),
        TICKET_SYSTEM_NUMBER_PREFIX("Prefix for the ticket number", null, Type.SINGLE_LINE_TEXT),
        TICKET_SYSTEM_AUTO_IMPORT_ENABLED("Enabled auto import from the ticket system", null, Type.BOOLEAN),

        //email
        EMAIL_RECIPIENT_NOTIFICATION("", null, Type.MAIL),
        EMAIL_RECIPIENT_ERRORS("", null, Type.MAIL),
        EMAIL_SENDER("", null, Type.MAIL),

        //notificationTemplate
        NOTIFICATION_TEMPLATE_SNV_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_INDEL_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_SOPHIA_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_ACESEQ_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_SNV_NOT_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_INDEL_NOT_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_SOPHIA_NOT_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_ACESEQ_NOT_PROCESSED("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_BASE("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_INSTALLATION("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_INSTALLATION_FURTHER_PROCESSING("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_ALIGNMENT("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_ALIGNMENT_PROCESSING("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_ALIGNMENT_FURTHER_PROCESSING("", null, Type.MULTI_LINE_TEXT),
        NOTIFICATION_TEMPLATE_ALIGNMENT_NO_FURTHER_PROCESSING("", null, Type.MULTI_LINE_TEXT),

        //clusterSubmissionOption
        CLUSTER_SUBMISSIONS_OPTION("", null, Type.PBS_OPTION),

        //validator
        VALIDATOR_SAMPLE_IDENTIFIER_REGEX("regular expression for the DEEP sample id", null, Type.SINGLE_LINE_TEXT),

        //systemConfiguration
        TIME_ZONE("The default time zone", "Europe/Berlin", Type.SINGLE_LINE_TEXT),
        OTP_USER_LINUX_GROUP("linux group of the otp user", null, Type.SINGLE_LINE_TEXT),

        //gui
        GUI_TRACKING_PIWIK_URL("URL for Piwik Tracking", null, Type.SINGLE_LINE_TEXT),
        GUI_TRACKING_SITE_ID("Site id for Piwik Tracking", "3", Type.POSITIVE_NUMBER),
        GUI_TRACKING_ENABLED("enable (true) or disable (false) tracking", null, Type.BOOLEAN),
        GUI_CONTACT_DATA_SUPPORT_EMAIL("", null, Type.MAIL),
        GUI_CONTACT_DATA_PERSON_IN_CHARGE("", null, Type.SINGLE_LINE_TEXT),
        GUI_CONTACT_DATA_POSTAL_ADDRESS("", null, Type.SINGLE_LINE_TEXT),
        GUI_CONTACT_DATA_OPERATED_BY("", null, Type.SINGLE_LINE_TEXT),
        GUI_SHOW_PARTNERS("", null, Type.BOOLEAN),
        GUI_ABOUT_OTP("", null, Type.MULTI_LINE_TEXT)


        private final String description
        private final String defaultValue
        private final Type type

        OptionName(String description, String defaultValue, Type type) {
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

        Type getType() {
            type
        }
    }

    static enum Type {
        SINGLE_LINE_TEXT({it ==~ /[^\n]+/}),
        MULTI_LINE_TEXT({it ==~ /.*/}),
        SINGLE_WORD_TEXT({it ==~ /\S+/}),
        MAIL({EmailValidator.getInstance().isValid(it)}),
        PATH({OtpPath.isValidPathComponent(it)}),
        FILE({OtpPath.isValidAbsolutePath(it)}),
        POSITIVE_NUMBER({it ==~ /^[\+]?\d+$/}),
        PLUGIN({it ==~ /^\d+\.\d+\.\d+(\-\d+)?$/}),
        BOOLEAN({it ==~ /true|false/}),
        FLOAT({it ==~ /^[\+\-]?\d+\.?\d*$/}),
        PBS_OPTION({/*TODO after OTP-2447*/true}),

        private Closure validator

        Type(Closure validator) {
            this.validator = validator
        }

        boolean validate() {
            validator()
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
        value validate: {val, obj->
            val && obj.type && obj.name.type.validate()
        }
    }
}
