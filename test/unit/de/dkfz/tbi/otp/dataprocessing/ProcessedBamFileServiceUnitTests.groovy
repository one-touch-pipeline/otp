package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.junit.*

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
        AlignmentPass.metaClass.isLatestPass= {true}
    }

    private ProcessedBamFileService createProcessedBamFileService() {
        final String pbfTempDir = TestCase.getUniqueNonExistentPath() as String
        ProcessedBamFileService processedBamFileService = new ProcessedBamFileService()
        processedBamFileService.processedAlignmentFileService = [
            getDirectory: { AlignmentPass alignmentPass -> return pbfTempDir}
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

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
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

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedBamFileService.deleteProcessingFiles(null) //
        }
    }
}
