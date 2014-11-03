package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

import groovy.transform.ToString

@ToString()
class SampleType {

    /**
     * This enum specifies if the sample type belongs to a disease or a control.
     *
     * @see #getCategory(Project)
     */
    enum Category {
        /**
         * Sample types with their category configured as IGNORED should be silently ignored and not be processed by
         * workflows for which the category is relevant.
         * In contrast, if no category is configured (i.e. no {@link SampleTypePerProject} instance exists for a
         * combination of project and sample type), such workflows should warn about the sample type category being
         * unknown.
         */
        IGNORED,
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
