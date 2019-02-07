package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.TimeStamped
import de.dkfz.tbi.otp.utils.Entity
import groovy.transform.ToString


/**
 * To receive more structure in the sample types it was decided to ask for the samples types which are expected to occur within a project.
 * These sample types are then stored in this domain per project.
 * Furthermore it is relevant to know if a sample type represents a DISEASE or a CONTROL.
 * This information will be requested via the SNV-GUI.
 */
@ToString(excludes=['dateCreated', 'lastUpdated'], includePackage = false)
class SampleTypePerProject implements TimeStamped, Entity {

    Project project

    SampleType sampleType

    /**
     * Holds the information if the specified sampleType is a DISEASE or a CONTROL in this project.
     */
    SampleType.Category category


    static constraints = {
        sampleType unique: 'project'
    }
}
