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

@TestFor(ProcessedSaiFileService)
@Build([
        MergingCriteria,
        ProcessedSaiFile,
        ReferenceGenome,
])
class ProcessedSaiFileServiceUnitTests {

    ProcessedSaiFileService processedSaiFileService

    @Before
    void setUp() throws Exception {
        //if something failed and the toString method is called, the criteria in isLatestPass makes Problems
        //Therefore this method is mocked.
        AlignmentPass.metaClass.isLatestPass = { true }

        final String saiFileProcessingDir = TestCase.getUniqueNonExistentPath() as String
        processedSaiFileService = new ProcessedSaiFileService()
        processedSaiFileService.processedAlignmentFileService = [
                getDirectory: { AlignmentPass alignmentPass -> return saiFileProcessingDir }
        ] as ProcessedAlignmentFileService
    }


    @Test
    void testCheckConsistencyForProcessingFilesDeletion() {
        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build()
        processedSaiFileService.dataProcessingFilesService = [
                checkConsistencyWithDatabaseForDeletion: { final def dbFile, final File fsFile ->
                    File filePath = processedSaiFileService.getFilePath(processedSaiFile) as File
                    assert processedSaiFile == dbFile
                    assert filePath == fsFile
                    return true
                },
        ] as DataProcessingFilesService

        assert processedSaiFileService.checkConsistencyForProcessingFilesDeletion(processedSaiFile)
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_ProcessedSaiFileIsNull() {
        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail(IllegalArgumentException) {
            processedSaiFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }


    @Test
    void testDeleteProcessingFiles() {
        final int FILE_LENGTH = 10
        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build()
        processedSaiFileService.dataProcessingFilesService = [
                deleteProcessingFiles: { final def dbFile, final File fsFile, final File... additionalFiles ->
                    File filePath = processedSaiFileService.getFilePath(processedSaiFile) as File
                    File errorFile = processedSaiFileService.bwaAlnErrorLogFilePath(processedSaiFile) as File
                    assert processedSaiFile == dbFile
                    assert filePath == fsFile
                    assert [errorFile] == additionalFiles
                    return FILE_LENGTH
                }
        ] as DataProcessingFilesService
        assert FILE_LENGTH == processedSaiFileService.deleteProcessingFiles(processedSaiFile)
    }

    @Test
    void testDeleteProcessingFiles_ProcessedSaiFileIsNull() {
        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail(IllegalArgumentException) {
            processedSaiFileService.deleteProcessingFiles(null) //
        }
    }
}
