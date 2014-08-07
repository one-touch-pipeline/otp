package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.*

/**
 * For each individual disease/control pairs are compared in the SNV pipeline. These pairs are defined in the GUI and stored in this domain.
 * The sampleTypes which have to be compared differ between the individuals, which is why it has to be specific for an individual.
 * It can happen that not only disease and control shall be compared but also disease&disease.
 * This is why the properties are not call disease and control.
 * The sample pairs can also be used for other purposes i.e. coverage combination between disease and control
 *
 */
class SampleTypeCombinationPerIndividual {

    /**
     * The sampleType pair shall be processed for this individual
     */
    Individual individual

    /**
     * The samples of these two sample types will be compared within the snv pipeline.
     */
    SampleType sampleType1
    SampleType sampleType2

    /**
     * These properties are handled automatically by grails.
     */
    Date dateCreated
    Date lastUpdated

    static constraints = {
        sampleType1 validator: {val, obj ->
            return (SampleTypeCombinationPerIndividual.findAllByIndividualAndSampleType1AndSampleType2(obj.individual, obj.sampleType2, val).isEmpty() &&
            SampleTypeCombinationPerIndividual.findAllByIndividualAndSampleType1AndSampleType2(obj.individual, val, obj.sampleType2).isEmpty())
        }
        sampleType2 validator: { val, obj ->
            return val != obj.sampleType1
        }


    }
}
