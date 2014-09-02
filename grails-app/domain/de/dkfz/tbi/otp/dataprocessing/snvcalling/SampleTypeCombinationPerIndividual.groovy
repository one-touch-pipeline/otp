package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

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

    SeqType seqType

    /**
     * This flag shows that a pair of two sample types has to be processed.
     * When creating an SampleTypeCombinationPerIndividual instance for one pair it is set to true per default.
     * To rerun the processing for one pair it has to be set to true again.
     */
    boolean needsProcessing = true

    /**
     * These properties are handled automatically by grails.
     */
    Date dateCreated
    Date lastUpdated

    static constraints = {
        sampleType1 validator: {val, obj ->
            return (SampleTypeCombinationPerIndividual.findAllByIndividualAndSampleType1AndSampleType2AndSeqType(obj.individual, obj.sampleType2, val, obj.seqType).isEmpty() &&
            SampleTypeCombinationPerIndividual.findAllByIndividualAndSampleType1AndSampleType2AndSeqType(obj.individual, val, obj.sampleType2, obj.seqType).isEmpty())
        }
        sampleType2 validator: { val, obj ->
            return val != obj.sampleType1
        }
    }

    Project getProject() {
        return individual.project
    }

    /**
     * Returns the latest ProcessedMergedBamFile which belongs to the given {@link SampleType}
     * for this {@link SampleTypeCombinationPerIndividual}, if available and not withdrawn, otherwise null.
     */
    ProcessedMergedBamFile getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(SampleType sampleType) {
        return atMostOneElement(ProcessedMergedBamFile.createCriteria().list {
            eq("withdrawn", false)
            mergingPass {
                mergingSet {
                    mergingWorkPackage {
                        eq ("seqType", seqType)
                        sample {
                            eq ("sampleType", sampleType)
                            eq ("individual", individual)
                        }
                    }
                }
            }
        }?.findAll {it.isMostRecentBamFile() })
    }
}
