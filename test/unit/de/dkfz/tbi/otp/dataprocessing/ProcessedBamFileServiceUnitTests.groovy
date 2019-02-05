/*
 * Copyright 2011-2019 The OTP authors
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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome

@TestFor(ProcessedBamFileService)
@Build([
        MergingCriteria,
        ProcessedBamFile,
        ReferenceGenome,
])
class ProcessedBamFileServiceUnitTests {


    @Before
    void setUp() throws Exception {
        //if something failed and the toString method is called, the criteria in isLatestPass makes Problems
        //Therefore this method is mocked.
        AlignmentPass.metaClass.isLatestPass = { true }
    }

    private ProcessedBamFileService createProcessedBamFileService() {
        final String pbfTempDir = TestCase.getUniqueNonExistentPath() as String
        ProcessedBamFileService processedBamFileService = new ProcessedBamFileService()
        processedBamFileService.processedAlignmentFileService = [
                getDirectory: { AlignmentPass alignmentPass -> return pbfTempDir }
        ] as ProcessedAlignmentFileService
        return processedBamFileService
    }


    @Test
    void testCheckConsistencyForProcessingFilesDeletion() {
        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()
        processedBamFileService.dataProcessingFilesService = [
                checkConsistencyWithDatabaseForDeletion: { final def dbFile, final File fsFile ->
                    File filePath = processedBamFileService.getFilePath(processedBamFile) as File
                    assert processedBamFile == dbFile
                    assert filePath == fsFile
                    return true
                },
        ] as DataProcessingFilesService

        assert processedBamFileService.checkConsistencyForProcessingFilesDeletion(processedBamFile)
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_ProcessedBamFileIsNull() {
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail(IllegalArgumentException) {
            processedBamFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }


    @Test
    void testDeleteProcessingFiles() {
        final int FILE_LENGTH = 10
        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()
        processedBamFileService.dataProcessingFilesService = [
                deleteProcessingFiles: { final def dbFile, final File fsFile, final File... additionalFiles ->
                    File filePath = processedBamFileService.getFilePath(processedBamFile) as File
                    File[] expectedAdditionFiles = [
                            processedBamFileService.baiFilePath(processedBamFile) as File,
                            processedBamFileService.bwaSampeErrorLogFilePath(processedBamFile) as File,
                    ]
                    assert processedBamFile == dbFile
                    assert filePath == fsFile
                    assert expectedAdditionFiles == additionalFiles
                    return FILE_LENGTH
                },
        ] as DataProcessingFilesService

        assert FILE_LENGTH == processedBamFileService.deleteProcessingFiles(processedBamFile)
    }

    @Test
    void testDeleteProcessingFiles_ProcessedBamFileIsNull() {
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail(IllegalArgumentException) {
            processedBamFileService.deleteProcessingFiles(null) //
        }
    }
}
