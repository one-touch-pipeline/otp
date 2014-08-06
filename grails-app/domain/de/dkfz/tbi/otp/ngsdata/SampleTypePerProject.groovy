package de.dkfz.tbi.otp.ngsdata


/**
 * To receive more structure in the sample types it was decided to ask for the samples types which are expected to occur within a project.
 * These sample types are then stored in this domain per project.
 * Furthermore it is relevant to know if a sample type represents a DISEASE or a CONTROL.
 * This information will be requested via the SNV-GUI.
 *
 */
class SampleTypePerProject {

    Project project

    SampleType sampleType

    /**
     * Holds the information if the specified sampleType is a DISEASE or a CONTROL in this project.
     */
    Category category = Category.UNKNOWN

    /**
     * This property is handled automatically by grails.
     */
    Date dateCreated

    /**
     * This property is handled automatically by grails.
     */
    Date lastUpdated


    static constraints = {
        sampleType unique: 'project'
    }

    /**
     * This enum specifies if the sample type belongs to a disease or a control.
     * In the beginning this information is not available in OTP, therefore it is set to UNKNOWN
     */
    enum Category {
        UNKNOWN,
        DISEASE,
        CONTROL
    }
}
