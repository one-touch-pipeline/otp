package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import grails.buildtestdata.mixin.Build

/**
 */
@Build([RoddyBamFile, SeqPlatform])
class RoddyBamFileUnitTests {

    SampleType sampleType
    Individual individual
    RoddyBamFile roddyBamFile
    final String TEST_DIR = TestCase.getUniqueNonExistentPath()
    final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    final String COMMON_PREFIX = "4_NoIndex_L004"

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        sampleType = roddyBamFile.sampleType
        individual = roddyBamFile.individual

        AbstractMergedBamFileService.metaClass.static.destinationDirectory = { AbstractMergedBamFile bamFile -> return TEST_DIR }
    }

    @After
    void tearDown() {
        sampleType = null
        individual = null
        roddyBamFile = null
        AbstractMergedBamFileService.metaClass = null
    }

    void testGetRoddyBamFileName() {
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam" == roddyBamFile.getBamFileName()
    }

    void testGetRoddyBaiFileName(){
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.bai" == roddyBamFile.getBaiFileName()
    }

    void testGetRoddyMd5sumFileName(){
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.md5sum" == roddyBamFile.getMd5sumFileName()
    }

    void testGetTmpRoddyDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}" == roddyBamFile.tmpRoddyDirectory.path
    }


    void testGetTmpRoddyQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.getTmpRoddyQADirectory().path
    }


    void testGetFinalQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.finalQADirectory.path
    }


    void testGetTmpRoddyExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.tmpRoddyExecutionStoreDirectory.path
    }


    void testGetFinalRoddyExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.finalExecutionStoreDirectory.path
    }


    void testGetTmpRoddyBamFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.tmpRoddyBamFile.path
    }


    void testGetTmpRoddyBaiFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.tmpRoddyBaiFile.path
    }


    void testGetTmpRoddyMd5sumFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.tmpRoddyMd5sumFile.path
    }


    void testGetFinalBamFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.finalBamFile.path
    }


    void testGetFinalBaiFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.finalBaiFile.path
    }


    void testGetFinalMd5sumFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.finalMd5sumFile.path
    }


    void testGetTmpRoddyMergedQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.tmpRoddyMergedQADirectory.path
    }


    void testGetFinalMergedQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.finalMergedQADirectory.path
    }


    void testGetLongestCommenPrefixBeforeLastUnderscore_FirstStringNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(null, SECOND_DATAFILE_NAME)
        }
    }

    void testGetLongestCommenPrefixBeforeLastUnderscore_SecondStringNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, null)
        }
    }

    void testGetLongestCommenPrefixBeforeLastUnderscore_FirstStringEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore("", SECOND_DATAFILE_NAME)
        }
    }

    void testGetLongestCommenPrefixBeforeLastUnderscore_SecondStringEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, "")
        }
    }

    void testGetLongestCommenPrefixBeforeLastUnderscore_StringsAreEqual() {
        assert "4_NoIndex_L004_R1_complete" ==
                RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, FIRST_DATAFILE_NAME)
    }

    void testGetLongestCommenPrefixBeforeLastUnderscore_PrefixIsEqual() {
        assert COMMON_PREFIX ==
                RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, SECOND_DATAFILE_NAME)
    }

    void testGetLongestCommenPrefixBeforeLastUnderscore_NoUnderScoreInStrings() {
        assert "NoUnderScoreR" ==
                RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore("NoUnderScoreR1.tar.gz", "NoUnderScoreR2.tar.gz")
    }

    void testGetReadGroupName_SeqTrackIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getReadGroupName(null)
        }
    }

    void testGetReadGroupName_OnlyOneDataFile_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getReadGroupName(roddyBamFile.seqTracks.iterator()[0])
        }
    }

    void testGetReadGroupName_SameDataFileName_ShouldFail() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        assert DomainFactory.buildSequenceDataFile([seqTrack: seqTrack]).save(flush: true)
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getReadGroupName(roddyBamFile.seqTracks.iterator()[0])
        }
    }

    void testGetReadGroupName_AllFine() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        makeSeqTrackPaired(seqTrack)
        assert "run${seqTrack.run.name}_${COMMON_PREFIX}" == RoddyBamFile.getReadGroupName(roddyBamFile.seqTracks.iterator()[0])
    }


    void testGetTmpRoddySingleLaneQADirectories_NoSeqTracks() {
        roddyBamFile.seqTracks = null
        assert roddyBamFile.tmpRoddySingleLaneQADirectories.isEmpty()
    }

    void testGetTmpRoddySingleLaneQADirectories_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        makeSeqTrackPaired(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.tmpRoddySingleLaneQADirectories
    }

    void testGetTmpRoddySingleLaneQADirectories_TwoSeqTracks() {
        makeSeqTrackPaired(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(roddyBamFile.workPackage)
        makeSeqTrackPaired(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, File> expected = new HashMap<>()
        roddyBamFile.seqTracks.each {
            File dir = new File("${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        Map<SeqTrack, File> actual = roddyBamFile.tmpRoddySingleLaneQADirectories
        assert  expected == actual
    }


    void testGetFinalRoddySingleLaneQADirectories_NoSeqTracks() {
        roddyBamFile.seqTracks = null
        assert roddyBamFile.finalRoddySingleLaneQADirectories.isEmpty()
    }

    void testGetFinalRoddySingleLaneQADirectories_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        makeSeqTrackPaired(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.finalRoddySingleLaneQADirectories
    }

    void testGetFinalRoddySingleLaneQADirectories_TwoSeqTracks() {
        makeSeqTrackPaired(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(roddyBamFile.workPackage)
        makeSeqTrackPaired(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, File> expected = new HashMap<>()
        roddyBamFile.seqTracks.each {
            File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        Map<SeqTrack, File> actual = roddyBamFile.finalRoddySingleLaneQADirectories
        assert  expected == actual
    }


    void testGetRoddySingleLaneQADirectoriesHelper_FinalFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        makeSeqTrackPaired(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getRoddySingleLaneQADirectoriesHelper(roddyBamFile.finalQADirectory)
    }

    void testGetRoddySingleLaneQADirectoriesHelper_TmpFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        makeSeqTrackPaired(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getRoddySingleLaneQADirectoriesHelper(roddyBamFile.tmpRoddyQADirectory)
    }


    private void makeSeqTrackPaired(SeqTrack seqTrack) {
        assert DomainFactory.buildSequenceDataFile([seqTrack: seqTrack]).save(flush: true)
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        dataFiles[0].fileName = FIRST_DATAFILE_NAME
        dataFiles[1].fileName = SECOND_DATAFILE_NAME
    }
}
