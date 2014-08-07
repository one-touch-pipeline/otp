package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.*

/**
 * To be more flexible the configuration shall be stored in the database instead of in the code.
 * This domain stores the configuration project and seqType specific.
 * If the configuration changes, the old database entry is set to obsolete and the new entry refers to the old entry.
 *
 */
class ConfigPerProjectAndSeqType {

    /**
     * This domain is very general and can be used for many different purposes.
     * Therefore the purpose for the config files has to be defined.
     * The possible purposes are listed in this enum.
     */
    enum Purpose {
        SNV
    }


    static belongsTo = [
        project: Project,
        seqType: SeqType
    ]

    /**
     * In this String the complete content of the config file is stored.
     * This solution was chosen to be as flexible as possible in case the style of the config file changes.
     */
    String configuration

    /**
     * The specification of the purpose of this config file.
     */
    Purpose purpose

    /**
     * These properties are handled automatically by grails.
     */
    Date dateCreated
    Date lastUpdated

    /**
     * When changes appear in the configuration, a new ConfigPerProjectAndSeqType entry is created and the old entry is set to obsolete.
     */
    Date obsoleteDate
    /**
     * When a previous config files exists, it should be referred here.
     * This is needed for tracking.
     */
    ConfigPerProjectAndSeqType previousConfig

    static constraints = {
        previousConfig nullable: true, validator: { val, obj ->
            return (val == null || val != null && val.obsoleteDate != null)
        }
        obsoleteDate nullable: true
        configuration blank: false
    }

    static mapping = {
        configuration type: 'text'
        project index: 'config_per_project_and_seq_type_project_idx'
        seqType index: 'config_per_project_and_seq_type_seq_type_idx'
        previousConfig index: 'config_per_project_and_seq_type_previous_config_idx'
    }
}
