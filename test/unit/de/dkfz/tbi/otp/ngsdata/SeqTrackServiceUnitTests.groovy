package de.dkfz.tbi.otp.ngsdata

import org.junit.After

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import grails.buildtestdata.mixin.Build
import grails.test.mixin.Mock
import org.junit.Test

@Mock([SeqTypeService])
@Build([
    DataFile,
    RunSegment,
    SeqPlatformGroup,
    SeqTrack,
])
class SeqTrackServiceUnitTests {

    SeqTrackService seqTrackService

    SeqType alignableSeqType


    void setUp() throws Exception {
        seqTrackService = new SeqTrackService()
        alignableSeqType = DomainFactory.createAlignableSeqTypes().first()
    }

    @After
    void after() {
        TestCase.cleanTestDirectory()
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAll_noReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build(
                        fastqcState: SeqTrack.DataProcessingState.UNKNOWN
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing()
        assert null == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAll_oneReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing()
        assert seqTrack == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_noReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.UNKNOWN,
                        seqType: alignableSeqType
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing(true)
        assert null == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_onlyNotAlignableSeqTracksAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing(true)
        assert null == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessing_getAlignable_withReadyAlignableSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                        seqType: alignableSeqType
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessing(true)
        assert seqTrack == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_NoReadySeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.UNKNOWN
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert null == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_SeqTrackAvailable() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )

        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert seqTrack == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_TakeFirstAlignableSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )
        seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                        seqType: alignableSeqType
                        )
        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert seqTrack == ret
    }

    @Test
    void testGetSeqTrackReadyForFastqcProcessingPreferAlignable_TakeOlderSeqTrack() {
        SeqTrack seqTrack = SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )
        SeqTrack.build (
                        fastqcState: SeqTrack.DataProcessingState.NOT_STARTED
                        )
        SeqTrack ret = seqTrackService.getSeqTrackReadyForFastqcProcessingPreferAlignable()
        assert seqTrack == ret
    }

    @Test
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_SeqTrackIsNull_ShouldFail() {
        shouldFail(AssertionError) {seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(null, true)}
    }

    @Test
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_HasToBeLinked() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

            assert seqTrack.linkedExternally == true
        }
    }

    @Test
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_WillBeAlignedIsFalse() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, false)

            assert seqTrack.linkedExternally == false
        }
    }

    @Test
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_DataNotFromCore() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
            seqTrack.run.seqCenter = SeqCenter.build(name: "NotCore")

            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

            assert seqTrack.linkedExternally == false
        }
    }

    @Test
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_ProjectForcedToBeCopied() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
            seqTrack.sample.individual.project.hasToBeCopied = true

            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, false)

            assert seqTrack.linkedExternally == false
        }
    }

    @Test
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_DataNotOnMidterm() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
            RunSegment.list()[0].dataPath = TestCase.uniqueNonExistentPath

            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, false)

            assert seqTrack.linkedExternally == false
        }
    }

    @Test
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_NoDKFZSeqCenterFound_ShouldFail() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()
            seqTrack.run.seqCenter.name = "NotDKFZ"

            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, false)

            assert seqTrack.linkedExternally == false
        }
    }


    @Test
    void testAreFilesLocatedOnMidTermStorage_SeqTrackIsNull_ShouldFail() {
        shouldFail(AssertionError) {seqTrackService.areFilesLocatedOnMidTermStorage(null)}

    }

    @Test
    void testAreFilesLocatedOnMidTermStorage_DataFilesOnMidterm() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage()
            assert true == seqTrackService.areFilesLocatedOnMidTermStorage(seqTrack)
        }
    }

    @Test
    void testAreFilesLocatedOnMidTermStorage_DataFilesNotOnMidterm() {
        SeqTrack seqTrack
        withTestMidtermStorageMountPoint {
            seqTrack = createDataForAreFilesLocatedOnMidTermStorage()
        }
        assert false == seqTrackService.areFilesLocatedOnMidTermStorage(seqTrack)
    }

    @Test
    void testAreFilesLocatedOnMidTermStorage_NoDataFilesForSeqTrack_ShouldFail() {
        SeqTrack seqTrack = SeqTrack.build()

        shouldFail(AssertionError) {seqTrackService.areFilesLocatedOnMidTermStorage(seqTrack)}

    }

    @Test
    void testAreFilesLocatedOnMidTermStorage_DataFilesFromDifferentRunSegments_ShouldFail() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage()
            DataFile.build(seqTrack: seqTrack, runSegment: RunSegment.build())

            shouldFail(AssertionError) { seqTrackService.areFilesLocatedOnMidTermStorage(seqTrack) }
        }
    }


    private SeqTrack createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked() {
        Run run = Run.build(seqCenter: SeqCenter.build(name: "DKFZ"))
        RunSegment runSegment = RunSegment.build(dataPath: LsdfFilesService.midtermStorageMountPoint, run: run)
        SeqTrack seqTrack = SeqTrack.build(
                run: run,
                sample: Sample.build(
                        individual: Individual.build(
                                project: Project.build(hasToBeCopied: false)
                        )
                )
        )

        DataFile.build(seqTrack: seqTrack, run: run, runSegment: runSegment)
        DataFile.build(seqTrack: seqTrack, run: run, runSegment: runSegment)
        return seqTrack
    }

    private SeqTrack createDataForAreFilesLocatedOnMidTermStorage() {
        RunSegment runSegment = RunSegment.build(dataPath: LsdfFilesService.midtermStorageMountPoint)
        SeqTrack seqTrack = SeqTrack.build()
        DataFile.build(seqTrack: seqTrack, runSegment: runSegment)
        DataFile.build(seqTrack: seqTrack, runSegment: runSegment)
        return seqTrack
    }

    private withTestMidtermStorageMountPoint(Closure code) {
        def originalMidtermStorageMountPoint = LsdfFilesService.midtermStorageMountPoint
        try {
            LsdfFilesService.midtermStorageMountPoint = TestCase.getUniqueNonExistentPath()
            code()
        } finally {
            LsdfFilesService.midtermStorageMountPoint = originalMidtermStorageMountPoint
        }
    }

    @Test
    void testMayAlign_everythingIsOkay_shouldReturnTrue() {
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile()

        assert SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenDataFileWithdrawn_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile([:], [
                fileWithdrawn: true,
        ])

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenNoDataFile_shouldReturnFalse() {
        SeqTrack seqTrack = SeqTrack.build()

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenWrongFileType_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile([:], [
                fileType: FileType.build(type: FileType.Type.SOURCE),
        ])

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenRunSegmentMustNotAlign_shouldReturnFalse() {
        SeqTrack seqTrack = SeqTrack.build()
        RunSegment runSegment = RunSegment.build(
                align: false,
        )
        DataFile.build(
                seqTrack: seqTrack,
                runSegment: runSegment,
        )

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenExomeKitReliabilityIsUnknownVerified_shouldReturnFalse() {
        SeqTrack seqTrack = ExomeSeqTrack.build(
                libraryPreparationKit: null,
                kitInfoReliability: InformationReliability.UNKNOWN_VERIFIED,
        )
        DataFile.build(
                seqTrack: seqTrack,
        )

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenSeqPlatformGroupIsNull_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile([
                seqPlatform: SeqPlatform.build(seqPlatformGroup: null),
        ])

        assert !SeqTrackService.mayAlign(seqTrack)
    }
}
