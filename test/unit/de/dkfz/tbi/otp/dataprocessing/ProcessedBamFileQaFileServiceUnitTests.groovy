package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.Build
import org.junit.After
import org.junit.Before
import org.junit.Test

@Build([
    MergingCriteria,
    QualityAssessmentPass,
    ReferenceGenome,
])
class ProcessedBamFileQaFileServiceUnitTests {

    @Before
    public void setUp() throws Exception {
        //these domain methods are mocked, since the contained critera makes problems
        QualityAssessmentPass.metaClass.isLatestPass = {true}
        AlignmentPass.metaClass.isLatestPass = {true}
    }

    @After
    public void tearDown() {
        QualityAssessmentPass.metaClass.isLatestPass = null
        AlignmentPass.metaClass.isLatestPass = null
    }


    private ProcessedBamFileQaFileService createProcessedBamFileQaFileService() {
        ProcessedBamFileQaFileService processedBamFileQaFileService = new ProcessedBamFileQaFileService()
        processedBamFileQaFileService.configService = [] as TestConfigService
        processedBamFileQaFileService.mergedAlignmentDataFileService = [
            buildRelativePath: { SeqType type, Sample sample -> return "RelativeDirectory"},
        ] as MergedAlignmentDataFileService
        processedBamFileQaFileService.processedAlignmentFileService = [
            getDirectory: { AlignmentPass alignmentPass -> return "AlignmentDirectory"},
            getRunLaneDirectory: { SeqTrack seqTrack -> return "RunLaneDirectory"},
        ] as ProcessedAlignmentFileService
        processedBamFileQaFileService.processedBamFileService = [
            getFileNameNoSuffix: { ProcessedBamFile bamFile -> return "BamFileName"},
        ] as ProcessedBamFileService
        return processedBamFileQaFileService
    }


    @Test
    public void testCheckConsistencyForProcessingFilesDeletion() {
        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.build()
        ProcessedBamFileQaFileService processedBamFileQaFileService = createProcessedBamFileQaFileService()
        processedBamFileQaFileService.dataProcessingFilesService = [
            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                File expectedProcessingDirectory = processedBamFileQaFileService.directoryPath(qualityAssessmentPass) as File
                File expectedFinalDestinationDirectory = processedBamFileQaFileService.finalDestinationDirectory(qualityAssessmentPass)
                Collection<String> expectedAdditionalFiles = processedBamFileQaFileService.allFileNames(qualityAssessmentPass.processedBamFile)
                assert expectedProcessingDirectory == processingDirectory
                assert expectedFinalDestinationDirectory == finalDestinationDirectory
                assert expectedAdditionalFiles == fileNames
                return true
            }
        ] as DataProcessingFilesService

        assert processedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(qualityAssessmentPass)
    }

    @Test
    public void testCheckConsistencyForProcessingFilesDeletion_QualityAssessmentPassIsNull() {
        ProcessedBamFileQaFileService processedBamFileQaFileService = createProcessedBamFileQaFileService()
        processedBamFileQaFileService.dataProcessingFilesService = [
            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
            }
        ] as DataProcessingFilesService

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }

    @Test
    public void testCheckConsistencyForProcessingFilesDeletion_notLastQualityAsssesmentPass() {
        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.build()
        ProcessedBamFileQaFileService processedBamFileQaFileService = createProcessedBamFileQaFileService()
        processedBamFileQaFileService.dataProcessingFilesService = [
            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
            }
        ] as DataProcessingFilesService
        QualityAssessmentPass.metaClass.isLatestPass = {false}

        assert processedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(qualityAssessmentPass)
    }

    @Test
    public void testCheckConsistencyForProcessingFilesDeletion_notLastAlignmentPass() {
        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.build()
        ProcessedBamFileQaFileService processedBamFileQaFileService = createProcessedBamFileQaFileService()
        processedBamFileQaFileService.dataProcessingFilesService = [
            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
            }
        ] as DataProcessingFilesService
        AlignmentPass.metaClass.isLatestPass = {false}

        assert processedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(qualityAssessmentPass)
    }

    @Test
    public void testDeleteProcessingFiles() {
        final int FILE_LENGTH = 10
        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.build()
        ProcessedBamFileQaFileService processedBamFileQaFileService = createProcessedBamFileQaFileService()
        processedBamFileQaFileService.dataProcessingFilesService = [
            deleteProcessingFilesAndDirectory: { final Project project, final File processingDirectory, final Collection<String> fileNames ->
                File filePath = processedBamFileQaFileService.directoryPath(qualityAssessmentPass) as File
                Set<String> expectedAdditionFiles = processedBamFileQaFileService.allFileNames(qualityAssessmentPass.processedBamFile) as Set
                assert qualityAssessmentPass.project == project
                assert filePath == processingDirectory
                assert expectedAdditionFiles == fileNames as Set
                return FILE_LENGTH
            },
        ] as DataProcessingFilesService

        assert FILE_LENGTH == processedBamFileQaFileService.deleteProcessingFiles(qualityAssessmentPass)
    }

    @Test
    public void testDeleteProcessingFiles_QualityAssessmentPassIsNull() {
        ProcessedBamFileQaFileService processedBamFileQaFileService = createProcessedBamFileQaFileService()
        processedBamFileQaFileService.dataProcessingFilesService = [
            deleteProcessingFilesAndDirectory: { final Project project, final File processingDirectory, final Collection<String> fileNames ->
                fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
            },
        ] as DataProcessingFilesService

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedBamFileQaFileService.deleteProcessingFiles(null) //
        }
    }

    @Test
    public void testDeleteProcessingFiles_NotConsistent() {
        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.build()
        ProcessedBamFileQaFileService processedBamFileQaFileService = createProcessedBamFileQaFileService()
        processedBamFileQaFileService.dataProcessingFilesService = [
            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                return false
            },
            deleteProcessingFilesAndDirectory: { final Project project, final File processingDirectory, final Collection<String> fileNames ->
                fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
            },
        ] as DataProcessingFilesService

        assert 0 == processedBamFileQaFileService.deleteProcessingFiles(qualityAssessmentPass)
    }
}
