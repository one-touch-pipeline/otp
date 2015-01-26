package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.dataprocessing.DataProcessingFilesService.OutputDirectories
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CheckedLogger
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.After
import org.springframework.context.ApplicationContext

@Build([
    AlignmentPass,
    ProcessedBamFile,
    QualityAssessmentPass,
    ProcessedSaiFile,
])
@TestFor(ProcessedAlignmentFileService)
class ProcessedAlignmentFileServiceUnitTests {

    private static final FILE_LENGTH_QUALITYFILE = 10
    private static final FILE_LENGTH_BAMFILE = 100
    private static final FILE_LENGTH_SAIFILE = 1000

    CheckedLogger checkedLogger



    public void setUp() {
        checkedLogger = new CheckedLogger()
        LogThreadLocal.setThreadLog(checkedLogger)
    }

    @After //Naming Convention does not work, need annotation
    public void tearDown() {
        LogThreadLocal.removeThreadLog()
        checkedLogger.assertAllMessagesConsumed()
    }

    private ProcessedAlignmentFileService createServiceForDeleteProcessingFiles() {
        ProcessedAlignmentFileService processedAlignmentFileService = new ProcessedAlignmentFileService()
        processedAlignmentFileService.dataProcessingFilesService = [
            deleteProcessingDirectory: {final Project project, final String directoryPath ->
                return
            },
            getOutputDirectory: {Individual individual, OutputDirectories dir->
                "SomeDirectory"
            }
        ] as DataProcessingFilesService

        processedAlignmentFileService.applicationContext = [:] as ApplicationContext

        processedAlignmentFileService.applicationContext.metaClass.processedBamFileQaFileService = [
            checkConsistencyForProcessingFilesDeletion: {final QualityAssessmentPass pass -> return true},
            deleteProcessingFiles: {final QualityAssessmentPass pass -> return FILE_LENGTH_QUALITYFILE},
            directoryPath: {AlignmentPass alignmentPass -> return "SomeDirectory"},
        ] as ProcessedBamFileQaFileService

        processedAlignmentFileService.applicationContext.metaClass.processedBamFileService = [
            checkConsistencyForProcessingFilesDeletion: {final ProcessedBamFile bamFile -> return true},
            deleteProcessingFiles: {final ProcessedBamFile bamFile -> return FILE_LENGTH_BAMFILE},
        ] as ProcessedBamFileService

        processedAlignmentFileService.applicationContext.metaClass.processedSaiFileService = [
            checkConsistencyForProcessingFilesDeletion: {final ProcessedSaiFile saiFile -> return true},
            deleteProcessingFiles: {final ProcessedSaiFile saiFile -> return FILE_LENGTH_SAIFILE},
        ] as ProcessedSaiFileService

        return processedAlignmentFileService
    }

