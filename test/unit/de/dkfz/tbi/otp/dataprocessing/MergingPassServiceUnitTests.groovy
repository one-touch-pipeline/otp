
package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*

import org.junit.*

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CheckedLogger
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@TestFor(MergingPassService)
@Build([
    QualityAssessmentMergedPass
])
class MergingPassServiceUnitTests {

    private static final SOME_FILE_LENGTH_1 = 10
    private static final SOME_FILE_LENGTH_2 = 100

    CheckedLogger checkedLogger

    MergingPass mergingPass
    ProcessedMergedBamFile processedMergedBamFile
    QualityAssessmentMergedPass qualityAssessmentMergedPass
    Date createdBefore

    MergingPassService mergingPassService

    @Before
    void setUp() {
        checkedLogger = new CheckedLogger()
        LogThreadLocal.setThreadLog(checkedLogger)
    }

    @After
    void tearDown() {
        LogThreadLocal.removeThreadLog()
        checkedLogger.assertAllMessagesConsumed()
    }

    private void createDomains() {
        mergingPass = MergingPass.build()
        processedMergedBamFile = ProcessedMergedBamFile.build(mergingPass: mergingPass, workPackage: mergingPass.mergingWorkPackage)
        qualityAssessmentMergedPass = QualityAssessmentMergedPass.build(abstractMergedBamFile: processedMergedBamFile)
    }

    private void createTestData() {
        createDomains()

        final File qaProcessingDirectory = TestCase.getUniqueNonExistentPath()
        mergingPassService = new MergingPassService()
        mergingPassService.processedMergedBamFileQaFileService = [
            qaProcessingDirectory: { final MergingPass mergingPass ->
                assert this.mergingPass == mergingPass
                return qaProcessingDirectory
            },
            checkConsistencyForProcessingFilesDeletion: { final QualityAssessmentMergedPass pass ->
                return true //
            },
            deleteProcessingFiles: { final QualityAssessmentMergedPass pass->
                return SOME_FILE_LENGTH_1 //
            },
        ] as ProcessedMergedBamFileQaFileService

        final String pmbfProcessingDirectory = TestCase.getUniqueNonExistentPath() as String
        mergingPassService.processedMergedBamFileService = [
            processingDirectory: { MergingPass mergingPass ->
                assert this.mergingPass == mergingPass
                return pmbfProcessingDirectory
            },
            checkConsistencyForProcessingFilesDeletion: { final ProcessedMergedBamFile bamFile ->
                assert processedMergedBamFile == bamFile
                return true
            },
            deleteProcessingFiles: { final ProcessedMergedBamFile bamFile ->
                assert processedMergedBamFile == bamFile
                return SOME_FILE_LENGTH_2
            },
        ] as ProcessedMergedBamFileService

        mergingPassService.dataProcessingFilesService = [
            deleteProcessingDirectory: { final Project project, final File directoryPath ->
                assert mergingPass.project == project
                return
            }
        ] as DataProcessingFilesService

        //method is overloaded, this can't be done both via map
        mergingPassService.dataProcessingFilesService.metaClass.deleteProcessingDirectory = { final Project project, final String directoryPath ->
            assert mergingPass.project == project
            return
        }
    }



    @Test
    void testDeleteProcessingFiles() {
        createTestData()
        int expected = SOME_FILE_LENGTH_1 + SOME_FILE_LENGTH_2
        checkedLogger.addDebug("${expected} bytes have been freed for merging pass ${mergingPass}.")
        assert expected == mergingPassService.deleteProcessingFiles(mergingPass)
    }

