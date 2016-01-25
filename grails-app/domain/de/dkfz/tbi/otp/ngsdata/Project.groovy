package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority

class Project {
    String name
    String dirName
    String realmName

    short processingPriority = ProcessingPriority.NORMAL_PRIORITY
    String alignmentDeciderBeanName

    /**
     * this flag defines if the fastq files of this project have to be copied (instead of linked) regardless of whether
     * they will be processed or not
     */
    boolean hasToBeCopied = false

    static belongsTo = [
        projectGroup: ProjectGroup
    ]

    static hasMany =  [
        contactPersons: ContactPerson
    ]

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false, unique: true, validator: { String val ->
            OtpPath.isValidRelativePath(val) &&
                    ['icgc', 'dkfzlsdf', 'lsdf', 'project'].every { !val.startsWith("${it}/") }
        })
        realmName(blank: false)
        projectGroup(nullable: true)
        processingPriority max: ProcessingPriority.MAXIMUM_PRIORITY
        alignmentDeciderBeanName(blank: false)  // If no alignment is desired, set to noAlignmentDecider instead of leaving blank
    }

    String toString() {
        name
    }

    static mapping = {
        projectGroup index: "project_project_group_idx"
        processingPriority index: "project_processing_priority_idx"
    }
}
