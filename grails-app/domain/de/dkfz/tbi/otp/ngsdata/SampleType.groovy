package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import groovy.transform.ToString

@ToString()
class SampleType implements Entity {

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

    /**
     * enum to define, if the  {@link Project} default or a {@link SampleType} specific {@link ReferenceGenome} should be used.
     *
     *
     */
    enum SpecificReferenceGenome {
        /**
         * For this {@link SampleType} the {@link Project} {@link SeqType} default {@link ReferenceGenome} should be used.
         */
        USE_PROJECT_DEFAULT,
        /**
         * For this {@link SampleType} the {@link Project} {@link SeqType} {@link SampleType} specific {@link ReferenceGenome} should be used.
         */
        USE_SAMPLE_TYPE_SPECIFIC,
        /**
         * For this {@link SampleType} it is not defined yet. It should be used only for automatically created {@link SampleType}s
         * and needs to be changed later.
         */
        UNKNOWN
    }

    String name

    SpecificReferenceGenome specificReferenceGenome = SpecificReferenceGenome.UNKNOWN

    static constraints = {
        name(unique: true, validator: { OtpPath.isValidPathComponent(it) && !it.contains('_') })
        // TODO: OTP-1122: unique constraint for dirName
    }

    String getDirName() {
        return name.toLowerCase()
    }

    String getDisplayName() {
        return name
    }

    /**
     * @return The category of this sample type or <code>null</code> if it is not configured.
     */
    Category getCategory(final Project project) {
        assert project
        return atMostOneElement(SampleTypePerProject.findAllWhere(project: project, sampleType: this))?.category
    }
}
