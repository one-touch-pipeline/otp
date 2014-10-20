package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

import groovy.transform.ToString

@ToString()
class SampleType {

    /**
     * This enum specifies if the sample type belongs to a disease or a control.
     * In the beginning this information is not available in OTP, therefore it is set to UNKNOWN
     *
     * @see #getCategory(Project)
     */
    enum Category {
        UNKNOWN,  // TODO: OTP-1169
        DISEASE,
        CONTROL
    }

    String name
    static constraints = {
        name(unique: true)
        // TODO: OTP-1122: unique constraint for dirName
    }

    String getDirName() {
        return name.toLowerCase()
    }

    /**
     * @return The category of this sample type or <code>null</code> if it is not configured.
     */
    Category getCategory(final Project project) {
        assert project
        return atMostOneElement(SampleTypePerProject.findAllWhere(project: project, sampleType: this))?.category
    }
}
