package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class Sample implements Entity {

    static belongsTo = [
        individual : Individual,
        sampleType : SampleType
    ]
    Individual individual
    SampleType sampleType

    static constraints = {
        individual(nullable: false)
        sampleType(nullable: false, unique: 'individual')
    }

    String toString() {
        // useful for scaffolding
        "${individual?.mockPid} ${sampleType?.name}"
    }

    String getDisplayName() {
        return "${individual?.displayName} ${sampleType?.displayName}"
    }

    /**
     * @return List of SampleIdentifier for this Sample.
     **/
    List<SampleIdentifier> getSampleIdentifiers() {
        return SampleIdentifier.findAllBySample(this)
    }

    Project getProject() {
        return individual.project
    }

    /**
     * @return The category of this sample's type or <code>null</code> if it is not configured.
     */
    SampleType.Category getSampleTypeCategory() {
        return sampleType.getCategory(project)
    }

    static mapping = {
        sampleType index: "sample_sample_type_idx"
    }
}