    private AlignmentPass createTestDataForDeleteProcessingFiles(int countQaFiles = 1, int countProcessedSaiFiles = 1) {
        AlignmentPass alignmentPass = AlignmentPass.build()

        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            alignmentPass: alignmentPass
        ] )

        countQaFiles.times {
            QualityAssessmentPass.build([
                processedBamFile: processedBamFile,
                identifier: QualityAssessmentPass.nextIdentifier(processedBamFile),
            ])
        }

        countProcessedSaiFiles.times {
            ProcessedSaiFile.build([
                alignmentPass: alignmentPass
            ])
        }

        return alignmentPass
    }



    void testDeleteProcessingFiles() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles()
        final int expectedSize = FILE_LENGTH_BAMFILE + FILE_LENGTH_QUALITYFILE + FILE_LENGTH_SAIFILE
        checkedLogger.addDebug("${expectedSize} bytes have been freed for alignment pass ${alignmentPass}.")

        assert expectedSize == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_AlignmentPassIsNull() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedAlignmentFileService.deleteProcessingFiles(null) //
        }
    }

    void testDeleteProcessingFiles_NoBamFile() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = AlignmentPass.build()
        checkedLogger.addError("Found 0 ProcessedBamFiles for AlignmentPass ${alignmentPass}. That's weird. Skipping that alignment pass.")

        assert 0 == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_NoQAPasses() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles(0)
        final int expectedSize = FILE_LENGTH_BAMFILE + FILE_LENGTH_SAIFILE
        checkedLogger.addDebug("${expectedSize} bytes have been freed for alignment pass ${alignmentPass}.")

        assert expectedSize == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_FiveQAPasses() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles(5)
        final int expectedSize = FILE_LENGTH_BAMFILE + 5 * FILE_LENGTH_QUALITYFILE + FILE_LENGTH_SAIFILE
        checkedLogger.addDebug("${expectedSize} bytes have been freed for alignment pass ${alignmentPass}.")

        assert expectedSize == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_NoSaiFile() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles(1, 0)
        final int expectedSize = FILE_LENGTH_BAMFILE + FILE_LENGTH_QUALITYFILE
        checkedLogger.addDebug("${expectedSize} bytes have been freed for alignment pass ${alignmentPass}.")

        assert expectedSize == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_FiveSaiFiles() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles(1, 5)
        final int expectedSize = FILE_LENGTH_BAMFILE + FILE_LENGTH_QUALITYFILE + 5 * FILE_LENGTH_SAIFILE
        checkedLogger.addDebug("${expectedSize} bytes have been freed for alignment pass ${alignmentPass}.")

        assert expectedSize == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_QaNotConsistent() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles()
        processedAlignmentFileService.applicationContext.processedBamFileQaFileService.metaClass.checkConsistencyForProcessingFilesDeletion = {final QualityAssessmentPass pass -> return false}
        checkedLogger.addError("There was at least one inconsistency (see earlier log message(s)) for alignment pass ${alignmentPass}. Skipping that alignment pass.")

        assert 0 == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_BamNotConsistent() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles()
        processedAlignmentFileService.applicationContext.processedBamFileService.metaClass.checkConsistencyForProcessingFilesDeletion = {final ProcessedBamFile bamFile -> return false}
        checkedLogger.addError("There was at least one inconsistency (see earlier log message(s)) for alignment pass ${alignmentPass}. Skipping that alignment pass.")

        assert 0 == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }

    void testDeleteProcessingFiles_SaiNotConsistent() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForDeleteProcessingFiles()
        AlignmentPass alignmentPass = createTestDataForDeleteProcessingFiles()
        processedAlignmentFileService.applicationContext.processedSaiFileService.metaClass.checkConsistencyForProcessingFilesDeletion = {final ProcessedSaiFile saiFile -> return false}
        checkedLogger.addError("There was at least one inconsistency (see earlier log message(s)) for alignment pass ${alignmentPass}. Skipping that alignment pass.")

        assert 0 == processedAlignmentFileService.deleteProcessingFiles(alignmentPass)
    }



    private ProcessedAlignmentFileService createServiceForMayProcessingFilesBeDeleted(boolean hasBeenQualityAssessedAndMergedValue = true) {
        ProcessedAlignmentFileService processedAlignmentFileService = new ProcessedAlignmentFileService()
        processedAlignmentFileService.abstractBamFileService = [
            hasBeenQualityAssessedAndMerged: {final AbstractBamFile bamFile, final Date before ->
                return hasBeenQualityAssessedAndMergedValue
            }
        ] as AbstractBamFileService
        return processedAlignmentFileService
    }

    private AlignmentPass createTestDataForMayProcessingFilesBeDeleted(boolean createBamFile = true, boolean createSaiFile = false) {
        SeqTrack seqTrack = SeqTrack.build([
            alignmentState: SeqTrack.DataProcessingState.FINISHED
        ])

        AlignmentPass alignmentPass = AlignmentPass.build([
            seqTrack: seqTrack,
        ])

        if (createSaiFile) {
            ProcessedSaiFile.build([
                alignmentPass: alignmentPass
            ])
        }

        if (createBamFile) {
            ProcessedBamFile processedBamFile = ProcessedBamFile.build([
                alignmentPass: alignmentPass,
            ] )
        }

        return alignmentPass
    }



    void testMayProcessingFilesBeDeleted() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted()
        Date createdBefore = new Date().plus(1)

        assert processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    void testMayProcessingFilesBeDeleted_NoAlignmentPass() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        Date createdBefore = new Date().plus(1)

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedAlignmentFileService.mayProcessingFilesBeDeleted(null, createdBefore)
        }
    }

    void testMayProcessingFilesBeDeleted_NoDateCreatedBefore() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            assert processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, null)
        }
    }

    void testMayProcessingFilesBeDeleted_WithSaiFileCreatedBefore() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted(true, true)
        Date createdBefore = new Date().plus(1)

        assert processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    void testMayProcessingFilesBeDeleted_WithSaiFileCreatedAfter() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted(true, true)
        Date createdBefore = new Date().minus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    void testMayProcessingFilesBeDeleted_NoProcessedBamFile() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted(false)
        Date createdBefore = new Date().plus(1)
        checkedLogger.addError("Found 0 ProcessedBamFiles for AlignmentPass ${alignmentPass}. That's weird.")

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    void testMayProcessingFilesBeDeleted_ProcessedBamFileToYoung() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted()
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted()
        Date createdBefore = new Date().minus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    void testMayProcessingFilesBeDeleted_HasNotBeenQualityAndMerged() {
        ProcessedAlignmentFileService processedAlignmentFileService = createServiceForMayProcessingFilesBeDeleted(false)
        AlignmentPass alignmentPass = createTestDataForMayProcessingFilesBeDeleted()
        Date createdBefore = new Date().plus(1)

        assert !processedAlignmentFileService.mayProcessingFilesBeDeleted(alignmentPass, createdBefore)
    }

    //The tests for second pass are written as integration test, since unit test can not handle the used criteria



    void testDeleteOldAlignmentProcessingFiles() {
        final int MAX_RUNTIME = 1000
        final int SOME_FILE_SIZE = 10
        Date createdBeforeDate = new Date()
        ProcessedAlignmentFileService processedAlignmentFileService = new ProcessedAlignmentFileService()
        processedAlignmentFileService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                assert processedAlignmentFileService == passService
                assert "alignment" == passTypeName
                assert createdBeforeDate == createdBefore
                assert MAX_RUNTIME == millisMaxRuntime
                return SOME_FILE_SIZE
            }
        ] as DataProcessingFilesService

        assert SOME_FILE_SIZE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate, MAX_RUNTIME)
    }

    void testDeleteOldAlignmentProcessingFiles_WithDefault() {
        final int SOME_FILE_SIZE = 10
        Date createdBeforeDate = new Date()
        ProcessedAlignmentFileService processedAlignmentFileService = new ProcessedAlignmentFileService()
        processedAlignmentFileService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                assert processedAlignmentFileService == passService
                assert "alignment" == passTypeName
                assert createdBeforeDate == createdBefore
                assert Long.MAX_VALUE == millisMaxRuntime
                return SOME_FILE_SIZE
            }
        ] as DataProcessingFilesService

        assert SOME_FILE_SIZE == processedAlignmentFileService.deleteOldAlignmentProcessingFiles(createdBeforeDate)
    }

    void testDeleteOldAlignmentProcessingFiles_NoDateCreatedBefore() {
        final int MAX_RUNTIME = 1000
        final int SOME_FILE_SIZE = 10
        ProcessedAlignmentFileService processedAlignmentFileService = new ProcessedAlignmentFileService()
        processedAlignmentFileService.dataProcessingFilesService = [
            deleteOldProcessingFiles: {final Object passService, final String passTypeName, final Date createdBefore, final long millisMaxRuntime, final Closure<Collection> passesFunc ->
                fail "deleteOldProcessingFiles was called when it shouldn't be. Method under test should have failed earlier."
            }
        ] as DataProcessingFilesService

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedAlignmentFileService.deleteOldAlignmentProcessingFiles(null, MAX_RUNTIME)
        }
    }

}
