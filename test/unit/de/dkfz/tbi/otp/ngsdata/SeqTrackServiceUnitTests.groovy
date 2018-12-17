package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.Mock
import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig

@Mock([SeqTypeService])
@Build([
        DataFile,
        MergingCriteria,
        Pipeline,
        RoddyWorkflowConfig,
        RunSegment,
        SeqCenter,
        SeqPlatformGroup,
        SeqTrack,
        LogMessage,
])
class SeqTrackServiceUnitTests {

    SeqTrackService seqTrackService


    @Before
    void setUp() throws Exception {
        seqTrackService = new SeqTrackService()

        DomainFactory.createPanCanPipeline()
    }

    @After
    void after() {
        TestCase.cleanTestDirectory()
    }

    @Test
    void testMayAlign_everythingIsOkay_shouldReturnTrue() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                run: DomainFactory.createRun(
                        seqPlatform: DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                                seqPlatformGroups: [DomainFactory.createSeqPlatformGroup()])))
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        assert SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenDataFileWithdrawn_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([:], [
                fileWithdrawn: true,
        ])

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenNoDataFile_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenWrongFileType_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([:], [
                fileType: DomainFactory.createFileType(type: FileType.Type.SOURCE),
        ])

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenRunSegmentMustNotAlign_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        RunSegment runSegment = DomainFactory.createRunSegment(
                align: false,
        )
        DomainFactory.createDataFile(
                seqTrack: seqTrack,
                runSegment: runSegment,
        )

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenExomeKitReliabilityIsUnknownVerified_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.createExomeSeqTrack(
                libraryPreparationKit: null,
                kitInfoReliability: InformationReliability.UNKNOWN_VERIFIED,
        )
        DomainFactory.createDataFile(seqTrack: seqTrack)

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenSeqPlatformGroupIsNull_shouldReturnFalse() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([
                run: DomainFactory.createRun(seqPlatform: DomainFactory.createSeqPlatform()),
        ])
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        assert !SeqTrackService.mayAlign(seqTrack)
    }

    @Test
    void testFillBaseCount_sequenceLengthDoesNotExist_shouldFail() {
        String sequenceLength = null
        Long nReads = 12345689
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        shouldFail(AssertionError) { seqTrackService.fillBaseCount(seqTrack) }
    }

    @Test
    void testFillBaseCount_nReadsDoesNotExist_shouldFail() {
        String sequenceLength = "101"
        Long nReads = null
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        shouldFail(AssertionError) { seqTrackService.fillBaseCount(seqTrack) }
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
        DomainFactory.createSequenceDataFile([nReads: nReads, sequenceLength: sequenceLength, seqTrack: seqTrack])
        seqTrack.seqType.libraryLayout = LibraryLayout.PAIRED
        seqTrackService.fillBaseCount(seqTrack)
        assert seqTrack.nBasePairs == expectedBasePairs
    }

    @Test
    void testFillBaseCount_sequenceLengthIsIntegerRange() {
        String sequenceLength = "90-100"
        int meanSequenceLength = sequenceLength.split('-').sum { it.toInteger() } / 2
        Long nReads = 12345689
        Long expectedBasePairs = meanSequenceLength * nReads
        SeqTrack seqTrack = createTestSeqTrack(sequenceLength, nReads)
        seqTrackService.fillBaseCount(seqTrack)
        assert seqTrack.nBasePairs == expectedBasePairs
    }

    private SeqTrack createTestSeqTrack(String sequenceLength, Long nReads) {
        return DomainFactory.createSeqTrackWithOneDataFile([:], [nReads: nReads, sequenceLength: sequenceLength])
    }
}
