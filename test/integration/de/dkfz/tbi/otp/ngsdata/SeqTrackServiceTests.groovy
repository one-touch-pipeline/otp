package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import org.junit.After
import org.junit.Before
import org.junit.Test

import static de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState.*

class SeqTrackServiceTests extends AbstractIntegrationTest {

    SeqTrackService seqTrackService

    TestData testData

    File dataPath
    File mdPath
    SeqType alignableSeqType

    @Before
    void setUp() {
        dataPath = TestCase.getUniqueNonExistentPath()
        mdPath = TestCase.getUniqueNonExistentPath()
        testData = new TestData()
        alignableSeqType = DomainFactory.createAlignableSeqTypes().first()
    }

    @After
    void tearDown() {
        TestCase.cleanTestDirectory()
        testData = null
    }
    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAll_noReadySeqTrackAvailable() {
        SeqTrack.build(
                fastqcState: UNKNOWN
        )

        assert null == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAll_oneReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                fastqcState: NOT_STARTED
        )

        assert seqTrack == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_noReadySeqTrackAvailable() {
        SeqTrack.build (
                fastqcState: UNKNOWN,
                seqType: alignableSeqType
        )

        assert null == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_withReadyAlignableSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                fastqcState: NOT_STARTED,
                seqType: alignableSeqType
        )

        assert seqTrack == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_TakeFirstAlignableSeqTrack() {
        SeqTrack.build (
                fastqcState: NOT_STARTED
        )
        SeqTrack seqTrack = SeqTrack.build (
                fastqcState: NOT_STARTED,
                seqType: alignableSeqType
        )
        assert seqTrack == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_TakeOlderSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                fastqcState: NOT_STARTED
        )
        SeqTrack.build (
                fastqcState: NOT_STARTED
        )
        assert seqTrack == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
        assert null == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.FAST_TRACK_PRIORITY)
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_takeWithHigherPriority() {
        SeqTrack.build (fastqcState: NOT_STARTED)
        SeqTrack seqTrack = SeqTrack.build (fastqcState: NOT_STARTED)
        Project project = seqTrack.project
        project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        project.save(flush: true)

        assert seqTrack == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.NORMAL_PRIORITY)
        assert seqTrack == seqTrackService.getSeqTrackReadyForFastqcProcessing(ProcessingPriority.FAST_TRACK_PRIORITY)
    }


    @Test
    void testDecideAndPrepareForAlignment_defaultDecider_shouldReturnOneWorkPackage() {
        SeqTrack seqTrack = setupSeqTrackProjectAndDataFile("defaultOtpAlignmentDecider")

        Collection<MergingWorkPackage> workPackages = seqTrackService.decideAndPrepareForAlignment(seqTrack)

        assert workPackages.size() == 1
        assert workPackages.iterator().next().seqType == seqTrack.seqType
    }

    @Test
    void testDecideAndPrepareForAlignment_noAlignmentDecider_shouldReturnEmptyList() {
        SeqTrack seqTrack = setupSeqTrackProjectAndDataFile("noAlignmentDecider")

        Collection<MergingWorkPackage> workPackages = seqTrackService.decideAndPrepareForAlignment(seqTrack)

        assert workPackages.empty
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_InputIsNull_ShouldFail() {
        shouldFail(IllegalArgumentException) {
            seqTrackService.returnExternallyProcessedMergedBamFiles(null)
        }
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_InputIsEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            seqTrackService.returnExternallyProcessedMergedBamFiles([])
        }
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_NoExternalBamFileAttached_AllFine() {
        SeqTrack seqTrack = SeqTrack.build()
        assert seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack]).isEmpty()
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_ExternalBamFileAttached_AllFine() {
        SeqTrack seqTrack = SeqTrack.build()
        ExternallyProcessedMergedBamFile bamFile = ExternallyProcessedMergedBamFile.build(
                fastqSet: FastqSet.build(seqTracks: [seqTrack]),
                type: AbstractBamFile.BamType.RMDUP,
        ).save(flush: true)
        assert [bamFile] == seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack])
    }

    @Test
    void testGetAlignmentDecider_WhenNoAlignmentDeciderBeanName_ShouldFail() {
        Project project = Project.build()

        TestCase.shouldFail (RuntimeException) {
            seqTrackService.getAlignmentDecider(project)
        }
    }

    @Test
    void testGetAlignmentDecider_WhenAllFine_ShouldReturnAlignmentDecider() {
        Project project = Project.build(alignmentDeciderBeanName: "noAlignmentDecider")

        assert "NoAlignmentDecider" == seqTrackService.getAlignmentDecider(project).class.simpleName
    }


    @Test
    void testSetRunReadyForFastqc_SeqTracksReady() {
        DataFile dataFile = createDataFor_setRunReadyForFastqc()

        seqTrackService.setRunReadyForFastqc(dataFile.run)
        assert SeqTrack.DataProcessingState.NOT_STARTED == dataFile.seqTrack.fastqcState
    }

    @Test
    void testSetRunReadyForFastqc_NoSeqTracksReady() {
        DataFile dataFile = createDataFor_setRunReadyForFastqc()
        dataFile.seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert dataFile.save(flush: true, failOnError: true)

        seqTrackService.setRunReadyForFastqc(dataFile.run)
        assert SeqTrack.DataProcessingState.FINISHED == dataFile.seqTrack.fastqcState
    }

    @Test
    void testSetRunReadyForFastqc_MultipleSeqTracksReady() {
        DataFile dataFile1 = createDataFor_setRunReadyForFastqc()
        DataFile dataFile2 = createDataFor_setRunReadyForFastqc()
        dataFile2.runSegment.run  = dataFile1.runSegment.run
        dataFile2.run  = dataFile1.run
        assert dataFile2.runSegment.save(flush: true, failOnError: true)
        assert dataFile2.save(flush: true, failOnError: true)

        seqTrackService.setRunReadyForFastqc(dataFile1.run)
        assert SeqTrack.DataProcessingState.NOT_STARTED == dataFile1.seqTrack.fastqcState
        assert SeqTrack.DataProcessingState.NOT_STARTED == dataFile2.seqTrack.fastqcState
    }


    private DataFile createDataFor_setRunReadyForFastqc() {
        Run run = DomainFactory.createRun()
        RunSegment runSegment = DomainFactory.createRunSegment(
                run: run,
                filesStatus: RunSegment.FilesStatus.FILES_CORRECT,
        )
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                run: run
        )
        DataFile dataFile = DomainFactory.buildSequenceDataFile(
                seqTrack: seqTrack,
                run: runSegment.run,
                runSegment: runSegment,
                project: seqTrack.project
        )
        return dataFile
    }




    private static SeqTrack setupSeqTrackProjectAndDataFile(String decider) {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                seqType: DomainFactory.createAlignableSeqTypes().first(),
        )

        SeqPlatform sp = seqTrack.seqPlatform
        sp.seqPlatformGroup = DomainFactory.createSeqPlatformGroup()
        sp.save(flush: true)

        DomainFactory.createReferenceGenomeProjectSeqType(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        ).save(flush: true)

        DomainFactory.buildSequenceDataFile(
                seqTrack: seqTrack,
                fileWithdrawn: false,
                fileExists: true,
                fileSize: 1L,
        ).save(flush: true)

        Project project = seqTrack.project
        project.alignmentDeciderBeanName = decider
        project.save(failOnError: true)
        return seqTrack
    }
}
