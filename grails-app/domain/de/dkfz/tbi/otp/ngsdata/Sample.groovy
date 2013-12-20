package de.dkfz.tbi.otp.ngsdata

class Sample {

    static belongsTo = [
        individual : Individual,
        sampleType : SampleType
    ]

    static constraints = {
        individual(nullable: false)
        sampleType(nullable: false)
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
}
