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
        // usefulll for scaffolding
        "${individual.mockFullName} ${sampleType.name}"
    }

    /**
     * @return List of SampleIdentifier for this Sample.
     **/
    List<SampleIdentifier> getSampleIdentifiers() {
        return SampleIdentifier.findAllBySample(this)
    }
}
