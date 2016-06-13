package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class SnvCallingService {

    static final List<SnvProcessingStates> processingStatesNotProcessable = [
            SnvProcessingStates.IN_PROGRESS
    ]

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
    SamplePair samplePairForSnvProcessing(short minPriority, SamplePair sp = null) {

        final String WORKPACKAGE = "workPackage"
        final String SAMPLE = "${WORKPACKAGE}.sample"
        final String SAMPLE_TYPE = "${SAMPLE}.sampleType"
        final String SEQ_TYPE = "${WORKPACKAGE}.seqType"
        final String INDIVIDUAL = "${SAMPLE}.individual"

        def testIfBamFileFulfillCriteria = { String number ->
            return "AND EXISTS (FROM AbstractMergedBamFile ambf${number} " +
            // check that the file is not withdrawn
            "       WHERE ambf${number}.withdrawn = false " +
            //check that the bam file belongs to the SamplePair
            "       AND ambf${number}.${WORKPACKAGE} = sp.mergingWorkPackage${number} " +
            //check that transfer workflow is finished
            "       AND ambf${number}.md5sum IS NOT NULL " +
            //check that coverage is high enough & number of lanes are enough
            "       AND EXISTS ( FROM ProcessingThresholds pt "+
            "           WHERE pt.project = ambf${number}.${INDIVIDUAL}.project " +
            "           AND pt.seqType = ambf${number}.${SEQ_TYPE} " +
            "           AND pt.sampleType = ambf${number}.${SAMPLE_TYPE} " +
            "           AND (pt.coverage is null OR pt.coverage <= ambf${number}.coverage) " +
            "           AND (pt.numberOfLanes is null OR pt.numberOfLanes <= ambf${number}.numberOfMergedLanes)) " +
            "       ) "
        }

        String pairForSnvProcessing =
                "FROM SamplePair sp " +
                //check that sample pair shall be processed
                "WHERE sp.processingStatus = :needsProcessing " +
                (sp ? "AND sp = :sp " : '') +
                //check that processing priority of the corresponding project is high enough
                'AND sp.mergingWorkPackage1.sample.individual.project.processingPriority >= :minPriority ' +


                //check that the config file is available with at least on script with same version
                "AND EXISTS (FROM SnvConfig cps " +
                "   WHERE cps.project = sp.mergingWorkPackage1.sample.individual.project " +
                "   AND cps.seqType = sp.mergingWorkPackage1.seqType " +
                "   AND EXISTS (from ExternalScript es " +
                "       where es.scriptVersion = cps.externalScriptVersion " +
                "       and es.deprecatedDate is null " +
                "   ) " +
                ") " +

                //check that this sample pair is not in process
                "AND NOT EXISTS (FROM SnvCallingInstance sci " +
                "   WHERE sci.samplePair = sp " +
                "   AND sci.processingState IN (:processingStates) " +
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
        ]
        if (sp) {
            parameters.sp = sp
        }
        List<SamplePair> samplePairs = SamplePair.findAll(pairForSnvProcessing, parameters)

        if (samplePairs) {
            return samplePairs.find {
                it.mergingWorkPackage1.completeProcessableBamFileInProjectFolder &&
                it.mergingWorkPackage2.completeProcessableBamFileInProjectFolder
            }
        } else {
            return null
        }
    }

    void markSnvCallingInstanceAsFailed(SnvCallingInstance instance, List<SnvCallingStep> stepsToWithdrawSnvJobResults) {
        assert stepsToWithdrawSnvJobResults : 'at least one of SnvCallingStep must be provided'
        stepsToWithdrawSnvJobResults.each { step ->
            def snvJobResult = exactlyOneElement(SnvJobResult.findAllBySnvCallingInstanceAndStep(instance, step))
            snvJobResult.withdrawn = true
            assert snvJobResult.save([flush: true])
        }
        instance.updateProcessingState(SnvProcessingStates.FAILED)
    }

}
