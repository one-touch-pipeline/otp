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

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Path

class RoddyBamFileDomainSpec extends Specification implements DomainUnitTest<RoddyBamFile> {

    TestConfigService configService

    static final String RODDY_EXECUTION_DIR_NAME = "exec_000000_000000000_a_a"
    SampleType sampleType
    Individual individual
    RoddyBamFile roddyBamFile
    String testDir

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
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
        ]
    }

    @TempDir
    Path tempDir

    static final String FIRST_DATAFILE_NAME = "4_NoIndex_L004_R1_complete_filtered.fastq.gz"
    static final String SECOND_DATAFILE_NAME = "4_NoIndex_L004_R2_complete_filtered.fastq.gz"
    static final String COMMON_PREFIX = "4_NoIndex_L004"

    void setupTest() {
        roddyBamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [],
        ])
        sampleType = roddyBamFile.sampleType
        individual = roddyBamFile.individual
        configService = new TestConfigService(tempDir)
        testDir = "${individual.getViewByPidPath(roddyBamFile.seqType).absoluteDataManagementPath.path}/${sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment"
    }

    void testGetRoddyBamFileName() {
        given:
        setupTest()

        expect:
        "${sampleType.dirName}_${individual.pid}_merged.mdup.bam" == roddyBamFile.bamFileName
    }

    void testGetRoddyBaiFileName() {
        given:
        setupTest()

        expect:
        "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.bai" == roddyBamFile.baiFileName
    }

    void testGetRoddyMd5sumFileName() {
        given:
        setupTest()

        expect:
        "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.md5" == roddyBamFile.md5sumFileName
    }

    void testGetWorkDirectory_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}" == roddyBamFile.workDirectory.path
    }

    void testGetWorkQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.workQADirectory.path
    }

    void testGetFinalQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.finalQADirectory.path
    }

    void testGetWorkExecutionStoreDirectory_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.workExecutionStoreDirectory.path
    }

    void testGetFinalRoddyExecutionStoreDirectory_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.finalExecutionStoreDirectory.path
    }

    void testGetWorkBamFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.workBamFile.path
    }

    void testGetWorkBaiFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.workBaiFile.path
    }

    void testGetWorkMd5sumFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.workMd5sumFile.path
    }

    void testGetFinalBamFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.finalBamFile.path
    }

    void testGetFinalBaiFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.finalBaiFile.path
    }

    void testGetFinalMd5sumFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.finalMd5sumFile.path
    }

    void testGetWorkMergedQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.workMergedQADirectory.path
    }

    void testGetWorkMergedQAJsonFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}" ==
                roddyBamFile.workMergedQAJsonFile.path
    }

    void testGetFinalMergedQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}" ==
                roddyBamFile.finalMergedQADirectory.path
    }

    void testGetFinalRoddyMergedQAJsonFile_AllFine() {
        given:
        setupTest()

        expect:
        "${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}" ==
                roddyBamFile.finalMergedQAJsonFile.path
    }

    void testGetWorkSingleLaneQADirectories_NoSeqTracks() {
        given:
        setupTest()
        roddyBamFile.seqTracks = null

        expect:
        roddyBamFile.workSingleLaneQADirectories.isEmpty()
    }

    void testGetWorkSingleLaneQADirectories_OneSeqTrack() {
        given:
        setupTest()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == roddyBamFile.workSingleLaneQADirectories
    }

    void testGetWorkSingleLaneQADirectories_TwoSeqTracks() {
        given:
        setupTest()
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

        expect:
        expected == actual
    }

    void testGetWorkSingleLaneQAJsonFiles_OneSeqTrack() {
        given:
        setupTest()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}")

        expect:
        [(seqTrack): file] == roddyBamFile.workSingleLaneQAJsonFiles
    }

    void testGetFinalRoddySingleLaneQADirectories_NoSeqTracks() {
        given:
        setupTest()
        roddyBamFile.seqTracks = null

        expect:
        roddyBamFile.finalSingleLaneQADirectories.isEmpty()
    }

    void testGetFinalRoddySingleLaneQADirectories_OneSeqTrack() {
        given:
        setupTest()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == roddyBamFile.finalSingleLaneQADirectories
    }

    void testGetFinalRoddySingleLaneQADirectories_TwoSeqTracks() {
        given:
        setupTest()
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

        expect:
        expected == actual
    }

    void testGetFinalRoddySingleLaneQAJsonFiles_OneSeqTrack() {
        given:
        setupTest()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File file = new File("${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}/${RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME}")

        expect:
        [(seqTrack): file] == roddyBamFile.finalSingleLaneQAJsonFiles
    }

    void testGetRoddySingleLaneQADirectoriesHelper_FinalFolder_OneSeqTrack() {
        given:
        setupTest()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${testDir}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == roddyBamFile.getSingleLaneQADirectoriesHelper(roddyBamFile.finalQADirectory)
    }

    void testGetRoddySingleLaneQADirectoriesHelper_WorkFolder_OneSeqTrack() {
        given:
        setupTest()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        updateDataFileNames(seqTrack)
        File dir = new File("${testDir}/${roddyBamFile.workDirectoryName}/${RoddyBamFile.QUALITY_CONTROL_DIR}/run${seqTrack.run.name}_${COMMON_PREFIX}")

        expect:
        [(seqTrack): dir] == roddyBamFile.getSingleLaneQADirectoriesHelper(roddyBamFile.workQADirectory)
    }

    void testGetLatestWorkExecutionDirectory_WhenRoddyExecutionDirectoryNamesEmpty_ShouldFail() {
        given:
        setupTest()

        when:
        roddyBamFile.latestWorkExecutionDirectory

        then:
        thrown AssertionError
    }

    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameIsNotLastNameInRoddyExecutionDirectoryNames_ShouldFail() {
        given:
        setupTest()
        roddyBamFile.roddyExecutionDirectoryNames.addAll(["exec_100000_000000000_a_a", "exec_000000_000000000_a_a"])
        roddyBamFile.save(flush: true)

        when:
        roddyBamFile.latestWorkExecutionDirectory

        then:
        thrown AssertionError
    }

    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameDoesNotMatch_ShouldFail() {
        given:
        setupTest()
        roddyBamFile.roddyExecutionDirectoryNames.add("someName")

        when:
        roddyBamFile.latestWorkExecutionDirectory

        then:
        thrown AssertionError
    }

    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameDoesNotExistOnFileSystem_ShouldFail() {
        given:
        setupTest()
        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_DIR_NAME)

        when:
        roddyBamFile.latestWorkExecutionDirectory

        then:
        thrown AssertionError
    }

    void testGetLatestWorkExecutionDirectory_WhenLatestDirectoryNameIsNoDirectory_ShouldFail() {
        given:
        setupTest()
        String fileName = RODDY_EXECUTION_DIR_NAME

        CreateFileHelper.createFile(tempDir.resolve(fileName))

        when:
        roddyBamFile.roddyExecutionDirectoryNames.add(fileName)
        roddyBamFile.latestWorkExecutionDirectory

        then:
        thrown AssertionError
    }

    void testGetLatestWorkExecutionDirectory_WhenAllFineAndTimeStampWith8Digits_ReturnLatestWorkExecutionDirectory() {
        given:
        setupTest()

        expect:
        helperTestGetLatestWorkExecutionDirectory_WhenAllFine('exec_000000_00000000_a_a')
    }

    void testGetLatestWorkExecutionDirectory_WhenAllFineAndTimeStampWith9Digits_ReturnLatestWorkExecutionDirectory() {
        given:
        setupTest()

        expect:
        helperTestGetLatestWorkExecutionDirectory_WhenAllFine(RODDY_EXECUTION_DIR_NAME)
    }

    private boolean helperTestGetLatestWorkExecutionDirectory_WhenAllFine(String roddyExecutionDirName) {
        roddyBamFile.roddyExecutionDirectoryNames.add(roddyExecutionDirName)

        File file = new File(roddyBamFile.workExecutionStoreDirectory, roddyExecutionDirName)
        file.mkdirs()

        return file == roddyBamFile.latestWorkExecutionDirectory
    }

    void testFinalRoddyExecutionDirectories_noRoddyExecutionDirsExist() {
        given:
        setupTest()

        expect:
        helperTestFinalRoddyExecutionDirectories([])
    }

    void testFinalRoddyExecutionDirectories_allFine() {
        given:
        setupTest()

        expect:
        helperTestFinalRoddyExecutionDirectories([
                'exec_123456_123456789_bla_bla',
                'exec_654321_987654321_bla_bla',
        ])
    }

    void testIsOldStructureUsed_useOldStructure_shouldReturnTrue() {
        given:
        setupTest()
        roddyBamFile.workDirectoryName = null

        expect:
        roddyBamFile.oldStructureUsed
    }

    void testIsOldStructureUsed_useLinkStructure_shouldReturnFalse() {
        given:
        setupTest()
        roddyBamFile.workDirectoryName = 'someWorkDirectory'

        expect:
        !roddyBamFile.oldStructureUsed
    }

    void testGetPathForFurtherProcessing_useOldStructure_shouldReturnFinalDir() {
        given:
        setupTest()
        roddyBamFile.workDirectoryName = null

        when:
        roddyBamFile.save(flush: true)
        roddyBamFile.mergingWorkPackage.save(flush: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile

        then:
        roddyBamFile.finalBamFile == roddyBamFile.pathForFurtherProcessing
    }

    void testGetPathForFurtherProcessing_useNewStructure_shouldReturnWorkDir() {
        given:
        setupTest()
        roddyBamFile.workDirectoryName = 'someDir'

        when:
        roddyBamFile.save(flush: true)
        roddyBamFile.mergingWorkPackage.save(flush: true)
        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile

        then:
        roddyBamFile.workBamFile == roddyBamFile.pathForFurtherProcessing
    }

    void testGetPathForFurtherProcessing_useNewStructure_notSetInMergingWorkPackage_shouldThrowException() {
        given:
        setupTest()
        roddyBamFile.workDirectoryName = 'someDir'

        when:
        roddyBamFile.save(flush: true)
        roddyBamFile.pathForFurtherProcessing

        then:
        thrown IllegalStateException
    }

    private boolean helperTestFinalRoddyExecutionDirectories(List<String> roddyExecutionDirectoryNames) {
        roddyBamFile.roddyExecutionDirectoryNames.addAll(roddyExecutionDirectoryNames)
        List<String> expectedResult = roddyExecutionDirectoryNames.collect {
            "${testDir}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/${it}"
        }

        return expectedResult == roddyBamFile.finalExecutionDirectories*.path
    }

    private void updateDataFileNames(SeqTrack seqTrack) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        dataFiles[0].vbpFileName = FIRST_DATAFILE_NAME
        dataFiles[1].vbpFileName = SECOND_DATAFILE_NAME
    }
}
