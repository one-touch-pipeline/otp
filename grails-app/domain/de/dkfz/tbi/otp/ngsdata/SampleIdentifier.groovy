package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

class SampleIdentifier {

    static final REGEX_OPTION_NAME = 'sampleIdentifierRegex'

    String name
    Sample sample
    static belongsTo = [sample: Sample]

    static constraints = {
        name(unique: true, nullable: false,blank: false, minSize: 3,
            validator: { String val, SampleIdentifier obj ->
                //should neither start nor end with a space
                if (val.startsWith(' ') || val.endsWith(' ')) {
                    return false
                }
                String regexFromProcessingOption
                // Using a new session prevents Hibernate from trying to auto-flush this object, which would fail
                // because it is still in validation.
                withNewSession { session ->
                    regexFromProcessingOption = ProcessingOptionService.findOption(REGEX_OPTION_NAME, null, obj.sample?.project)
                }
                if (!(val ==~ (regexFromProcessingOption ?: '.+'))) {
                    return false
                }
                return true
            }
        )
        sample()
    }

    Project getProject() {
        return sample.project
    }

    Individual getIndividual() {
        return sample.individual
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    String toString() {
        name
    }

    static mapping = {
        sample index: "sample_identifier_sample_idx"
    }
}
