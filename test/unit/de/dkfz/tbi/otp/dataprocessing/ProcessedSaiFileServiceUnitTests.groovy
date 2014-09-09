
package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*

import org.junit.*

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ProcessedSaiFileService)
@Build([
    ProcessedSaiFile,
])
class ProcessedSaiFileServiceUnitTests {

    ProcessedSaiFileService processedSaiFileService



    public void setUp() throws Exception {
        //if something failed and the toString method is called, the criteria in isLatestPass makes Problems
        //Therefore this method is mocked.
        AlignmentPass.metaClass.isLatestPass= {true}

        processedSaiFileService = new ProcessedSaiFileService()
        processedSaiFileService.processedAlignmentFileService = [
            getDirectory: { AlignmentPass alignmentPass -> return TestConstants.BASE_TEST_DIRECTORY}
        ] as ProcessedAlignmentFileService
    }



    public void testCheckConsistencyForProcessingFilesDeletion() {
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

    public void testCheckConsistencyForProcessingFilesDeletion_ProcessedSaiFileIsNull() {
        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedSaiFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }



    public void testDeleteProcessingFiles() {
        final int FILE_LENGTH = 10
        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build()
        processedSaiFileService.dataProcessingFilesService = [
            deleteProcessingFiles: { final def dbFile, final File fsFile, final File... additionalFiles ->
                File filePath = processedSaiFileService.getFilePath(processedSaiFile) as File
                File errorFile = processedSaiFileService.bwaAlnErrorLogFilePath(processedSaiFile) as File
                assert processedSaiFile == dbFile
                assert filePath == fsFile
                assert [errorFile]== additionalFiles
                return FILE_LENGTH
            }
        ] as DataProcessingFilesService
        assert FILE_LENGTH == processedSaiFileService.deleteProcessingFiles(processedSaiFile)
    }

    public void testDeleteProcessingFiles_ProcessedSaiFileIsNull() {
        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedSaiFileService.deleteProcessingFiles(null) //
        }
    }
}
