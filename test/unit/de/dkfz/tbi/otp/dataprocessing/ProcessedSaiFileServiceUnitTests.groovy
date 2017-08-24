
package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.junit.*

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
        AlignmentPass.metaClass.isLatestPass= {true}

        processedSaiFileService = new ProcessedSaiFileService()
        processedSaiFileService.processedAlignmentFileService = [
            getDirectory: { AlignmentPass alignmentPass -> return TestConstants.BASE_TEST_DIRECTORY}
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
        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
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
                assert [errorFile]== additionalFiles
                return FILE_LENGTH
            }
        ] as DataProcessingFilesService
        assert FILE_LENGTH == processedSaiFileService.deleteProcessingFiles(processedSaiFile)
    }

    @Test
    void testDeleteProcessingFiles_ProcessedSaiFileIsNull() {
        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedSaiFileService.deleteProcessingFiles(null) //
        }
    }
}
