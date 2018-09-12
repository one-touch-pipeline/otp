package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.TimeStamped
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.validation.*

/**
 * For each individual disease/control pairs are compared in the analysis pipelines. These pairs are defined in the GUI and stored in this domain.
 * The sampleTypes which have to be compared differ between the individuals, which is why it has to be specific for an individual.
 * It can happen that not only disease and control shall be compared but also disease&disease.
 * This is why the properties are not call disease and control.
 * The sample pairs can also be used for other purposes i.e. coverage combination between disease and control
 */
class SamplePair implements TimeStamped, Entity {

    final static String SNV_RESULTS_PATH_PART = 'snv_results'
    final static String INDEL_RESULTS_PATH_PART = 'indel_results'
    final static String SOPHIA_RESULTS_PATH_PART = 'sv_results'
    final static String ACESEQ_RESULTS_PATH_PART = 'cnv_results'
    final static String RUN_YAPSA_RESULTS_PATH_PART = 'mutational_signatures_results'

    /**
     * Creates an instance. Does <em>not</em> persist it.
     */
    static SamplePair createInstance(Map properties) {
        SamplePair samplePair = new SamplePair(properties)
        samplePair.relativePath = samplePair.buildPath(SNV_RESULTS_PATH_PART).relativePath.path
        return samplePair
    }

    static enum ProcessingStatus {

        /**
         * The sample pair needs to be processed as soon as conditions are satisfied (coverage is high enough etc).
         */
        NEEDS_PROCESSING,

        /**
         * The sample pair does not have to be processed, because there already is an {@link BamFilePairAnalysis} which
         * is up-to-date.
         */
        NO_PROCESSING_NEEDED,

        /**
         * The sample pair shall not be processed (again), for example because there was a notice from the responsible
         * bioinformatician to exclude it.
         */
        DISABLED,
    }

    AbstractMergingWorkPackage mergingWorkPackage1
    AbstractMergingWorkPackage mergingWorkPackage2

    String relativePath

    ProcessingStatus snvProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus indelProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus sophiaProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus aceseqProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus runYapsaProcessingStatus = ProcessingStatus.NEEDS_PROCESSING

    boolean isProcessingDisabled() {
        return  snvProcessingStatus      == ProcessingStatus.DISABLED &&
                indelProcessingStatus    == ProcessingStatus.DISABLED &&
                sophiaProcessingStatus   == ProcessingStatus.DISABLED &&
                aceseqProcessingStatus   == ProcessingStatus.DISABLED &&
                runYapsaProcessingStatus == ProcessingStatus.DISABLED
    }

    static constraints = {
        mergingWorkPackage1 validator: { AbstractMergingWorkPackage val, SamplePair obj, Errors errors ->
            final SampleType.Category category = val.sampleType.getCategory(obj.project)
            if (category != SampleType.Category.DISEASE) {
                errors.reject(null, "Category of mergingWorkPackage1.sampleType is ${category}. Expected ${SampleType.Category.DISEASE}.")
            }
        }
        mergingWorkPackage2 unique: 'mergingWorkPackage1', validator: { AbstractMergingWorkPackage val, SamplePair obj, Errors errors ->
            if (val == obj.mergingWorkPackage1) {
                errors.reject(null, "mergingWorkPackage1 and mergingWorkPackage2 are equal.")
            }
            // For one sample pair the individual, the seqType and the pipeline must be the same.
            // To provide the possibility to create sample pairs manually other properties are ignored here.
            ['individual', 'seqType'].each {
                def mwp1Value = obj.mergingWorkPackage1."${it}"
                def mwp2Value = val."${it}"
                if (mwp1Value != mwp2Value) {
                    errors.reject(null, "${it} of mergingWorkPackage1 and mergingWorkPackage2 are different:\n${mwp1Value}\n${mwp2Value}")
                }
            }
        }
        relativePath unique: true, validator: { String val, SamplePair obj ->
            return OtpPath.isValidRelativePath(val) && val == obj.buildPath(SNV_RESULTS_PATH_PART).relativePath.path
        }
    }

