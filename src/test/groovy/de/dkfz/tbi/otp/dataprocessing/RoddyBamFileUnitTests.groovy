/*
 * Copyright 2011-2020 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import static de.dkfz.tbi.TestCase.shouldFail

@Mock([
        AbstractMergedBamFile,
        SoftwareTool,
        MergingCriteria,
        MergingWorkPackage,
        LibraryPreparationKit,
        SeqPlatform,
        SeqPlatformGroup,
        SeqCenter,
        SeqType,
        SeqTrack,
        RoddyBamFile,
        SampleType,
        Pipeline,
        ProcessingPriority,
        Project,
        ProcessingPriority,
        Individual,
        Sample,
        SeqPlatformModelLabel,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        FastqImportInstance,
        FileType,
        DataFile,
        Realm,
        RoddyWorkflowConfig,
        Run,
])
class RoddyBamFileUnitTests {

    TestConfigService configService

    static final String RODDY_EXECUTION_DIR_NAME = "exec_000000_000000000_a_a"
    SampleType sampleType
    Individual individual
    RoddyBamFile roddyBamFile
    String testDir

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    static final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    static final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    static final String COMMON_PREFIX = "4_NoIndex_L004"

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [],
        ])
        sampleType = roddyBamFile.sampleType
        individual = roddyBamFile.individual
        configService = new TestConfigService(tmpDir.newFolder())
        testDir = "${individual.getViewByPidPath(roddyBamFile.seqType).absoluteDataManagementPath.path}/${sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment"
    }

    @After
    void tearDown() {
        sampleType = null
        individual = null
        roddyBamFile = null
    }

    @Test
    void testGetRoddyBamFileName() {
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam" == roddyBamFile.bamFileName
    }

    @Test
    void testGetRoddyBaiFileName() {
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.bai" == roddyBamFile.baiFileName
    }

    @Test
    void testGetRoddyMd5sumFileName() {
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.md5" == roddyBamFile.md5sumFileName
    }

    @Test
    void testGetWorkDirectory_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}" == roddyBamFile.workDirectory.path
    }

    @Test
    void testGetWorkQADirectory_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.workQADirectory.path
    }

    @Test
    void testGetFinalQADirectory_AllFine() {
        assert "${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.finalQADirectory.path
    }

    @Test
    void testGetWorkExecutionStoreDirectory_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.workExecutionStoreDirectory.path
    }

    @Test
    void testGetFinalRoddyExecutionStoreDirectory_AllFine() {
        assert "${testDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.finalExecutionStoreDirectory.path
    }

    @Test
    void testGetWorkBamFile_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.workBamFile.path
    }

    @Test
    void testGetWorkBaiFile_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.workBaiFile.path
    }

    @Test
    void testGetWorkMd5sumFile_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.workMd5sumFile.path
    }

    @Test
    void testGetFinalBamFile_AllFine() {
        assert "${testDir}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.finalBamFile.path
    }

    @Test
    void testGetFinalBaiFile_AllFine() {
        assert "${testDir}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.finalBaiFile.path
    }

    @Test
    void testGetFinalMd5sumFile_AllFine() {
        assert "${testDir}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.finalMd5sumFile.path
    }

    @Test
    void testGetWorkMergedQADirectory_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.workMergedQADirectory.path
    }

    @Test
    void testGetWorkMergedQAJsonFile_AllFine() {
        assert "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}" ==
                roddyBamFile.workMergedQAJsonFile.path
    }

    @Test
    void testGetFinalMergedQADirectory_AllFine() {
        assert "${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.finalMergedQADirectory.path
    }

    @Test
    void testGetFinalRoddyMergedQAJsonFile_AllFine() {
        assert "${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}" ==
                roddyBamFile.finalMergedQAJsonFile.path
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
        File dir = new File("${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.workSingleLaneQADirectories
    }

    @Test
    void testGetWorkSingleLaneQADirectories_TwoSeqTracks() {
        updateDataFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.workPackage)
        updateDataFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, File> expected = [:]
        roddyBamFile.seqTracks.each {
            File dir = new File("${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        Map<SeqTrack, File> actual = roddyBamFile.workSingleLaneQADirectories
        assert expected == actual
    }

    @Test
    void testGetWorkSingleLaneQAJsonFiles_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}")
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
        File dir = new File("${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.finalSingleLaneQADirectories
    }

    @Test
    void testGetFinalRoddySingleLaneQADirectories_TwoSeqTracks() {
        updateDataFileNames(roddyBamFile.seqTracks.iterator()[0])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.workPackage)
        updateDataFileNames(seqTrack)
        roddyBamFile.seqTracks.add(seqTrack)
        Map<SeqTrack, File> expected = [:]
        roddyBamFile.seqTracks.each {
            File dir = new File("${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${it.run.name}_${COMMON_PREFIX}")
            expected.put((it), dir)
        }

        Map<SeqTrack, File> actual = roddyBamFile.finalSingleLaneQADirectories
        assert expected == actual
    }

    @Test
    void testGetFinalRoddySingleLaneQAJsonFiles_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}")
        assert [(seqTrack): file] == roddyBamFile.finalSingleLaneQAJsonFiles
    }

    @Test
    void testGetRoddySingleLaneQADirectoriesHelper_FinalFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getSingleLaneQADirectoriesHelper(roddyBamFile.finalQADirectory)
    }

    @Test
    void testGetRoddySingleLaneQADirectoriesHelper_WorkFolder_OneSeqTrack() {
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")
        assert [(seqTrack): dir] == roddyBamFile.getSingleLaneQADirectoriesHelper(roddyBamFile.workQADirectory)
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenRoddyExecutionDirectoryNamesEmpty_ShouldFail() {
        shouldFail(RuntimeException) {
            roddyBamFile.latestWorkExecutionDirectory
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameIsNotLastNameInRoddyExecutionDirectoryNames_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.addAll(["exec_100000_000000000_a_a", "exec_000000_000000000_a_a"])
        roddyBamFile.save(flush: true)

        shouldFail(AssertionError) {
            roddyBamFile.latestWorkExecutionDirectory
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameDoesNotMatch_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.add("someName")

        shouldFail(AssertionError) {
            roddyBamFile.latestWorkExecutionDirectory
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameDoesNotExistOnFileSystem_ShouldFail() {
        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_DIR_NAME)

        shouldFail(AssertionError) {
            roddyBamFile.latestWorkExecutionDirectory
        }
    }

    @Test
    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameIsNoDirectory_ShouldFail() {
        String fileName = RODDY_EXECUTION_DIR_NAME

        tmpDir.newFile(fileName)

        roddyBamFile.roddyExecutionDirectoryNames.add(fileName)

        shouldFail(AssertionError) {
            roddyBamFile.latestWorkExecutionDirectory
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
        roddyBamFile.roddyExecutionDirectoryNames.add(roddyExecutionDirName)

        File file = new File(roddyBamFile.workExecutionStoreDirectory, roddyExecutionDirName)
        file.mkdirs()

        assert file == roddyBamFile.latestWorkExecutionDirectory
    }

    @Test
    void testFinalRoddyExecutionDirectories_noRoddyExecutionDirsExist() {
        helperTestFinalRoddyExecutionDirectories([])
    }

    @Test
    void testFinalRoddyExecutionDirectories_allFine() {
        helperTestFinalRoddyExecutionDirectories([
                'exec_123456_123456789_bla_bla',
                'exec_654321_987654321_bla_bla',
        ])
    }


    @Test
    void testIsOldStructureUsed_useOldStructure_shouldReturnTrue() {
        roddyBamFile.workDirectoryName = null

        assert roddyBamFile.oldStructureUsed
    }

    @Test
    void testIsOldStructureUsed_useLinkStructure_shouldReturnFalse() {
        roddyBamFile.workDirectoryName = 'someWorkDirectory'

        assert !roddyBamFile.oldStructureUsed
    }

    @Test
    void testGetPathForFurtherProcessing_useOldStructure_shouldReturnFinalDir() {
        roddyBamFile.workDirectoryName = null
        assert roddyBamFile.save(flush: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert roddyBamFile.finalBamFile == roddyBamFile.pathForFurtherProcessing
    }

    @Test
    void testGetPathForFurtherProcessing_useNewStructure_shouldReturnWorkDir() {
        roddyBamFile.workDirectoryName = 'someDir'
        assert roddyBamFile.save(flush: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert roddyBamFile.workBamFile == roddyBamFile.pathForFurtherProcessing
    }

    @Test
    void testGetPathForFurtherProcessing_useNewStructure_notSetInMergingWorkPackage_shouldThrowException() {
        roddyBamFile.workDirectoryName = 'someDir'
        assert roddyBamFile.save(flush: true)
        TestCase.shouldFail(IllegalStateException) {
            roddyBamFile.pathForFurtherProcessing
        }
    }

    void helperTestFinalRoddyExecutionDirectories(List<String> roddyExecutionDirectoryNames) {
        roddyBamFile.roddyExecutionDirectoryNames.addAll(roddyExecutionDirectoryNames)
        List<String> expectedResult = roddyExecutionDirectoryNames.collect {
            "${testDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/${it}"
        }
        assert expectedResult == roddyBamFile.finalExecutionDirectories*.path
    }

    private void updateDataFileNames(SeqTrack seqTrack) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        dataFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        dataFiles[1].vbpFileName = SECOND_DATAFILE_NAME
    }
}
