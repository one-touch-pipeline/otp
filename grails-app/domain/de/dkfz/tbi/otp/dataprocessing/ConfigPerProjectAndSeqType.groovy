package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.TimeStamped
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.Entity

/**
 * To be more flexible the configuration shall be stored in the database instead of in the code.
 * This domain stores the configuration project specific.
 * If the configuration changes, the old database entry is set to obsolete and the new entry refers to the old entry.
 */
abstract class ConfigPerProjectAndSeqType implements TimeStamped, Entity {

    static belongsTo = [
            project: Project,
            seqType: SeqType,
    ]

    Pipeline pipeline

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
        seqType nullable: true, //needs to be nullable because of old data, should never be null for new data
                validator: { val, obj ->
                    obj.obsoleteDate ? true : val != null
                }
    }

    static mapping = {
        'class' index: "config_per_project_class_idx"
        obsoleteDate index: "config_per_project_project_id_seq_type_id_obsolete_date_idx"
        seqType index: "config_per_project_project_id_seq_type_id_obsolete_date_idx"
        project index: "config_per_project_project_id_seq_type_id_obsolete_date_idx"
    }

    void createConfigPerProjectAndSeqType() {
        Project.withTransaction {
            this.previousConfig?.makeObsolete()
            assert this.save(flush: true)
        }
    }

    void makeObsolete() {
        this.obsoleteDate = new Date()
        assert this.save(flush: true)
    }
}
