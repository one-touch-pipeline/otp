package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.utils.Entity
import org.springframework.validation.Errors
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * For each individual disease/control pairs are compared in the SNV pipeline. These pairs are defined in the GUI and stored in this domain.
 * The sampleTypes which have to be compared differ between the individuals, which is why it has to be specific for an individual.
 * It can happen that not only disease and control shall be compared but also disease&disease.
 * This is why the properties are not call disease and control.
 * The sample pairs can also be used for other purposes i.e. coverage combination between disease and control
 *
 */
class SamplePair implements Entity {

    /**
     * Creates an instance. Does <em>not</em> persist it.
     */
    static SamplePair createInstance(Map properties) {
        SamplePair samplePair = new SamplePair(properties)
        samplePair.relativePath = samplePair.buildPath().relativePath.path
        return samplePair
    }

    static enum ProcessingStatus {

        /**
         * The sample pair needs to be processed as soon as conditions are satisfied (coverage is high enough etc).
         */
        NEEDS_PROCESSING,

        /**
         * The sample pair does not have to be processed, because there already is an {@link SnvCallingInstance} which
         * is up-to-date.
         */
        NO_PROCESSING_NEEDED,

        /**
         * The sample pair shall not be processed (again), for example because there was a notice from the responsible
         * bioinformatician to exclude it.
         */
        DISABLED,
    }

    MergingWorkPackage mergingWorkPackage1
    MergingWorkPackage mergingWorkPackage2

    String relativePath

    ProcessingStatus processingStatus = ProcessingStatus.NEEDS_PROCESSING

    /**
     * These properties are handled automatically by grails.
     */
    Date dateCreated
    Date lastUpdated

    static constraints = {
        mergingWorkPackage1 validator: { MergingWorkPackage val, SamplePair obj, Errors errors ->
            final SampleType.Category category = val.sampleType.getCategory(obj.project)
            if (category != SampleType.Category.DISEASE) {
                errors.reject(null, "Category of mergingWorkPackage1.sampleType is ${category}. Expected ${SampleType.Category.DISEASE}.")
            }
        }
        mergingWorkPackage2 unique: 'mergingWorkPackage1', validator: { MergingWorkPackage val, SamplePair obj, Errors errors ->
            if (val == obj.mergingWorkPackage1) {
                errors.reject(null, "mergingWorkPackage1 and mergingWorkPackage2 are equal.")
            }
            // For one sample pair the individual, the seqType and the pipeline must be the same.
            // To provide the possibility to create sample pairs manually other properties are ignored here.
            ['individual', 'seqType', 'pipeline'].each {
                def mwp1Value = obj.mergingWorkPackage1."${it}"
                def mwp2Value = val."${it}"
                if (mwp1Value != mwp2Value && !(mwp1Value?.hasProperty('id') && mwp2Value?.hasProperty('id') && mwp1Value.id == mwp2Value.id)) {
                    errors.reject(null, "${it} of mergingWorkPackage1 and mergingWorkPackage2 are different:\n${mwp1Value}\n${mwp2Value}")
                }
            }
        }
        relativePath unique: true, validator: { String val, SamplePair obj ->
            return OtpPath.isValidRelativePath(val) && val == obj.buildPath().relativePath.path
        }
    }

    static mapping = {
        /**
         * sample_pair_idx1 is used by SnvCallingService.samplePairForSnvProcessing.
         * processing_status must be the first column in this index! Grails does not provide a means to specify this, so
         * this must be done via SQL.
         *
         * The implicit unique index on (mergingWorkPackage1, mergingWorkPackage2) is used by
         * SamplePair.findMissingDiseaseControlSamplePairs.
         */
        mergingWorkPackage1 index: 'sample_pair_idx1'
        mergingWorkPackage2 index: 'sample_pair_idx1'
        processingStatus index: 'sample_pair_idx1'
    }

    Project getProject() {
        return individual.project
    }

    Individual getIndividual() {
        assert mergingWorkPackage1.individual.id == mergingWorkPackage2.individual.id
        return mergingWorkPackage1.individual
    }

