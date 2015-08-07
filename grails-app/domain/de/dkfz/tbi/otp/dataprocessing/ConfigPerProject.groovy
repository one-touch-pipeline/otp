package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript

/**
 * To be more flexible the configuration shall be stored in the database instead of in the code.
 * This domain stores the configuration project specific.
 * If the configuration changes, the old database entry is set to obsolete and the new entry refers to the old entry.
 *
 */
abstract class ConfigPerProject {

    static belongsTo = [
        project: Project
    ]

    /**
     * Defines which version of the external scripts has to be used for this project.
     */
    String externalScriptVersion

    // The following two properties are automatically maintained by Grails.
    // See http://grails.org/doc/latest/ref/Database%20Mapping/autoTimestamp.html
    Date dateCreated
    Date lastUpdated

    /**
     * When changes appear in the configuration, a new ConfigPerProject entry is created and the old entry is set to obsolete.
     */
    Date obsoleteDate
    /**
     * When a previous config files exists, it should be referred here.
     * This is needed for tracking.
     */
    ConfigPerProject previousConfig

    static constraints = {
        previousConfig nullable: true, validator: { val, obj ->
            return (val == null || val != null && val.obsoleteDate != null)
        }
        obsoleteDate nullable: true
        externalScriptVersion blank: false, validator: { val, obj ->
            if (obj.obsoleteDate) {
                !ExternalScript.findAllByScriptVersion(val).empty
            } else {
                !ExternalScript.findAllByScriptVersionAndDeprecatedDate(val, null).empty
            }
        }
    }

    static mapping = {
        project index: 'config_per_project_project_idx'
        previousConfig index: 'config_per_project_previous_config_idx'
        externalScriptVersion index: 'config_per_project_external_script_version_idx'
    }

     void createConfigPerProject() {
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
