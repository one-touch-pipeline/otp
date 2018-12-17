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
