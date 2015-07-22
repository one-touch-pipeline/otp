package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.dataprocessing.RoddyBamFile.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import org.junit.After
import org.junit.Before
import grails.buildtestdata.mixin.Build
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static de.dkfz.tbi.TestCase.shouldFail
import static de.dkfz.tbi.TestCase.shouldFailWithMessage

@Build([RoddyBamFile, SeqPlatform])
class RoddyBamFileUnitTests {

    public static final String RODDY_EXECUTION_DIR_NAME = "exec_000000_000000000_a_a"
    SampleType sampleType
    Individual individual
    RoddyBamFile roddyBamFile
    final String TEST_DIR = TestCase.getUniqueNonExistentPath()
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()
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

    @Test
    void testGetRoddyBamFileName() {
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam" == roddyBamFile.getBamFileName()
    }

    @Test
    void testGetRoddyBaiFileName(){
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.bai" == roddyBamFile.getBaiFileName()
    }

    @Test
    void testGetRoddyMd5sumFileName(){
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.md5" == roddyBamFile.getMd5sumFileName()
    }

    @Test
    void testGetTmpRoddyDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}" == roddyBamFile.tmpRoddyDirectory.path
    }

    @Test
    void testGetTmpRoddyQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.getTmpRoddyQADirectory().path
    }

    @Test
    void testGetFinalQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.finalQADirectory.path
    }

    @Test
    void testGetTmpRoddyExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.tmpRoddyExecutionStoreDirectory.path
    }

    @Test
    void testGetFinalRoddyExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.finalExecutionStoreDirectory.path
    }

    @Test
    void testGetTmpRoddyBamFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.tmpRoddyBamFile.path
    }

    @Test
    void testGetTmpRoddyBaiFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.tmpRoddyBaiFile.path
    }

    @Test
    void testGetTmpRoddyMd5sumFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.tmpRoddyMd5sumFile.path
    }

    @Test
    void testGetFinalBamFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.finalBamFile.path
    }

    @Test
    void testGetFinalBaiFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.finalBaiFile.path
    }

    @Test
    void testGetFinalMd5sumFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.finalMd5sumFile.path
    }

    @Test
    void testGetTmpRoddyMergedQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.tmpRoddyMergedQADirectory.path
    }

    @Test
    void testGetTmpRoddyMergedQAJsonFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}/${QUALITY_CONTROL_JSON_FILE_NAME}" ==
                roddyBamFile.tmpRoddyMergedQAJsonFile.path
    }

    @Test
    void testGetFinalMergedQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.finalMergedQADirectory.path
    }

    @Test
    void testGetFinalRoddyMergedQAJsonFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}/${QUALITY_CONTROL_JSON_FILE_NAME}" ==
                roddyBamFile.finalMergedQAJsonFile.path
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_FirstStringNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(null, SECOND_DATAFILE_NAME)
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_SecondStringNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, null)
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_FirstStringEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore("", SECOND_DATAFILE_NAME)
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_SecondStringEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, "")
        }
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_StringsAreEqual() {
        assert "4_NoIndex_L004_R1_complete" ==
                RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, FIRST_DATAFILE_NAME)
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_PrefixIsEqual() {
        assert COMMON_PREFIX ==
                RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore(FIRST_DATAFILE_NAME, SECOND_DATAFILE_NAME)
    }

    @Test
    void testGetLongestCommonPrefixBeforeLastUnderscore_NoUnderScoreInStrings() {
        assert "NoUnderScoreR" ==
                RoddyBamFile.getLongestCommonPrefixBeforeLastUnderscore("NoUnderScoreR1.tar.gz", "NoUnderScoreR2.tar.gz")
    }

    @Test
    void testGetReadGroupName_SeqTrackIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getReadGroupName(null)
        }
    }

    @Test
    void testGetReadGroupName_OnlyOneDataFile_ShouldFail() {
        DataFile.findAllBySeqTrack(roddyBamFile.seqTracks.iterator()[0])[0].delete(flush: true)
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getReadGroupName(roddyBamFile.seqTracks.iterator()[0])
        }
    }

    @Test
    void testGetReadGroupName_SameDataFileName_ShouldFail() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        assert DomainFactory.buildSequenceDataFile([seqTrack: seqTrack]).save(flush: true)
        TestCase.shouldFail(AssertionError) {
            RoddyBamFile.getReadGroupName(roddyBamFile.seqTracks.iterator()[0])
        }
    }

    @Test
    void testGetReadGroupName_AllFine() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        assert "run${seqTrack.run.name}_${COMMON_PREFIX}" == RoddyBamFile.getReadGroupName(roddyBamFile.seqTracks.iterator()[0])
    }

    @Test
    void testGetTmpRoddySingleLaneQADirectories_NoSeqTracks() {
        roddyBamFile.seqTracks = null
        assert roddyBamFile.tmpRoddySingleLaneQADirectories.isEmpty()
    }

    @Test
    void testGetTmpRoddySingleLaneQADirectories_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.tmpRoddySingleLaneQADirectories
    }

    @Test
    void testGetTmpRoddySingleLaneQADirectories_TwoSeqTracks() {
        updateDataFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(roddyBamFile.workPackage)
        updateDataFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, File> expected = new HashMap<>()
        roddyBamFile.seqTracks.each {
            File dir = new File("${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        Map<SeqTrack, File> actual = roddyBamFile.tmpRoddySingleLaneQADirectories
        assert  expected == actual
    }

    @Test
    void testGetTmpRoddySingleLaneQAJsonFiles_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${QUALITY_CONTROL_JSON_FILE_NAME}")
        assert [(seqTrack): file] == roddyBamFile.tmpRoddySingleLaneQAJsonFiles
    }

    @Test
    void testGetFinalRoddySingleLaneQADirectories_NoSeqTracks() {
        roddyBamFile.seqTracks = null
        assert roddyBamFile.finalRoddySingleLaneQADirectories.isEmpty()
    }

    @Test
    void testGetFinalRoddySingleLaneQADirectories_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.finalRoddySingleLaneQADirectories
    }

    @Test
    void testGetFinalRoddySingleLaneQADirectories_TwoSeqTracks() {
        updateDataFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(roddyBamFile.workPackage)
        updateDataFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, File> expected = new HashMap<>()
        roddyBamFile.seqTracks.each {
            File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        Map<SeqTrack, File> actual = roddyBamFile.finalRoddySingleLaneQADirectories
        assert  expected == actual
    }

    @Test
    void testGetFinalRoddySingleLaneQAJsonFiles_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${QUALITY_CONTROL_JSON_FILE_NAME}")
        assert [(seqTrack): file] == roddyBamFile.finalRoddySingleLaneQAJsonFiles
    }

    @Test
    void testGetRoddySingleLaneQADirectoriesHelper_FinalFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getRoddySingleLaneQADirectoriesHelper(roddyBamFile.finalQADirectory)
    }

    @Test
    void testGetRoddySingleLaneQADirectoriesHelper_TmpFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getRoddySingleLaneQADirectoriesHelper(roddyBamFile.tmpRoddyQADirectory)
    }

    @Test
    void testGetLatestTmpRoddyExecutionDirectory_WhenRoddyExecutionDirectoryNamesEmpty_ShouldFail() {
        shouldFail(RuntimeException) {
            roddyBamFile.getLatestTmpRoddyExecutionDirectory()
        }
    }

    @Test
    void testGetLatestTmpRoddyExecutionDirectory_WhenLatestDirectoryNameIsNotLastNameInRoddyExecutionDirectoryNames_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.addAll(["exec_100000_000000000_a_a", "exec_000000_000000000_a_a"])
        roddyBamFile.save(flush: true)

        shouldFail(AssertionError) {
            roddyBamFile.getLatestTmpRoddyExecutionDirectory()
        }
    }

    void testGetLatestTmpRoddyExecutionDirectory_WhenLatestDirectoryNameDoesNotMatch_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.add("someName")

        shouldFail(AssertionError) {
            roddyBamFile.getLatestTmpRoddyExecutionDirectory()
        }
    }

    @Test
    void testGetLatestTmpRoddyExecutionDirectory_WhenLatestDirectoryNameDoesNotExistOnFileSystem_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_DIR_NAME)

        shouldFail(AssertionError) {
            roddyBamFile.getLatestTmpRoddyExecutionDirectory()
        }
    }

    @Test
    void testGetLatestTmpRoddyExecutionDirectory_WhenLatestDirectoryNameIsNoDirectory_ShouldFail() {
        String fileName = RODDY_EXECUTION_DIR_NAME

        tmpDir.newFile(fileName)

        roddyBamFile.roddyExecutionDirectoryNames.add(fileName)

        shouldFail(AssertionError) {
            roddyBamFile.getLatestTmpRoddyExecutionDirectory()
        }
    }

    @Test
    void testGetLatestTmpRoddyExecutionDirectory_WhenAllFine_ReturnLatestTmpRoddyExecutionDirectory() {
        CreateJobStateLogFileHelper.withTmpRoddyExecutionDir(tmpDir, { File roddyExecutionDir ->
            roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_DIR_NAME)
            assert roddyExecutionDir == roddyBamFile.getLatestTmpRoddyExecutionDirectory()
        }, RODDY_EXECUTION_DIR_NAME)
    }

    private void updateDataFileNames(SeqTrack seqTrack) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        dataFiles[0].fileName = FIRST_DATAFILE_NAME
        dataFiles[1].fileName = SECOND_DATAFILE_NAME
    }
}
