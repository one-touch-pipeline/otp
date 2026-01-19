/*
 * Copyright 2011-2026 The OTP authors
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
import de.dkfz.tbi.otp.infrastructure.alignment.RoddyBamFileNames
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Path
import java.nio.file.Paths

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
                AbstractBamFile,
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
                RawSequenceFile,
                RoddyWorkflowConfig,
                Run,
                FastqFile,
        ]
    }

    @TempDir
    Path tempDir

    void setupTest() {
        roddyBamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [],
        ])
        sampleType = roddyBamFile.sampleType
        individual = roddyBamFile.individual
        configService = new TestConfigService(tempDir)
        testDir = "${individual.getViewByPidPath(roddyBamFile.seqType).absoluteDataManagementPath.path}${File.separator}${sampleType.dirName}${File.separator}${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment"
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

    void testGetWorkDirectory_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.workDirectoryName).toFile() == roddyBamFile.workDirectory
    }

    void testGetWorkQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR).toFile() == roddyBamFile.workQADirectory
    }

    void testGetFinalQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, RoddyBamFileNames.QUALITY_CONTROL_DIR).toFile() == roddyBamFile.finalQADirectory
    }

    void testGetWorkExecutionStoreDirectory_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.workDirectoryName, RoddyBamFile.RODDY_EXECUTION_STORE_DIR).toFile() == roddyBamFile.workExecutionStoreDirectory
    }

    void testGetWorkBamFile_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.workDirectoryName, roddyBamFile.bamFileName).toFile() == roddyBamFile.workBamFile
    }

    void testGetWorkBaiFile_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.workDirectoryName, roddyBamFile.baiFileName).toFile() == roddyBamFile.workBaiFile
    }

    void testGetFinalBamFile_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.bamFileName).toFile() == roddyBamFile.finalBamFile
    }

    void testGetWorkMergedQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR, RoddyBamFileNames.MERGED_DIR).toFile() ==
                roddyBamFile.workMergedQADirectory
    }

    void testGetWorkMergedQAJsonFile_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir, roddyBamFile.workDirectoryName, RoddyBamFileNames.QUALITY_CONTROL_DIR, RoddyBamFileNames.MERGED_DIR, RoddyBamFileNames.QUALITY_CONTROL_JSON_FILE_NAME).toFile() ==
                roddyBamFile.workMergedQAJsonFile
    }

    void testGetFinalMergedQADirectory_AllFine() {
        given:
        setupTest()

        expect:
        Paths.get(testDir,
                RoddyBamFileNames.QUALITY_CONTROL_DIR,
                RoddyBamFileNames.MERGED_DIR).toFile() == roddyBamFile.finalMergedQADirectory
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
}