    static mapping = {
        /**
         * sample_pair_snv_idx1 is used by SnvCallingService.samplePairForProcessing.
         * processing_status must be the first column in this index! Grails does not provide a means to specify this, so
         * this must be done via SQL.
         *
         * The implicit unique index on (mergingWorkPackage1, mergingWorkPackage2) is used by
         * SamplePair.findMissingDiseaseControlSamplePairs.
         */
        mergingWorkPackage1 index: 'sample_pair_snv_idx1,sample_pair_indel_idx1,sample_pair_sophia_idx1,sample_pair_aceseq_idx1'
        mergingWorkPackage2 index: 'sample_pair_snv_idx1,sample_pair_indel_idx1,sample_pair_sophia_idx1,sample_pair_aceseq_idx1'
        snvProcessingStatus index: 'sample_pair_snv_idx1'
        indelProcessingStatus index: 'sample_pair_indel_idx1'
        sophiaProcessingStatus index: 'sample_pair_sophia_idx1'
        aceseqProcessingStatus index: 'sample_pair_aceseq_idx1'
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
    OtpPath getSnvSamplePairPath() {
        OtpPath path = buildPath(SNV_RESULTS_PATH_PART)
        assert relativePath == path.relativePath.path
        return path
    }

    OtpPath getIndelSamplePairPath() {
        return buildPath(INDEL_RESULTS_PATH_PART)
    }

    OtpPath getSophiaSamplePairPath() {
        return buildPath(SOPHIA_RESULTS_PATH_PART)
    }

    OtpPath getAceseqSamplePairPath() {
        return buildPath(ACESEQ_RESULTS_PATH_PART)
    }

    OtpPath getRunYapsaSamplePairPath() {
        return buildPath(RUN_YAPSA_RESULTS_PATH_PART)
    }

    private OtpPath buildPath(String analysisPath) {
        return new OtpPath(individual.getViewByPidPath(seqType), analysisPath, seqType.libraryLayoutDirName, "${sampleType1.dirName}_${sampleType2.dirName}")
    }

    String toStringWithoutId() {
        return "${individual.mockPid} ${sampleType1.name} ${sampleType2.name} ${seqType.displayName} ${seqType.libraryLayout}"
    }

    @Override
    String toString() {
        return "SP ${id}: ${toStringWithoutId()}"
    }

    AbstractSnvCallingInstance findLatestSnvCallingInstance() {
        return findLatestInstance(AbstractSnvCallingInstance.class) as AbstractSnvCallingInstance
    }

    IndelCallingInstance findLatestIndelCallingInstance() {
        return findLatestInstance(IndelCallingInstance.class) as IndelCallingInstance
    }

    SophiaInstance findLatestSophiaInstance() {
        return findLatestInstance(SophiaInstance.class) as SophiaInstance
    }

    AceseqInstance findLatestAceseqInstance() {
        return findLatestInstance(AceseqInstance.class) as AceseqInstance
    }

    RunYapsaInstance findLatestRunYapsaInstance() {
        return findLatestInstance(RunYapsaInstance.class) as RunYapsaInstance
    }

    private BamFilePairAnalysis findLatestInstance(Class instanceClass) {
        BamFilePairAnalysis criteria = instanceClass.createCriteria().get {
            eq ('samplePair', this)
            order('id', 'desc')
            maxResults(1)
        }
        if (!criteria?.withdrawn) {
            return criteria
        }
        return null
    }

    /**
     * The names of the properties of {@link #mergingWorkPackage1} and {@link #mergingWorkPackage2} which must be equal.
     * Like {@link MergingWorkPackage#getMergingProperties}, except libraryPreparationKit which is not relevant for sample pairs.
     */
    static final Collection<String> mergingWorkPackageEqualProperties = ['sample.individual', 'seqType', 'seqPlatformGroup', 'pipeline', 'antibodyTarget'].asImmutable()

    /**
     * Finds distinct combinations of [mergingWorkPackage1, mergingWorkPackage2] with these criteria:
     * <ul>
     *     <li>mergingWorkPackage1 and mergingWorkPackage2 differ only in their sampleType.</li>
     *     <li>mergingWorkPackage1.sampleType has category {@link SampleType.Category#DISEASE} and
     *         mergingWorkPackage2.sampleType has category {@link SampleType.Category#CONTROL}.</li>
     *     <li>neither mergingWorkPackage1.pipeline nor mergingWorkPackage2.pipeline have as name {@link Pipeline.Name#EXTERNALLY_PROCESSED}.
     *         This is only to avoid the creation of SamplePair instances violating the unique constraint
     *         on relativePath as long as OTP-1657 in not resolved.</li>
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
              ${mergingWorkPackageEqualProperties.collect {
                  "(mwp1.${it} = mwp2.${it} OR mwp1.${it} IS NULL AND mwp2.${it} IS NULL)"
              }.join(' AND\n')} AND
              mwp1.seqType IN :analysableSeqTypes AND
              stpp1.project = project_1 AND
              stpp2.project = project_1 AND
              stpp1.sampleType = sampleType_1 AND
              stpp2.sampleType = sampleType_2 AND
              stpp1.category = :disease AND
              stpp2.category = :control AND
              NOT EXISTS (
                FROM
                  SamplePair
                WHERE
                  mergingWorkPackage1 = mwp1 AND
                  mergingWorkPackage2 = mwp2)
            """, [
                disease: SampleType.Category.DISEASE,
                control: SampleType.Category.CONTROL,
                analysableSeqTypes: SeqType.getAllAnalysableSeqTypes(),
            ], [readOnly: true])
        return queryResults.collect {
            return createInstance(
                    mergingWorkPackage1: it[0],
                    mergingWorkPackage2: it[1]
            )
        }
    }


    static void setSnvProcessingStatus(final Collection<SamplePair> samplePairs,
                                       final ProcessingStatus snvProcessingStatus) {
        setProcessingStatus(samplePairs, snvProcessingStatus, "snvProcessingStatus")
    }

    static void setIndelProcessingStatus(final Collection<SamplePair> samplePairs,
                                         final ProcessingStatus indelProcessingStatus) {
        setProcessingStatus(samplePairs, indelProcessingStatus, "indelProcessingStatus")
    }

    static void setAceseqProcessingStatus(final Collection<SamplePair> samplePairs,
                                         final ProcessingStatus aceseqProcessingStatus) {
        setProcessingStatus(samplePairs, aceseqProcessingStatus, "aceseqProcessingStatus")
    }


    private static void setProcessingStatus(final Collection<SamplePair> samplePairs,
                                            final ProcessingStatus processingStatus, String property) {
        if (processingStatus == null) {
            throw new IllegalArgumentException()
        }
        SamplePair.withTransaction {
            samplePairs.each {
                it."${property}" = processingStatus
                assert it.save()
            }
        }
    }
}
