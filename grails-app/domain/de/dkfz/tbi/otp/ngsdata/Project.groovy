package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority

class Project {
    String name
    String dirName
    String realmName

    String emailAddressOfContactPerson

    short processingPriority = ProcessingPriority.NORMAL_PRIORITY
    String alignmentDeciderBeanName

    static belongsTo = [
        projectGroup: ProjectGroup
    ]

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false)
        realmName(blank: false)
        projectGroup(nullable: true)
        emailAddressOfContactPerson (nullable: true)
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
