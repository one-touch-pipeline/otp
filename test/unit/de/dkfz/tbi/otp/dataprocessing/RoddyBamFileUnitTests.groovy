package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import grails.buildtestdata.mixin.Build
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static de.dkfz.tbi.TestCase.shouldFail
import static de.dkfz.tbi.otp.dataprocessing.RoddyBamFile.RODDY_EXECUTION_STORE_DIR
import static de.dkfz.tbi.otp.dataprocessing.RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME

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
    void testGetWorkDirectory_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}" == roddyBamFile.workDirectory.path
    }

    @Test
    void testGetWorkQADirectory_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.getWorkQADirectory().path
    }

    @Test
    void testGetFinalQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.finalQADirectory.path
    }

    @Test
    void testGetWorkExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.workExecutionStoreDirectory.path
    }

    @Test
    void testGetFinalRoddyExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.finalExecutionStoreDirectory.path
    }

    @Test
    void testGetWorkBamFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.workBamFile.path
    }

    @Test
    void testGetWorkBaiFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.workBaiFile.path
    }

    @Test
    void testGetWorkMd5sumFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.workMd5sumFile.path
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
    void testGetWorkMergedQADirectory_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.workMergedQADirectory.path
    }

    @Test
    void testGetWorkMergedQAJsonFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}/${QUALITY_CONTROL_JSON_FILE_NAME}" ==
                roddyBamFile.workMergedQAJsonFile.path
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
    void testGetWorkSingleLaneQADirectories_NoSeqTracks() {
        roddyBamFile.seqTracks = null
        assert roddyBamFile.workSingleLaneQADirectories.isEmpty()
    }

    @Test
    void testGetWorkSingleLaneQADirectories_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.workSingleLaneQADirectories
    }

    @Test
    void testGetWorkSingleLaneQADirectories_TwoSeqTracks() {
        updateDataFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(roddyBamFile.workPackage)
        updateDataFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, File> expected = new HashMap<>()
        roddyBamFile.seqTracks.each {
            File dir = new File("${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        Map<SeqTrack, File> actual = roddyBamFile.workSingleLaneQADirectories
        assert  expected == actual
    }

    @Test
    void testGetWorkSingleLaneQAJsonFiles_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${QUALITY_CONTROL_JSON_FILE_NAME}")
        assert [(seqTrack): file] == roddyBamFile.workSingleLaneQAJsonFiles
    }

    @Test
    void testGetFinalRoddySingleLaneQADirectories_NoSeqTracks() {
        roddyBamFile.seqTracks = null
        assert roddyBamFile.finalSingleLaneQADirectories.isEmpty()
    }

    @Test
    void testGetFinalRoddySingleLaneQADirectories_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.finalSingleLaneQADirectories
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

        Map<SeqTrack, File> actual = roddyBamFile.finalSingleLaneQADirectories
        assert  expected == actual
    }

    @Test
    void testGetFinalRoddySingleLaneQAJsonFiles_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${QUALITY_CONTROL_JSON_FILE_NAME}")
        assert [(seqTrack): file] == roddyBamFile.finalSingleLaneQAJsonFiles
    }

    @Test
    void testGetRoddySingleLaneQADirectoriesHelper_FinalFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getSingleLaneQADirectoriesHelper(roddyBamFile.finalQADirectory)
    }

    @Test
    void testGetRoddySingleLaneQADirectoriesHelper_WorkFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${TEST_DIR}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getSingleLaneQADirectoriesHelper(roddyBamFile.workQADirectory)
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenRoddyExecutionDirectoryNamesEmpty_ShouldFail() {
        shouldFail(RuntimeException) {
            roddyBamFile.getLatestWorkExecutionDirectory()
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameIsNotLastNameInRoddyExecutionDirectoryNames_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.addAll(["exec_100000_000000000_a_a", "exec_000000_000000000_a_a"])
        roddyBamFile.save(flush: true)

        shouldFail(AssertionError) {
            roddyBamFile.getLatestWorkExecutionDirectory()
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameDoesNotMatch_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.add("someName")

        shouldFail(AssertionError) {
            roddyBamFile.getLatestWorkExecutionDirectory()
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameDoesNotExistOnFileSystem_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_DIR_NAME)

        shouldFail(AssertionError) {
            roddyBamFile.getLatestWorkExecutionDirectory()
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameIsNoDirectory_ShouldFail() {
        String fileName = RODDY_EXECUTION_DIR_NAME

        tmpDir.newFile(fileName)

        roddyBamFile.roddyExecutionDirectoryNames.add(fileName)

        shouldFail(AssertionError) {
            roddyBamFile.getLatestWorkExecutionDirectory()
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenAllFineAndTimeStampWith8Digits_ReturnLatestWorkExecutionDirectory() {
        helperTestGetLatestWorkExecutionDirectory_WhenAllFine('exec_000000_00000000_a_a')
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenAllFineAndTimeStampWith9Digits_ReturnLatestWorkExecutionDirectory() {
        helperTestGetLatestWorkExecutionDirectory_WhenAllFine(RODDY_EXECUTION_DIR_NAME)
    }

    void helperTestGetLatestWorkExecutionDirectory_WhenAllFine(String roddyExecutionDirName) {
        CreateJobStateLogFileHelper.withWorkExecutionDir(tmpDir, { File roddyExecutionDir ->
            roddyBamFile.roddyExecutionDirectoryNames.add(roddyExecutionDirName)
            assert roddyExecutionDir == roddyBamFile.getLatestWorkExecutionDirectory()
        }, roddyExecutionDirName)
    }

    @Test
    void testFinalRoddyExecutionDirectories_noRoddyExecutionDirsExist() {
        helperTestFinalRoddyExecutionDirectories([])
    }

    @Test
    void testFinalRoddyExecutionDirectories_allFine() {
        helperTestFinalRoddyExecutionDirectories([
                'exec_123456_123456789_bla_bla',
                'exec_654321_987654321_bla_bla'
        ])
    }



    @Test
    void testIsOldStructureUsed_useOldStructure_shouldReturnTrue() {
        roddyBamFile.workDirectoryName = null

        assert roddyBamFile.isOldStructureUsed()
    }

    @Test
    void testIsOldStructureUsed_useLinkStructure_shouldReturnFalse() {
        roddyBamFile.workDirectoryName = 'someWorkDirectory'

        assert !roddyBamFile.isOldStructureUsed()
    }

    @Test
    void testGetPathForFurtherProcessing_useOldStructure_shouldReturnFinalDir() {
        roddyBamFile.workDirectoryName = null
        assert roddyBamFile.save(flush: true, failOnError: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true, failOnError: true)

        assert roddyBamFile.finalBamFile == roddyBamFile.getPathForFurtherProcessing()
    }

    @Test
    void testGetPathForFurtherProcessing_useNewStructure_shouldReturnWorkDir() {
        roddyBamFile.workDirectoryName = 'someDir'
        assert roddyBamFile.save(flush: true, failOnError: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true, failOnError: true)

        assert roddyBamFile.workBamFile == roddyBamFile.getPathForFurtherProcessing()
    }

    @Test
    void testGetPathForFurtherProcessing_useNewStructure_notSetInMergingWorkPackage_shouldThrowException() {
        roddyBamFile.workDirectoryName = 'someDir'
        assert roddyBamFile.save(flush: true, failOnError: true)
        TestCase.shouldFail(IllegalStateException) {
            roddyBamFile.getPathForFurtherProcessing()
        }
    }


    void helperTestFinalRoddyExecutionDirectories(List<String> roddyExecutionDirectoryNames) {
        roddyBamFile.roddyExecutionDirectoryNames.addAll(roddyExecutionDirectoryNames)
        List<String> expectedResult = roddyExecutionDirectoryNames.collect {
            "${TEST_DIR}/${RODDY_EXECUTION_STORE_DIR}/${it}"
        }
        assert expectedResult == roddyBamFile.finalExecutionDirectories*.path
    }

    private void updateDataFileNames(SeqTrack seqTrack) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        dataFiles[0].fileName = FIRST_DATAFILE_NAME
        dataFiles[1].fileName = SECOND_DATAFILE_NAME
    }
}
