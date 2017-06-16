package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.utils.*

class SampleIdentifier implements Entity {

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
                    regexFromProcessingOption = ProcessingOptionService.findOption(OptionName.VALIDATOR_SAMPLE_IDENTIFIER_REGEX, null, obj.sample?.project)
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
