package de.dkfz.tbi.otp.ngsdata

class Sample {

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
        "${individual.mockPid} ${sampleType.name}"
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
