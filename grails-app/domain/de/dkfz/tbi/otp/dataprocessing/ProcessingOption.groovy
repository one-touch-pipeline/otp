/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.TypeValidators
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

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
                TypeValidators.SEQ_TYPE_RODDY_NAME_SNV
        ),
        PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Roddy SNV pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME_SNV
        ),

        PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_NAME(
                "Default name of the Indel pipeline plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "IndelCallingWorkflow", TypeValidators.SINGLE_WORD_TEXT,
        ),
        PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_VERSION(
                "Default version of the Indel pipeline plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME_INDEL
        ),
        PIPELINE_RODDY_INDEL_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Indel pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME_INDEL
        ),

        PIPELINE_ACESEQ_DEFAULT_PLUGIN_NAME(
                "Default name of the Aceseq pipeline plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "ACEseqWorkflow", TypeValidators.SINGLE_WORD_TEXT,
        ),
        PIPELINE_ACESEQ_DEFAULT_PLUGIN_VERSION(
                "Default version of the Aceseq pipeline plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME_ACESEQ
        ),
        PIPELINE_ACESEQ_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Aceseq pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME_ACESEQ
        ),
        PIPELINE_ACESEQ_REFERENCE_GENOME(
                "Name of reference genomes for Aceseq, comma-separated",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        PIPELINE_RUNYAPSA_DEFAULT_VERSION(
                "Default runYapsa version, used when configuring the pipeline",
                Necessity.OPTIONAL, "yapsa-devel/80f748e", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        PIPELINE_RUNYAPSA_AVAILABLE_VERSIONS(
                "runYapsa versions, comma-separated",
                Necessity.OPTIONAL, "yapsa-devel/80f748e", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        PIPELINE_RUNYAPSA_REFERENCE_GENOME(
                "Name of reference genomes for runYapsa, comma-separated",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        PIPELINE_CELLRANGER_DEFAULT_VERSION(
                "Default Cell Ranger version, used when configuring the pipeline",
                Necessity.OPTIONAL, "cellranger/6.1.1", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        PIPELINE_CELLRANGER_AVAILABLE_VERSIONS(
                "Cell Ranger versions, comma-separated",
                Necessity.OPTIONAL, "cellranger/3.0.1, cellranger/3.1.0, cellranger/4.0.0, cellranger/6.1.1", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        PIPELINE_CELLRANGER_CORE_COUNT(
                "Count of cores to use by cell ranger, used for parameter '--localcores'",
                Necessity.OPTIONAL, "15", TypeValidators.POSITIVE_NUMBER
        ),
        PIPELINE_CELLRANGER_CORE_MEM(
                "Allowed memory in GB to use by Cell Ranger, used for parameter '--localmem', at least 16 is required",
                Necessity.OPTIONAL, "60", TypeValidators.POSITIVE_NUMBER
        ),

        PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME(
                "Default name of the Sophia pipeline plugin, used when configuring the pipeline",
                Necessity.OPTIONAL, "SophiaWorkflow", TypeValidators.SINGLE_WORD_TEXT,
        ),
        PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS(
                "Default version of the Sophia pipeline plugin, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.PLUGIN,
                TypeValidators.SEQ_TYPE_RODDY_NAME_SOPHIA
        ),
        PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG(
                "Default base project file for Sophia pipeline, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT,
                TypeValidators.SEQ_TYPE_RODDY_NAME_SOPHIA
        ),
        PIPELINE_SOPHIA_REFERENCE_GENOME(
                "Name of reference genomes for Sophia, comma-separated",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
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
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL(
                "Default merge tool, used when configuring the pipeline",
                Necessity.OPTIONAL, "sambamba", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_ALL_MERGE_TOOLS(
                "All merge tools, used when configuring the pipeline",
                Necessity.OPTIONAL, "picard,biobambam,sambamba", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL,
                TypeValidators.SEQ_TYPE_RODDY_NAME
        ),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT(
                "Default version for alignment with bwa_mem, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.7.15", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE(
                "Available versions for alignment with bwa_mem, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.7.8, 0.7.15, 0.7.17", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT(
                "Default version for merging and duplication marking with sambamba, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.6.5", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE(
                "Available versions for merging and duplication marking with sambamba, used when configuring the pipeline",
                Necessity.OPTIONAL, "0.5.9, 0.6.5", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
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
        PIPELINE_RODDY_ALIGNMENT_RNA_DEFAULT_GENOME_STAR_INDEX(
                "Default genome star index, used when configuring the pipeline",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL,
        ),

        @Deprecated
        PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT(
                "Quality assessment Command and parameters template",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        @Deprecated
        COMMAND_BWA(
                "BWA command for pairing and sorting",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        @Deprecated
        COMMAND_CONVEY_BWA(
                "BWA convey command for alignment",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        // modules and commands for OTP workflows
        COMMAND_LOAD_MODULE_LOADER(
                "command to load the module system (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_ENABLE_MODULE(
                "command to enable a module. The module name will provided somewhere else",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_ACTIVATION_FASTQC(
                "command to enable the module containing fastqc (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_FASTQC(
                "command for fastqc",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_ACTIVATION_SAMTOOLS(
                "command to enable the module containing samtools (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_SAMTOOLS(
                "command for samtools",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_ACTIVATION_GROOVY(
                "command to enable the module containing groovy (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_GROOVY(
                "command for groovy",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_ACTIVATION_JAVA(
                "command to enable the module containing java (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        COMMAND_ACTIVATION_R(
                "command to enable the module containing R (executed in Bash, may be empty if not required)",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        @Deprecated
        COMMAND_R(
                "command for R",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        @Deprecated
        COMMAND_RUN_YAPSA(
                "command for runYAPSA",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
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
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        @Deprecated // base configs are not used anymore by the new WF system
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
        RODDY_SHARED_FILES_BASE_DIRECTORY(
                "Directory containing bioinformatics database, reference genomes etc.",
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
                Necessity.OPTIONAL, "2.339", TypeValidators.FLOAT
        ),

        //ticketSystem
        TICKET_SYSTEM_URL(
                "Ticket system URL",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        TICKET_SYSTEM_NUMBER_PREFIX(
                "Prefix for the ticket number",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        TICKET_SYSTEM_AUTO_IMPORT_ENABLED(
                "Enabled auto import from the ticket system",
                Necessity.OPTIONAL, "false", TypeValidators.BOOLEAN
        ),

        // FileSystem
        FILESYSTEM_FASTQ_IMPORT(
                "Realm to use for filesystem access when importing FASTQs, local is used if empty",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_WORD_TEXT_OPTIONAL
        ),
        FILESYSTEM_BAM_IMPORT(
                "Realm to use for filesystem access when importing BAMs, local is used if empty",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_WORD_TEXT_OPTIONAL
        ),
        FILESYSTEM_PROCESSING_USE_REMOTE(
                "Use remote (true) or local (false) filesystem when processing files",
                Necessity.REQUIRED, null, TypeValidators.BOOLEAN
        ),
        FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE(
                "Use remote (true) or local (false) filesystem for config file checks",
                Necessity.REQUIRED, null, TypeValidators.BOOLEAN
        ),
        FILESYSTEM_TIMEOUT(
                "The amount of time (im minutes) the FileService allows the filesystem until a given commands times out",
                Necessity.OPTIONAL, "0", TypeValidators.POSITIVE_NUMBER
        ),

        //defaults
        PROCESSING_PRIORITY_DEFAULT_NAME(
                "The default processing priority, used as preselection in project creation. If not given, the oldest object is used as default.",
                Necessity.REQUIRED, null, TypeValidators.PROCESSING_PRIORITY_NAME
        ),

        //realm
        REALM_DEFAULT_VALUE(
                "The default realm by name",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT
        ),

        //email
        EMAIL_TICKET_SYSTEM(
                "Email address to the ticket system. It gets all (!) emails in CC.",
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),
        EMAIL_REPLY_TO(
                "Email address that is used to reply emails",
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),
        EMAIL_SENDER(
                "Email address that is used to send emails",
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),
        EMAIL_SENDER_SALUTATION(
                "The name and optionally email of the helpdesk team, to be used in the middle of sentences",
                Necessity.OPTIONAL, "OTP team", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        EMAIL_TEXT_REASON_DEACTIVATED_USERS(
                "A reason which will be printed in an email to the PI, which informs about deactivated users.",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT
        ),

        NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE(
                "Notes from the sequencing center that is sent with every notification mail",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT,
                TypeValidators.SEQ_CENTER_NAME
        ),
        NOTIFICATION_TEMPLATE_FAQ_LINK(
                "Link to FAQs",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        BLACKLIST_IMPORT_SOURCE_NOTIFICATION(
                "Comma separated list of path prefixes which are ignored in the import source notification",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        @SuppressWarnings('GStringExpressionWithinString')
        AD_GROUP_ADD_USER_SNIPPET(
                "Shell program to do AD group changes with, outside of OTP, e.g. adtool." +
                        " OTP will interpret the placeholders \${unixGroup} and \${userName} when using this template.",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        @SuppressWarnings('GStringExpressionWithinString')
        AD_GROUP_REMOVE_USER_SNIPPET(
                "Shell program to do AD group changes with, outside of OTP, e.g. adtool." +
                        " OTP will interpret the placeholders \${unixGroup} and \${userName} when using this template",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        // cluster job scheduler
        CLUSTER_SUBMISSIONS_OPTION(
                "Resource requests for job submission, as JSON map",
                Necessity.OPTIONAL, "", TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION,
                TypeValidators.JOB_NAME_SEQ_TYPE,
        ),
        @Deprecated
        CLUSTER_SUBMISSIONS_FAST_TRACK_QUEUE(
                "name of the queue used by fast track projects",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_WORD_TEXT
        ),

        //validator
        VALIDATOR_SAMPLE_IDENTIFIER_REGEX(
                "regular expression for the sample id",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        //systemConfiguration
        TIME_ZONE(
                "Local time zone",
                Necessity.OPTIONAL, "Europe/Berlin", TypeValidators.TIME_ZONE
        ),
        OTP_SYSTEM_USER(
                "reference to an existing user to use for system work",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        OTP_USER_LINUX_GROUP(
                "linux group of the otp user",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),
        WITHDRAWN_UNIX_GROUP(
                "unix group to use for withdrawn data",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        //ldap
        LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD(
                "The grace period in days before OTP removes a user from all projects, when they are deactivated in the LDAP",
                Necessity.OPTIONAL, "30", TypeValidators.POSITIVE_NUMBER
        ),
        LDAP_RESPECT_DEACTIVATED_USER(
                "Should OTP consider deactivated users? true = evaluate; false = skip evaluation, all users are activated",
                Necessity.OPTIONAL, "true", TypeValidators.BOOLEAN
        ),

        //gui
        GUI_TRACKING_PIWIK_URL(
                "URL for Piwik Tracking",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
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
                Necessity.REQUIRED, null, TypeValidators.MAIL
        ),
        GUI_CONTACT_DATA_PERSON_IN_CHARGE(
                "Person in charge, shown on contact page",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
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
        GUI_IGNORE_UNREGISTERED_OTP_USERS_FOUND(
                "This value is used to not show certain members, in a comma separated list," +
                        " which will not be found under the 'Unregistered OTP user found' section of project members page",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_WORD_TEXT_OPTIONAL
        ),
        /** @see GuiAnnotation */
        GUI_ANNOTATION(
                "This is the category for site-specific, configurable, GUI annotations. The contents" +
                        " of specific annotations will be interpreted as HTML, and must handle being unset gracefully",
                Necessity.OPTIONAL, "", TypeValidators.MULTI_LINE_TEXT,
                TypeValidators.GUI_ANNOTATION
        ),

        CRONJOB_ACTIVE(
                "Defines if the cronjob of the given subtype should be executed or not",
                Necessity.OPTIONAL, "true", TypeValidators.BOOLEAN,
                TypeValidators.CRONJOB_CLASS
        ),

        //naming
        CLUSTER_NAME(
                "The common name to refer to the cluster OTP uses",
                Necessity.REQUIRED, null, TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
        ),

        // CellRanger cleanup
        CELLRANGER_CLEANUP_WEEKS_TILL_DELETION_AFTER_REMINDER(
                "How many weeks to wait, after reminder was sent, before uncategorised CellRanger results are deleted again",
                Necessity.OPTIONAL, "4", TypeValidators.POSITIVE_NUMBER
        ),
        CELLRANGER_CLEANUP_WEEKS_TILL_REMINDER(
                "How many weeks to wait before bioinformaticians of a project are reminded that they must categorise their CellRanger results",
                Necessity.OPTIONAL, "12", TypeValidators.POSITIVE_NUMBER
        ),
        METADATA_WHITELIST_COLUMNS(
                "Mention metadata columns that should be exported; provide column names that are comma or semi-colon or tab separated, values are trimmed",
                Necessity.OPTIONAL, "FASTQ_FILE, PROJECT, SAMPLE_NAME, SEQUENCING_TYPE", TypeValidators.SINGLE_LINE_TEXT
        ),

        REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST(
                "Regex for project name",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_WORD_TEXT_OPTIONAL
        ),

        DESCRIPTION_PROJECT_NAME_NEW_PROJECT_REQUEST(
                "Description for project name",
                Necessity.OPTIONAL, "", TypeValidators.SINGLE_LINE_TEXT_OPTIONAL
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

        @Override
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

        boolean isDeprecated() {
            return this.class.getField(name()).isAnnotationPresent(Deprecated)
        }
    }

    OptionName name
    String type
    @SuppressWarnings("GrailsDomainReservedSqlKeywordName")
    String value
    Project project
    Date dateObsoleted

    static belongsTo = [
        project: Project,
    ]

    static mapping = {
        value type: 'text'
    }

    static constraints = {
        name(blank: false)
        type(nullable: true, validator: { val, obj ->
            if (obj.dateObsoleted) {
                return true
            }
            if (obj.name.validatorForType) {
                return obj.name.validatorForType.validate(val)
            }
            return (val == null)
        })
        project(nullable: true)
        dateObsoleted(nullable: true)
        value(validator: { val, obj ->
            (val != null && obj.name && obj.name.validatorForValue.validate(val)) || obj.dateObsoleted
        })
    }
}

enum Necessity {
    REQUIRED,
    OPTIONAL,
}