    @Test
    void testDeleteProcessingFiles_NoMergingPass() {
        mergingPassService = new MergingPassService()
        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            mergingPassService.deleteProcessingFiles(null) //
        }
    }

    @Test
    void testDeleteProcessingFiles_NoProcessedMergedBamFile() {
        mergingPassService = new MergingPassService()
        mergingPass = MergingPass.build()
        checkedLogger.addError("Found 0 ProcessedMergedBamFiles for MergingPass ${mergingPass}. That's weird. Skipping that merging pass.")
        assert 0 == mergingPassService.deleteProcessingFiles(mergingPass)
    }

    @Test
    void testDeleteProcessingFiles_MultipleQA() {
        createTestData()
        QualityAssessmentMergedPass.build(identifier: 1, abstractMergedBamFile: processedMergedBamFile)
        QualityAssessmentMergedPass.build(identifier: 2, abstractMergedBamFile: processedMergedBamFile)
        int expected = SOME_FILE_LENGTH_1 * 3 + SOME_FILE_LENGTH_2
        checkedLogger.addDebug("${expected} bytes have been freed for merging pass ${mergingPass}.")
        assert expected == mergingPassService.deleteProcessingFiles(mergingPass)
    }

    @Test
    void testDeleteProcessingFiles_NoQA() {
        createTestData()
        qualityAssessmentMergedPass.delete(flush: true)
        int expected = SOME_FILE_LENGTH_2
        checkedLogger.addDebug("${expected} bytes have been freed for merging pass ${mergingPass}.")
        assert expected == mergingPassService.deleteProcessingFiles(mergingPass)
    }

    @Test
    void testDeleteProcessingFiles_InconsistenceInMergedQA() {
        createTestData()
        checkedLogger.addError("There was at least one inconsistency (see earlier log message(s)) for merging pass ${mergingPass}. Skipping that merging pass.")
        mergingPassService.processedMergedBamFileQaFileService.metaClass.checkConsistencyForProcessingFilesDeletion = { final QualityAssessmentMergedPass pass -> return false }
        assert 0 == mergingPassService.deleteProcessingFiles(mergingPass)
    }

    @Test
    void testDeleteProcessingFiles_InconsistenceInProcessedMergedBamFile() {
        createTestData()
        qualityAssessmentMergedPass.delete(flush: true)
        checkedLogger.addError("There was at least one inconsistency (see earlier log message(s)) for merging pass ${mergingPass}. Skipping that merging pass.")
        mergingPassService.processedMergedBamFileService.metaClass.checkConsistencyForProcessingFilesDeletion = { final ProcessedMergedBamFile pass -> return false }
        assert 0 == mergingPassService.deleteProcessingFiles(mergingPass)
    }




    private void createDataForMayProcessingFilesBeDeleted(Boolean hasBeenQualityAssessedAndMerged) {
        createdBefore = new Date().plus(1) //some date later the the bam file

        mergingPassService = new MergingPassService()
        mergingPassService.abstractBamFileService = [
            hasBeenQualityAssessedAndMerged: {final AbstractBamFile bamFile, final Date before ->
                if (hasBeenQualityAssessedAndMerged == null) {
                    fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
                } else {
                    return hasBeenQualityAssessedAndMerged as boolean
                }
            }
        ] as AbstractBamFileService
    }



    /*
     * Some test cases for MayProcessingFilesBeDeleted case are written as Integration test, since the unit tesst
     * couldn't execute the criteria (which always return with null).
     */
    @Test
    void testMayProcessingFilesBeDeleted() {
        createDomains()
        createDataForMayProcessingFilesBeDeleted(false)

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_NoMergingPass() {
        createDataForMayProcessingFilesBeDeleted()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            mergingPassService.mayProcessingFilesBeDeleted(null, createdBefore) //
        }
    }

    @Test
    void testMayProcessingFilesBeDeleted_NoDateCreatedBefore() {
        createDomains()
        createDataForMayProcessingFilesBeDeleted()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            mergingPassService.mayProcessingFilesBeDeleted(mergingPass, null) //
        }
    }

    @Test
    void testMayProcessingFilesBeDeleted_NoProcessedMergedBamFile() {
        mergingPass = MergingPass.build()
        createDataForMayProcessingFilesBeDeleted()
        checkedLogger.addError("Found 0 ProcessedMergedBamFiles for MergingPass ${mergingPass}. That's weird.")

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_FileToNew() {
        createDomains()
        createDataForMayProcessingFilesBeDeleted()
        createdBefore = processedMergedBamFile.dateCreated.plus(-1) //some date before the the bam file is created

        assert !mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    //already copied mean, that merged quality assessment workflow and transfere workflow has run successfully
    @Test
    void testMayProcessingFilesBeDeleted_FileAlreadyCopied() {
        createDomains()
        createDataForMayProcessingFilesBeDeleted()
        processedMergedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
        processedMergedBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        processedMergedBamFile.md5sum = "12345678901234567890123456789012"
        processedMergedBamFile.fileSize = 10000
        assert processedMergedBamFile.save(flush: true)

        assert mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }

    @Test
    void testMayProcessingFilesBeDeleted_HasBeenQualityAssessedAndMerged() {
        createDomains()
        createDataForMayProcessingFilesBeDeleted(true)

        assert mergingPassService.mayProcessingFilesBeDeleted(mergingPass, createdBefore)
    }



    @Test
    void testDeleteOldMergingProcessingFiles() {
        final int MAX_RUNTIME = 1000
        final int SOME_FILE_SIZE = 10
        createdBefore = new Date()
        mergingPassService = new MergingPassService()
        mergingPassService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                assert mergingPassService == passService
                assert "merging" == passTypeName
                assert this.createdBefore == createdBefore
                assert MAX_RUNTIME == millisMaxRuntime
                return SOME_FILE_SIZE
            }
        ] as DataProcessingFilesService

        assert SOME_FILE_SIZE == mergingPassService.deleteOldMergingProcessingFiles(createdBefore, MAX_RUNTIME)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_WithoutDefault() {
        final int SOME_FILE_SIZE = 10
        createdBefore = new Date()
        mergingPassService = new MergingPassService()
        mergingPassService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                assert mergingPassService == passService
                assert "merging" == passTypeName
                assert this.createdBefore == createdBefore
                assert Long.MAX_VALUE == millisMaxRuntime
                return SOME_FILE_SIZE
            }
        ] as DataProcessingFilesService

        assert SOME_FILE_SIZE == mergingPassService.deleteOldMergingProcessingFiles(createdBefore)
    }

    @Test
    void testDeleteOldMergingProcessingFiles_createDateIsNull() {
        createdBefore = new Date()
        mergingPassService = new MergingPassService()
        mergingPassService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
            }
        ] as DataProcessingFilesService

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            mergingPassService.deleteOldMergingProcessingFiles(null)
        }
    }

}
