package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.LogMessage
import org.junit.After

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import grails.buildtestdata.mixin.Build
import grails.test.mixin.Mock
import org.junit.Before
import org.junit.Test

@Mock([SeqTypeService])
@Build([
    DataFile,
    RunSegment,
    SeqPlatformGroup,
    SeqTrack,
    LogMessage,
])
class SeqTrackServiceUnitTests {

    SeqTrackService seqTrackService


    @Before
    void setUp() throws Exception {
        seqTrackService = new SeqTrackService()
    }

    @After
    void after() {
        TestCase.cleanTestDirectory()
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
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_HasToBeLinked_UsingSecondPath() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked(1)

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
    void testDetermineAndStoreIfFastqFilesHaveToBeLinked_WgbsData_MustBeCopied() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked()

            SeqType wgbsSeqType = DomainFactory.createSeqType(name: "WHOLE_GENOME_BISULFITE")
            seqTrack.seqType = wgbsSeqType

            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, true)

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
    void testAreFilesLocatedOnMidTermStorage_DataFilesOnMidterm_SecondStorage() {
        withTestMidtermStorageMountPoint {
            SeqTrack seqTrack = createDataForAreFilesLocatedOnMidTermStorage(1)
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


    private SeqTrack createDataForDetermineAndStoreIfFastqFilesHaveToBeLinked(int pos = 0) {
        Run run = Run.build(seqCenter: SeqCenter.build(name: "DKFZ"))
        RunSegment runSegment = RunSegment.build(dataPath: LsdfFilesService.midtermStorageMountPoint[pos], run: run)
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

    private SeqTrack createDataForAreFilesLocatedOnMidTermStorage(int pos = 0) {
        RunSegment runSegment = RunSegment.build(dataPath: LsdfFilesService.midtermStorageMountPoint[pos])
        SeqTrack seqTrack = SeqTrack.build()
        DataFile.build(seqTrack: seqTrack, runSegment: runSegment)
        DataFile.build(seqTrack: seqTrack, runSegment: runSegment)
        return seqTrack
    }

    private withTestMidtermStorageMountPoint(Closure code) {
        def originalMidtermStorageMountPoint = LsdfFilesService.midtermStorageMountPoint
        try {
            LsdfFilesService.midtermStorageMountPoint = [1, 2].collect{TestCase.getUniqueNonExistentPath().path}.asImmutable()
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

    @Test
    void testFillBaseCount_sequenceLengthDoesNotExist_shouldFail() {
        String sequenceLength = null
        Long nReads = 12345689
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        shouldFail(AssertionError) {seqTrackService.fillBaseCount(seqTrack)}
    }

    @Test
    void testFillBaseCount_nReadsDoesNotExist_shouldFail() {
        String sequenceLength = "101"
        Long nReads = null
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        shouldFail(AssertionError) {seqTrackService.fillBaseCount(seqTrack)}
    }


    @Test
    void testFillBaseCount_sequenceLengthIsSingleValue_LibraryLayoutSingle() {
        String sequenceLength = "101"
        Long nReads = 12345689
        Long expectedBasePairs = sequenceLength.toInteger() * nReads
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        seqTrackService.fillBaseCount(seqTrack)
        assert seqTrack.nBasePairs == expectedBasePairs
    }


    @Test
    void testFillBaseCount_sequenceLengthIsSingleValue_LibraryLayoutPaired() {
        String sequenceLength = "101"
        Long nReads = 12345689
        Long expectedBasePairs = sequenceLength.toInteger() * nReads * 2
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        DomainFactory.buildSequenceDataFile([nReads: nReads, sequenceLength: sequenceLength, seqTrack: seqTrack])
        seqTrack.seqType.libraryLayout = LibraryLayout.PAIRED
        seqTrackService.fillBaseCount(seqTrack)
        assert seqTrack.nBasePairs == expectedBasePairs
    }

    @Test
    void testFillBaseCount_sequenceLengthIsIntegerRange() {
        String sequenceLength = "90-100"
        int meanSequenceLength = sequenceLength.split('-').sum {it.toInteger()}/2
        Long nReads = 12345689
        Long expectedBasePairs = meanSequenceLength * nReads
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        seqTrackService.fillBaseCount(seqTrack)
        assert seqTrack.nBasePairs == expectedBasePairs
    }

    private SeqTrack createTestSeqTrack(String sequenceLength, Long nReads) {
        return DomainFactory.buildSeqTrackWithDataFile([:], [nReads:nReads, sequenceLength:sequenceLength])
    }
}
