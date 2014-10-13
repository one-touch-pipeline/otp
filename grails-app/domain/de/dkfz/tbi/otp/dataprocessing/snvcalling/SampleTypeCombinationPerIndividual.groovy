package de.dkfz.tbi.otp.dataprocessing.snvcalling

import org.springframework.validation.Errors
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
        sampleType1 validator: { SampleType val, SampleTypeCombinationPerIndividual obj, Errors errors ->
            if (!obj.individual) {
                errors.reject(null, 'Cannot validate sampleType1 without individual being set.')
                return
            }
            final SampleType.Category category = val.getCategory(obj.individual.project)
            if (category != SampleType.Category.DISEASE) {
                errors.reject(null, "Category of sampleType1 is ${category}. Expected ${SampleType.Category.DISEASE}.")
                return
            }
            final SampleTypeCombinationPerIndividual sameOrderObj = obj.findForSameIndividualAndSeqType(val, obj.sampleType2)
            final SampleTypeCombinationPerIndividual otherOrderObj = obj.findForSameIndividualAndSeqType(obj.sampleType2, val)
            if (obj.id) {
                // change an already existing object
                if ((sameOrderObj && sameOrderObj.id != obj.id) || (otherOrderObj && otherOrderObj.id != obj.id)) {
                    errors.reject(null, 'A SampleTypeCombinationPerIndividual for that combination already exists.')
                    return
                }
            } else {
                // save a new object
                if (sameOrderObj || otherOrderObj) {
                    errors.reject(null, 'A SampleTypeCombinationPerIndividual for that combination already exists.')
                    return
                }
            }
        }
    }

    private SampleTypeCombinationPerIndividual findForSameIndividualAndSeqType(final SampleType sampleType1, final SampleType sampleType2) {
        return atMostOneElement(SampleTypeCombinationPerIndividual.findAllByIndividualAndSeqTypeAndSampleType1AndSampleType2(individual, seqType, sampleType1, sampleType2))
    }

    static mapping = {
        individual      index: 'sample_type_combination_per_individual_idx1,sample_type_combination_per_individual_idx2'
        sampleType1     index: 'sample_type_combination_per_individual_idx1,sample_type_combination_per_individual_idx2'
        sampleType2     index: 'sample_type_combination_per_individual_idx1,sample_type_combination_per_individual_idx2'
        seqType         index: 'sample_type_combination_per_individual_idx1,sample_type_combination_per_individual_idx2'
        needsProcessing index: 'sample_type_combination_per_individual_idx1'
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
     * Returns the path of the symlink to the result file for current step of the SNV pipeline.
     * !Attention: since there is more then one result for the filter step,
     * and it is possible that even more results will come up, only the path to these results are returned in case of
     * the filter step
     *
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/snvs_${pid}_raw.vcf.gz
     */
    OtpPath getResultFileLinkedPath(SnvCallingStep step) {
        if (step == SnvCallingStep.CALLING) {
            return new OtpPath(sampleTypeCombinationPath, step.getResultFileName(individual, null))
        } else if (step == SnvCallingStep.FILTER_VCF) {
            return sampleTypeCombinationPath
        } else {
            return new OtpPath(sampleTypeCombinationPath, step.getResultFileName(individual))
        }
    }

    OtpPath getIndexFileLinkedPath(SnvCallingStep step) {
        return new OtpPath(sampleTypeCombinationPath, step.getIndexFileName(individual))
    }

    @Override
    String toString() {
        return "STCPI ${id} ${individual.pid} ${sampleType1.name} ${sampleType2.name} ${seqType.name} ${seqType.libraryLayout}"
    }

    /**
     * Returns the latest ProcessedMergedBamFile which belongs to the given {@link SampleType}
     * for this {@link SampleTypeCombinationPerIndividual}, if available and not withdrawn, otherwise null.
     */
    ProcessedMergedBamFile getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(SampleType sampleType) {
        final ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.createCriteria().get {
            mergingPass {
                mergingSet {
                    mergingWorkPackage {
                        eq ("seqType", seqType)
                        sample {
                            eq ("sampleType", sampleType)
                            eq ("individual", individual)
                        }
                    }
                    order("identifier", "desc")
                }
                order("identifier", "desc")
            }
            maxResults(1)
        }
        if (bamFile && !bamFile.withdrawn) {
            return bamFile
        } else {
            return null
        }
    }

    /**
     * Finds distinct combinations of [individual, sampleType1, sampleType2, seqType] with this criteria:
     * <ul>
     *     <li>A pair of non-withdrawn SeqTracks exists for that combination and sampleType1 has category
     *         {@link SampleType.Category#DISEASE} and sampleType2 has category {@link SampleType.Category#CONTROL}.</li>
     *     <li>The seqType is processable by OTP.</li>
     *     <li>No SampleTypeCombinationPerIndividual exists for that combination.</li>
     * </ul>
     * The results are returned as SampleTypeCombinationPerIndividual instances, <em>which have not been persisted yet</em>.
     */
    static Collection<SampleTypeCombinationPerIndividual> findMissingDiseaseControlCombinations() {
        final Collection queryResults = SampleTypeCombinationPerIndividual.executeQuery("""
            SELECT DISTINCT
              st1.sample.individual,
              st1.sample.sampleType,
              st2.sample.sampleType,
              st1.seqType
            FROM
              SeqTrack st1,
              SeqTrack st2,
              SampleTypePerProject stpp1,
              SampleTypePerProject stpp2
            WHERE
              st1.seqType IN (:seqTypes) AND
              st2.seqType = st1.seqType AND
              st2.sample.individual = st1.sample.individual AND
              stpp1.project = st1.sample.individual.project AND
              stpp2.project = st1.sample.individual.project AND
              stpp1.sampleType = st1.sample.sampleType AND
              stpp2.sampleType = st2.sample.sampleType AND
              stpp1.category = :disease AND
              stpp2.category = :control AND
              NOT EXISTS (FROM DataFile WHERE seqTrack = st1 AND fileType.type = :fileType AND fileWithdrawn = true) AND
              NOT EXISTS (FROM DataFile WHERE seqTrack = st2 AND fileType.type = :fileType AND fileWithdrawn = true) AND
              NOT EXISTS (
                FROM
                  SampleTypeCombinationPerIndividual
                WHERE
                  individual = st1.sample.individual AND
                  sampleType1 = st1.sample.sampleType AND
                  sampleType2 = st2.sample.sampleType AND
                  seqType = st1.seqType)
            """, [
                seqTypes: SeqTypeService.alignableSeqTypes(),
                disease: SampleType.Category.DISEASE,
                control: SampleType.Category.CONTROL,
                fileType: FileType.Type.SEQUENCE
            ], [readOnly: true])
        return queryResults.collect {
            new SampleTypeCombinationPerIndividual(
                individual: it[0],
                sampleType1: it[1],
                sampleType2: it[2],
                seqType: it[3],
            )
        }
    }

    /**
     * Sets {@link #needsProcessing} of all specified instances to the specified value and saves the instances.
     */
    static void setNeedsProcessing(final Collection<SampleTypeCombinationPerIndividual> combinations, final boolean needsProcessing) {
        SampleTypeCombinationPerIndividual.withTransaction {
            combinations.each {
                it.needsProcessing = needsProcessing
                assert it.save()
            }
        }
    }
}