    SeqType getSeqType() {
        assert mergingWorkPackage1.seqType.id == mergingWorkPackage2.seqType.id
        return mergingWorkPackage1.seqType
    }

    SampleType getSampleType1() {
        return mergingWorkPackage1.sampleType
    }

    SampleType getSampleType2() {
        return mergingWorkPackage2.sampleType
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control
     */
    OtpPath getSamplePairPath() {
        OtpPath path = buildPath()
        assert relativePath == path.relativePath.path
        return path
    }

    private OtpPath buildPath() {
        return new OtpPath(individual.getViewByPidPath(seqType), 'snv_results', seqType.libraryLayoutDirName, "${sampleType1.dirName}_${sampleType2.dirName}")
    }

    String toStringWithoutId() {
        return "${individual.pid} ${sampleType1.name} ${sampleType2.name} ${seqType.name} ${seqType.libraryLayout}"
    }

    @Override
    String toString() {
        return "SP ${id}: ${toStringWithoutId()}"
    }

    SnvCallingInstance findLatestSnvCallingInstance() {
        return SnvCallingInstance.createCriteria().get {
            eq ('samplePair', this)
            order('id', 'desc')
            maxResults(1)
        }
    }

    /**
     * The names of the properties of {@link #mergingWorkPackage1} and {@link #mergingWorkPackage2} which must be equal.
     */
    static final Collection<String> mergingWorkPackageEqualProperties =
            (MergingWorkPackage.seqTrackPropertyNames - ['sample', 'libraryPreparationKit'] + MergingWorkPackage.processingParameterNames).asImmutable()

    /**
     * Finds distinct combinations of [mergingWorkPackage1, mergingWorkPackage2] with these criteria:
     * <ul>
     *     <li>mergingWorkPackage1 and mergingWorkPackage2 differ only in their sampleType.</li>
     *     <li>mergingWorkPackage1.sampleType has category {@link SampleType.Category#DISEASE} and
     *         mergingWorkPackage2.sampleType has category {@link SampleType.Category#CONTROL}.</li>
     *     <li>No SamplePair exists for that combination.</li>
     * </ul>
     * The results are returned as SamplePair instances, <em>which have not been persisted yet</em>.
     */
    static Collection<SamplePair> findMissingDiseaseControlSamplePairs() {
        final Collection queryResults = executeQuery("""
            SELECT DISTINCT
              mwp1,
              mwp2
            FROM
              MergingWorkPackage mwp1
                join mwp1.sample.individual.project project_1
                join mwp1.sample.sampleType sampleType_1,
              MergingWorkPackage mwp2
                join mwp2.sample.sampleType sampleType_2,
              SampleTypePerProject stpp1,
              SampleTypePerProject stpp2
            WHERE
              ${(mergingWorkPackageEqualProperties + ['sample.individual']).collect{
                  "(mwp1.${it} = mwp2.${it} OR mwp1.${it} IS NULL AND mwp2.${it} IS NULL)"
              }.join(' AND\n')} AND
              stpp1.project = project_1 AND
              stpp2.project = project_1 AND
              stpp1.sampleType = sampleType_1 AND
              stpp2.sampleType = sampleType_2 AND
              stpp1.category = :disease AND
              stpp2.category = :control AND
              (mwp1.libraryPreparationKit = mwp2.libraryPreparationKit OR
              mwp1.libraryPreparationKit IS NULL AND mwp2.libraryPreparationKit IS NULL OR
              mwp1.seqType.name in (:mwpLibPrepKitsMayMismatchSeqTypeNames)) AND
              NOT EXISTS (
                FROM
                  SamplePair
                WHERE
                  mergingWorkPackage1 = mwp1 AND
                  mergingWorkPackage2 = mwp2)
            """, [
                disease: SampleType.Category.DISEASE,
                control: SampleType.Category.CONTROL,
                mwpLibPrepKitsMayMismatchSeqTypeNames: (SeqType.WGBS_SEQ_TYPE_NAMES + SeqTypeNames.WHOLE_GENOME)*.seqTypeName,
            ], [readOnly: true])
        return queryResults.collect {
            return createInstance(
                    mergingWorkPackage1: it[0],
                    mergingWorkPackage2: it[1]
            )
        }
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
