package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.testing.*
import org.junit.*

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
    void testDecideAndPrepareForAlignment_noAlignmentDecider_shouldReturnEmptyList() {
        SeqTrack seqTrack = setupSeqTrackProjectAndDataFile()

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


    private static SeqTrack setupSeqTrackProjectAndDataFile() {
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
        project.save(failOnError: true)
        return seqTrack
    }
}
