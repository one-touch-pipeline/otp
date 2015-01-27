package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.springframework.util.Assert.*

class SnvCallingService {

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
    SamplePair samplePairForSnvProcessing() {

        List<SnvProcessingStates> unallowedProcessingStates = [
            SnvProcessingStates.IN_PROGRESS
        ]

        final String WORKPACKAGE = "mergingPass.mergingSet.mergingWorkPackage"
        final String SAMPLE = "${WORKPACKAGE}.sample"
        final String SAMPLE_TYPE = "${SAMPLE}.sampleType"
        final String SEQ_TYPE = "${WORKPACKAGE}.seqType"
        final String INDIVIDUAL = "${SAMPLE}.individual"

        def testIfBamFileFulfillCriteria = { String number ->
            return "AND EXISTS (FROM ProcessedMergedBamFile pmbf${number} " +
            // check that the file is not withdrawn
            "       WHERE pmbf${number}.withdrawn = false " +
            //check that the bam file belongs to the sample type, seq type and individual from the SamplePair
            "       AND pmbf${number}.${INDIVIDUAL} = sp.individual " +
            "       AND pmbf${number}.${SAMPLE_TYPE} = sp.sampleType${number} " +
            "       AND pmbf${number}.${SEQ_TYPE} = sp.seqType " +
            //check that transfer workflow is finished
            "       AND pmbf${number}.md5sum IS NOT NULL " +
            //check that coverage is high enough & number of lanes are enough
            "       AND EXISTS ( FROM ProcessingThresholds pt "+
            "           WHERE pt.project = pmbf${number}.${INDIVIDUAL}.project " +
            "           AND pt.seqType = pmbf${number}.${SEQ_TYPE} " +
            "           AND pt.sampleType = pmbf${number}.${SAMPLE_TYPE} " +
            "           AND (pt.coverage is null OR pt.coverage <= pmbf${number}.coverage) " +
            "           AND (pt.numberOfLanes is null OR pt.numberOfLanes <= pmbf${number}.numberOfMergedLanes)) " +
            "       ) "
        }

        String pairForSnvProcessing =
                "FROM SamplePair sp " +
                //check that sample pair shall be processed
                "WHERE sp.processingStatus = :needsProcessing " +

                //check that the config file is available
                "AND EXISTS (FROM SnvConfig cps " +
                "   WHERE cps.project = sp.individual.project " +
                "   AND cps.seqType = sp.seqType) " +

                //check that this sample pair is not in process
                "AND NOT EXISTS (FROM SnvCallingInstance sci " +
                "   WHERE sci.samplePair = sp " +
                "   AND sci.processingState IN (:processingStates) " +
                ") " +

                //check that the first bam file fulfill the criteria
                testIfBamFileFulfillCriteria("1") +

                //check that the second bam file fulfill the criteria
                testIfBamFileFulfillCriteria("2") +

                "ORDER BY sp.dateCreated"

        List<SamplePair> samplePairs = SamplePair.findAll(
                pairForSnvProcessing,
                [
                        needsProcessing: ProcessingStatus.NEEDS_PROCESSING,
                        processingStates: unallowedProcessingStates,
                ])

        if (samplePairs) {
            return samplePairs.find {

                //get the latest ProcessedMergedBamFiles for both sample Types
                ProcessedMergedBamFile processedMergedBamFile1 = it.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(it.sampleType1)
                ProcessedMergedBamFile processedMergedBamFile2 = it.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(it.sampleType2)


                //check that the latest ProcessedMergedBamFiles contain all available seqTracks
                processedMergedBamFile1 && processedMergedBamFile2 && checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile1) &&
                        checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile2)
            }
        } else {
            return null
        }
    }

    /**
     * returns if all seqTracks for one sample and seqType, which are available in OTP, are merged in the given bam file
     */
    boolean checkIfAllAvailableSeqTracksAreIncluded(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The input of method checkIfAllAvailableSeqTracksAreIncluded is null")
        Set<SeqTrack> containedSeqTracks = processedMergedBamFile.getContainedSeqTracks()
        Set<SeqTrack> availableSeqTracks = SeqTrack.findAllBySampleAndSeqType(processedMergedBamFile.sample,
                processedMergedBamFile.seqType).findAll{!it.isWithdrawn()} as Set<SeqTrack>
        return containedSeqTracks*.id as Set == availableSeqTracks*.id as Set
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
