package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
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
        sampleType2 validator: { val, obj ->
            return val != obj.sampleType1
        }
        sampleType1 validator: { val, obj ->
            final SampleTypeCombinationPerIndividual sameOrderObj = obj.findForSameIndividualAndSeqType(val, obj.sampleType2)
            final SampleTypeCombinationPerIndividual otherOrderObj = obj.findForSameIndividualAndSeqType(obj.sampleType2, val)
            if (obj.id) {
                // change an already existing object
                return (!sameOrderObj || sameOrderObj.id == obj.id) && (!otherOrderObj || otherOrderObj.id == obj.id)
            } else {
                // save a new object
                return !sameOrderObj && !otherOrderObj
            }
        }
    }

    private SampleTypeCombinationPerIndividual findForSameIndividualAndSeqType(final SampleType sampleType1, final SampleType sampleType2) {
        return atMostOneElement(SampleTypeCombinationPerIndividual.findAllByIndividualAndSeqTypeAndSampleType1AndSampleType2(individual, seqType, sampleType1, sampleType2))
    }

    static mapping = {
        individual index: 'sample_type_combination_per_individual_individual_idx'
        sampleType1 index: 'sample_type_combination_per_individual_sample_type1_idx'
        sampleType2 index: 'sample_type_combination_per_individual_sample_type2_idx'
        seqType index: 'sample_type_combination_per_individual_seq_type_idx'
    }

    Project getProject() {
        return individual.project
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control
     */
    OtpPath getSampleTypeCombinationPath() {
        return new OtpPath(individual.getViewByPidPath(seqType), 'snv_results', seqType.libraryLayoutDirName, "${sampleType1.dirName}_${sampleType2.dirName}")
    }

    /**
     * Returns the path of the symlink to the result file for the CALLING step of the SNV pipeline.
     *
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/snvs_${pid}_raw.vcf.gz
     */
    OtpPath getResultFileLinkedPath(SnvCallingStep step) {
        if (step == SnvCallingStep.CALLING) {
            return new OtpPath(sampleTypeCombinationPath, step.getResultFileName(individual, null))
        } else if (step == SnvCallingStep.FILTER_VCF) {
            throw new UnsupportedOperationException("TODO -> OTP-989")
        } else {
            return new OtpPath(sampleTypeCombinationPath, step.getResultFileName(individual))
        }
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
