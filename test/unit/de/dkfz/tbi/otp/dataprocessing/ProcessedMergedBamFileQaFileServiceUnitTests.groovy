package de.dkfz.tbi.otp.dataprocessing

import org.junit.Test

import static org.springframework.util.Assert.*
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.ngsdata.*

@Build([
    QualityAssessmentMergedPass
])
class ProcessedMergedBamFileQaFileServiceUnitTests {


    private final static long SOME_FILE_LENGTH = 10 //Content should only be positive



    private def createDataForDeleteChecking(Boolean valueForDestinationConsistence = null) {
        QualityAssessmentMergedPass qualityAssessmentMergedPass = QualityAssessmentMergedPass.build()
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService = new ProcessedMergedBamFileQaFileService()
        processedMergedBamFileQaFileService.processedMergedBamFileService = [
            qaResultDestinationDirectory: { ProcessedMergedBamFile file -> return TestConstants.BASE_TEST_DIRECTORY },
            processingDirectory: {final MergingPass mergingPass -> return TestConstants.BASE_TEST_DIRECTORY },
        ] as ProcessedMergedBamFileService

        processedMergedBamFileQaFileService.dataProcessingFilesService = [
            getOutputDirectory: { Individual individual, DataProcessingFilesService.OutputDirectories dir ->
                return TestConstants.BASE_TEST_DIRECTORY
            },

            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                File expectedProcessingDirectory = processedMergedBamFileQaFileService.qaPassProcessingDirectory(qualityAssessmentMergedPass) as File
                File expectedFinalDestinationDirectory = new File(TestConstants.BASE_TEST_DIRECTORY)
                Collection<String> expectedAdditionalFiles = processedMergedBamFileQaFileService.allFileNames(qualityAssessmentMergedPass.processedMergedBamFile)
                assert expectedProcessingDirectory == processingDirectory
                assert expectedFinalDestinationDirectory == finalDestinationDirectory
                assert expectedAdditionalFiles == fileNames
                if (valueForDestinationConsistence == null) {
                    fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
                } else {
                    return valueForDestinationConsistence.value
                }
            },

            deleteProcessingFilesAndDirectory: { final Project project, final File processingDirectory, final Collection<String> fileNames ->
                File expectedDirectory = processedMergedBamFileQaFileService.qaPassProcessingDirectory(qualityAssessmentMergedPass) as File
                Collection<String> expectedAdditionalFiles = processedMergedBamFileQaFileService.allFileNames(qualityAssessmentMergedPass.processedMergedBamFile)
                assert qualityAssessmentMergedPass.project == project
                assert expectedDirectory == processingDirectory
                assert expectedAdditionalFiles == fileNames
                return SOME_FILE_LENGTH
            },
        ] as DataProcessingFilesService

        return [
            qualityAssessmentMergedPass,
            processedMergedBamFileQaFileService
        ]
    }



    @Test
    public void testCheckConsistencyForProcessingFilesDeletion() {
        QualityAssessmentMergedPass qualityAssessmentMergedPass
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService
        (qualityAssessmentMergedPass, processedMergedBamFileQaFileService) = createDataForDeleteChecking(true)

        assert processedMergedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(qualityAssessmentMergedPass)
    }

    @Test
    public void testCheckConsistencyForProcessingFilesDeletion_NoQualityAssessmentMergedPass() {
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService = new ProcessedMergedBamFileQaFileService()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedMergedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }

    @Test
    public void testCheckConsistencyForProcessingFilesDeletion_NotLatestQualityAssessmentMergedPass() {
        QualityAssessmentMergedPass qualityAssessmentMergedPass
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService
        (qualityAssessmentMergedPass, processedMergedBamFileQaFileService) = createDataForDeleteChecking()
        QualityAssessmentMergedPass.build(identifier: 1, processedMergedBamFile: qualityAssessmentMergedPass.processedMergedBamFile)

        assert processedMergedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(qualityAssessmentMergedPass)
    }

    @Test
    public void testCheckConsistencyForProcessingFilesDeletion_NotLatestMergingPass() {
        QualityAssessmentMergedPass qualityAssessmentMergedPass
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService
        (qualityAssessmentMergedPass, processedMergedBamFileQaFileService) = createDataForDeleteChecking()
        MergingPass.build(identifier: 1, mergingSet: qualityAssessmentMergedPass.mergingSet)

        assert processedMergedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(qualityAssessmentMergedPass)
    }

    @Test
    public void testCheckConsistencyForProcessingFilesDeletion_NotLatestMergingSet() {
        QualityAssessmentMergedPass qualityAssessmentMergedPass
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService
        (qualityAssessmentMergedPass, processedMergedBamFileQaFileService) = createDataForDeleteChecking()
        MergingSet.build(identifier: 1, mergingWorkPackage: qualityAssessmentMergedPass.mergingSet.mergingWorkPackage)

        assert processedMergedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(qualityAssessmentMergedPass)
    }

    @Test
    public void testDeleteProcessingFiles() {
        QualityAssessmentMergedPass qualityAssessmentMergedPass
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService
        (qualityAssessmentMergedPass, processedMergedBamFileQaFileService) = createDataForDeleteChecking(true)

        assert SOME_FILE_LENGTH == processedMergedBamFileQaFileService.deleteProcessingFiles(qualityAssessmentMergedPass)
    }

    @Test
    public void testDeleteProcessingFiles_NoQualityAssessmentMergedPass() {
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService = new ProcessedMergedBamFileQaFileService()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedMergedBamFileQaFileService.deleteProcessingFiles(null) //
        }
    }

    @Test
    public void testDeleteProcessingFiles_CheckConsistenceIsWrong() {
        QualityAssessmentMergedPass qualityAssessmentMergedPass
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService
        (qualityAssessmentMergedPass, processedMergedBamFileQaFileService) = createDataForDeleteChecking(false)

        assert 0 == processedMergedBamFileQaFileService.deleteProcessingFiles(qualityAssessmentMergedPass)
    }
}

