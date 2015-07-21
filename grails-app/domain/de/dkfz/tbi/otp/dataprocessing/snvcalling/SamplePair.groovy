package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.utils.CollectionUtils
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
class SamplePair {

    static enum ProcessingStatus {

        /**
         * The sample pair needs to be processed as soon as conditions are satisfied (coverage is high enough etc).
         */
        NEEDS_PROCESSING,

        /**
         * The sample pair does not have to be processed, because there already is an {@link SnvCallingInstance} which
         * is up-to-date. If this is no longer true,
         * {@link SamplePair#findSamplePairsForSettingNeedsProcessing()} will find the sample
         * pair.
         */
        NO_PROCESSING_NEEDED,

        /**
         * The sample pair shall not be processed (again), for example because there was a notice from the responsible
         * bioinformatician to exclude it.
         */
        DISABLED,
    }

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

    ProcessingStatus processingStatus = ProcessingStatus.NEEDS_PROCESSING

    /**
     * These properties are handled automatically by grails.
     */
    Date dateCreated
    Date lastUpdated

    static constraints = {
        sampleType2 validator: { val, obj ->
            return val != obj.sampleType1
        }
        sampleType1 validator: { SampleType val, SamplePair obj, Errors errors ->
            if (!obj.individual) {
                errors.reject(null, 'Cannot validate sampleType1 without individual being set.')
                return
            }
            final SampleType.Category category = val.getCategory(obj.individual.project)
            if (category != SampleType.Category.DISEASE) {
                errors.reject(null, "Category of sampleType1 is ${category}. Expected ${SampleType.Category.DISEASE}.")
                return
            }
            final SamplePair sameOrderObj = obj.findForSameIndividualAndSeqType(val, obj.sampleType2)
            final SamplePair otherOrderObj = obj.findForSameIndividualAndSeqType(obj.sampleType2, val)
            if (obj.id) {
                // change an already existing object
                if ((sameOrderObj && sameOrderObj.id != obj.id) || (otherOrderObj && otherOrderObj.id != obj.id)) {
                    errors.reject(null, 'A SamplePair for that combination already exists.')
                    return
                }
            } else {
                // save a new object
                if (sameOrderObj || otherOrderObj) {
                    errors.reject(null, 'A SamplePair for that combination already exists.')
                    return
                }
            }
        }
    }

    private SamplePair findForSameIndividualAndSeqType(final SampleType sampleType1, final SampleType sampleType2) {
        return atMostOneElement(SamplePair.findAllByIndividualAndSeqTypeAndSampleType1AndSampleType2(individual, seqType, sampleType1, sampleType2))
    }

    static mapping = {
        individual       index: 'sample_pair_idx1,sample_pair_idx2'
        sampleType1      index: 'sample_pair_idx1,sample_pair_idx2'
        sampleType2      index: 'sample_pair_idx1,sample_pair_idx2'
        seqType          index: 'sample_pair_idx1,sample_pair_idx2'
        processingStatus index: 'sample_pair_idx1'
    }

    Project getProject() {
        return individual.project
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control
     */
    OtpPath getSamplePairPath() {
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
            return new OtpPath(samplePairPath, step.getResultFileName(individual, null))
        } else if (step == SnvCallingStep.FILTER_VCF) {
            return samplePairPath
        } else {
            return new OtpPath(samplePairPath, step.getResultFileName(individual))
        }
    }

    OtpPath getIndexFileLinkedPath(SnvCallingStep step) {
        return new OtpPath(samplePairPath, step.getIndexFileName(individual))
    }

    String toStringWithoutId() {
        return "${individual.pid} ${sampleType1.name} ${sampleType2.name} ${seqType.name} ${seqType.libraryLayout}"
    }

    @Override
    String toString() {
        return "SP ${id}: ${toStringWithoutId()}"
    }

    /**
     * Returns the latest AbstractMergedBamFile which belongs to the given {@link SampleType}
     * for this {@link SamplePair}, if available and not withdrawn, otherwise null.
     */
    AbstractMergedBamFile getAbstractMergedBamFileInProjectFolder(SampleType sampleType) {
        final AbstractMergedBamFile bamFile = CollectionUtils.atMostOneElement(
            MergingWorkPackage.createCriteria().list {
                eq ("seqType", seqType)
                sample {
                    eq ("sampleType", sampleType)
                    eq ("individual", individual)
                }
            }
        )?.bamFileInProjectFolder

        if (bamFile && !bamFile.withdrawn && bamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED) {
            return bamFile
        } else {
            return null
        }
    }

