package de.dkfz.tbi.otp.ngsdata
import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.testing.*
import org.junit.*

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
        alignableSeqType = DomainFactory.createDefaultOtpAlignableSeqTypes().first()
    }

    @After
    void tearDown() {
        TestCase.cleanTestDirectory()
        testData = null
    }
    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAll_noReadySeqTrackAvailable() {
        DomainFactory.createSeqTrack(
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
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        assert seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack]).isEmpty()
    }

    @Test
    void testReturnExternallyProcessedMergedBamFiles_ExternalBamFileAttached_AllFine() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage(
                seqType: seqTrack.seqType,
                sample:  seqTrack.sample,
        )
        ExternallyProcessedMergedBamFile bamFile = DomainFactory.createExternallyProcessedMergedBamFile(
                workPackage: externalMergingWorkPackage,
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


    private static SeqTrack setupSeqTrackProjectAndDataFile(String decider) {
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                seqType: DomainFactory.createDefaultOtpAlignableSeqTypes().first(),
        )

        SeqPlatform sp = seqTrack.seqPlatform
        sp.save(flush: true)

        DomainFactory.createMergingCriteriaLazy(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        )

        DomainFactory.createReferenceGenomeProjectSeqType(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        ).save(flush: true)

        DomainFactory.createSequenceDataFile(
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
