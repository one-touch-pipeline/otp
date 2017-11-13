package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.SeqType

abstract class BamFileAnalysisService {

    AbstractMergedBamFileService abstractMergedBamFileService

    static final List<AnalysisProcessingStates> processingStatesNotProcessable = [
            AnalysisProcessingStates.IN_PROGRESS
    ]

    static Collection<Class<? extends ConfigPerProject>> ANALYSIS_CONFIG_CLASSES = [
            SnvConfig,
            RoddyWorkflowConfig,
    ].asImmutable()

    /**
     * The method goes through the list of sample pairs and checks if there is a disease/control pair
     * which can be processed and returns it.
     * Criteria to pass before being processed are:
     * - bam & bai file available for disease and control
     * - if coverage threshold is given: coverage higher than coverage threshold
     * - if lane number threshold is given: lane number has reached lane number threshold
     * - processing of disease & control files finished -> transfer completed
     * - all not withdrawn lanes for these samples, available in OTP, are already merged in the bam files
     * - disease/control pair listed in {@link SamplePair}
     * - pair is not set to IGNORED
     * - pair is not already in processing
     * - config file is available
     */
    SamplePair samplePairForProcessing(short minPriority, Class<? extends ConfigPerProject> configClass, SamplePair sp = null) {
        samplePairForProcessing(minPriority, configClass, [SeqType.wholeGenomePairedSeqType, SeqType.exomePairedSeqType],sp)
    }

    SamplePair samplePairForProcessing(short minPriority, Class<? extends ConfigPerProject> configClass, List<SeqType> seqTypes, SamplePair sp = null) {
        assert ANALYSIS_CONFIG_CLASSES.contains(configClass)
        final String WORKPACKAGE = "workPackage"
        final String SAMPLE = "${WORKPACKAGE}.sample"
        final String SAMPLE_TYPE = "${SAMPLE}.sampleType"
        final String SEQ_TYPE = "${WORKPACKAGE}.seqType"
        final String INDIVIDUAL = "${SAMPLE}.individual"

        double threshold = ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE, getAnalysisType().toString(), null)?.toDouble() ?: 0.0

        def testIfBamFileFulfillCriteria = { String number ->
            return "AND EXISTS (FROM AbstractMergedBamFile ambf${number} " +
            // check that the file is not withdrawn
            "       WHERE ambf${number}.withdrawn = false " +
            //check that the bam file belongs to the SamplePair
            "       AND ambf${number}.${WORKPACKAGE} = sp.mergingWorkPackage${number} " +
            //check that transfer workflow is finished
            "       AND ambf${number}.md5sum IS NOT NULL " +
                    pipelineSpecificBamFileChecks(number) +
            //check that coverage is high enough & number of lanes are enough
            "       AND EXISTS ( FROM ProcessingThresholds pt "+
            "           WHERE pt.project = ambf${number}.${INDIVIDUAL}.project " +
            "           AND pt.seqType = ambf${number}.${SEQ_TYPE} " +
            "           AND pt.sampleType = ambf${number}.${SAMPLE_TYPE} " +
            "           AND (pt.coverage is null OR pt.coverage <= ambf${number}.coverage) " +
            "           AND (:threshold <= ambf${number}.coverage) " +
            "           AND (pt.numberOfLanes is null OR pt.numberOfLanes <= ambf${number}.numberOfMergedLanes) " +
            "           ) " +
            //check that the file is in the workpackage
            "       AND ambf${number}.${WORKPACKAGE}.bamFileInProjectFolder = ambf${number} " +
            //check that the file file operation status ist processed
            "       AND ambf${number}.fileOperationStatus = '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}' " +
            //check that the id is the last for that MergingWorkPackage
            "       AND ambf${number} = (select max(bamFile.id) from AbstractMergedBamFile bamFile where bamFile.workPackage = ambf${number}.workPackage)" +
            "       ) "
        }

        String onlyOtpSnv = configClass == SnvConfig ?
                "   AND EXISTS (from ExternalScript es " +
                "       where es.scriptVersion = cps.externalScriptVersion " +
                "       and es.deprecatedDate is null " +
                "   ) " : ""


        String samplePairForProcessing =
                "FROM SamplePair sp " +
                //check that sample pair shall be processed
                "WHERE " + getProcessingStateCheck() +

                (sp ? "AND sp = :sp " : '') +
                //check that processing priority of the corresponding project is high enough
                'AND sp.mergingWorkPackage1.sample.individual.project.processingPriority >= :minPriority ' +
                'AND sp.mergingWorkPackage1.seqType in (:seqTypes) ' +
                checkReferenceGenome() +


                //check that the config file is available with at least one script with same version
                "AND EXISTS (FROM ${configClass.name} cps " +
                "   WHERE cps.project = sp.mergingWorkPackage1.sample.individual.project " +
                "   AND cps.pipeline.type = :analysis " +
                "   AND cps.seqType = sp.mergingWorkPackage1.seqType " +
                "   AND cps.obsoleteDate is null " +
                onlyOtpSnv +
                ") " +


                //check that this sample pair is not in process
                "AND NOT EXISTS (FROM ${getAnalysisClass().name} sci " +
                "   WHERE sci.samplePair = sp " +
                "   AND sci.processingState IN (:processingStates) " +
                "   AND sci.withdrawn = false " +
                ") " +

                //check that the first bam file fulfill the criteria
                testIfBamFileFulfillCriteria("1") +

                //check that the second bam file fulfill the criteria
                testIfBamFileFulfillCriteria("2") +

                "ORDER BY sp.mergingWorkPackage1.sample.individual.project.processingPriority DESC, sp.dateCreated"

        Map parameters = [
                needsProcessing: ProcessingStatus.NEEDS_PROCESSING,
                processingStates: processingStatesNotProcessable,
                minPriority: minPriority,
                analysis: getAnalysisType(),
                seqTypes: seqTypes,
                threshold: threshold,
        ]
        if (sp) {
            parameters.sp = sp
        }
        parameters.putAll(checkReferenceGenomeMap())

        return SamplePair.find(samplePairForProcessing, parameters)
    }

    void validateInputBamFiles(final BamFilePairAnalysis analysis) throws Throwable {
        try {
            abstractMergedBamFileService.getExistingBamFilePath(analysis.sampleType1BamFile)
            abstractMergedBamFileService.getExistingBamFilePath(analysis.sampleType2BamFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input BAM files have changed on the file system while this job processed them.', e)
        }
    }

    protected String checkReferenceGenome(){
        return ''
    }

    protected String pipelineSpecificBamFileChecks(String number) {
        return ''
    }

    public Map<String, Object> checkReferenceGenomeMap(){
        return [:]
    }

    protected Map<String, Object> pipelineSpecificBamFileChecksMap(){
        return [:]
    }

    abstract protected String getProcessingStateCheck()
    abstract protected Class<BamFilePairAnalysis> getAnalysisClass()
    abstract protected Pipeline.Type getAnalysisType()

}