    /**
     * Finds distinct combinations of [individual, sampleType1, sampleType2, seqType] with these criteria:
     * <ul>
     *     <li>A pair of non-withdrawn SeqTracks exists for that combination and sampleType1 has category
     *         {@link SampleType.Category#DISEASE} and sampleType2 has category {@link SampleType.Category#CONTROL}.</li>
     *     <li>New {@link DataFile}s were added for the individual since <code>minDate</code>.</li>
     *     <li>The seqType is processable by OTP.</li>
     *     <li>No SamplePair exists for that combination.</li>
     * </ul>
     * The results are returned as SamplePair instances, <em>which have not been persisted yet</em>.
     */
    static Collection<SamplePair> findMissingDiseaseControlSamplePairs(final Date minDate) {
        final Collection queryResults = SamplePair.executeQuery("""
            SELECT DISTINCT
              individual_1,
              sampleType_1,
              sampleType_2,
              seqType_1
            FROM
              SeqTrack st1
                join st1.sample.individual individual_1
                join individual_1.project project_1
                join st1.sample.sampleType sampleType_1
                join st1.seqType seqType_1,
              SeqTrack st2
                join st2.sample.individual individual_2
                join st2.sample.sampleType sampleType_2
                join st2.seqType seqType_2,
              SampleTypePerProject stpp1,
              SampleTypePerProject stpp2
            WHERE
              seqType_1 IN (:seqTypes) AND
              seqType_2 = seqType_1 AND
              individual_2 = individual_1 AND
              stpp1.project = project_1 AND
              stpp2.project = project_1 AND
              stpp1.sampleType = sampleType_1 AND
              stpp2.sampleType = sampleType_2 AND
              stpp1.category = :disease AND
              stpp2.category = :control AND
              EXISTS (FROM DataFile WHERE seqTrack.sample.individual = individual_1 AND dateCreated >= :minDate) AND
              NOT EXISTS (FROM DataFile WHERE seqTrack = st1 AND fileType.type = :fileType AND fileWithdrawn = true) AND
              NOT EXISTS (FROM DataFile WHERE seqTrack = st2 AND fileType.type = :fileType AND fileWithdrawn = true) AND
              NOT EXISTS (
                FROM
                  SamplePair
                WHERE
                  individual = individual_1 AND
                  sampleType1 = sampleType_1 AND
                  sampleType2 = sampleType_2 AND
                  seqType = seqType_1)
            """, [
                seqTypes: SeqTypeService.alignableSeqTypes(),
                disease: SampleType.Category.DISEASE,
                control: SampleType.Category.CONTROL,
                minDate: minDate,
                fileType: FileType.Type.SEQUENCE
            ], [readOnly: true])
        return queryResults.collect {
            new SamplePair(
                individual: it[0],
                sampleType1: it[1],
                sampleType2: it[2],
                seqType: it[3],
            )
        }
    }

    /**
     * Finds existing SamplePairs with these criteria:
     * <ul>
     *     <li>{@link #processingStatus} is set to {@link ProcessingStatus#NO_PROCESSING_NEEDED}.</li>
     *     <li>{@link #sampleType1} (still) has category {@link SampleType.Category#DISEASE}.</li>
     *     <li>No {@link SnvCallingInstance} exists which belongs to the sample pair and fulfills these criteria:
     *     <ul>
     *         <li>The BAM files that the {@link SnvCallingInstance} is based on contain all non-withdrawn
     *             {@link DataFile}s matching the SamplePair.</li>
     *         <li>No {@link SnvJobResult} belonging to the {@link SnvCallingInstance} is withdrawn.</li>
     *     </ul>
     * </ul>
     */
    static Collection<SamplePair> findSamplePairsForSettingNeedsProcessing() {
        return SamplePair.executeQuery("""
            FROM
              SamplePair sp
            WHERE
              sp.processingStatus = :noProcessingNeeded AND
              EXISTS (
                FROM
                  SampleTypePerProject
                WHERE
                  project = sp.individual.project AND
                  sampleType = sp.sampleType1 AND
                  category = :disease
              ) AND
              NOT EXISTS (
                FROM
                  SnvCallingInstance sci
                WHERE
                  sci.samplePair = sp AND
                  NOT EXISTS (
                    FROM
                      SnvJobResult
                    WHERE
                      snvCallingInstance = sci AND
                      withdrawn = true
                  ) AND
                  latestDataFileCreationDate >= (
                    SELECT
                      MAX(dateCreated)
                    FROM
                      DataFile
                    WHERE
                      fileType.type = :fileType AND
                      fileWithdrawn = false AND
                      seqTrack.sample.individual = sp.individual AND
                      seqTrack.sample.sampleType IN (sp.sampleType1, sp.sampleType2) AND
                      seqTrack.seqType = sp.seqType
                  )
              )
            """, [
                noProcessingNeeded: ProcessingStatus.NO_PROCESSING_NEEDED,
                disease: SampleType.Category.DISEASE,
                fileType: FileType.Type.SEQUENCE
        ], [readOnly: true])
    }

    /**
     * Sets {@link #processingStatus} of all specified instances to the specified value and saves the instances.
     */
    static void setProcessingStatus(final Collection<SamplePair> samplePairs,
                                    final ProcessingStatus processingStatus) {
        if (processingStatus == null) {
            throw new IllegalArgumentException()
        }
        SamplePair.withTransaction {
            samplePairs.each {
                it.processingStatus = processingStatus
                assert it.save()
            }
        }
    }
}
