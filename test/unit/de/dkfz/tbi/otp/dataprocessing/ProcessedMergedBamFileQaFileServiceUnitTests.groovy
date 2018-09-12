package de.dkfz.tbi.otp.dataprocessing

import org.junit.Test

import grails.buildtestdata.mixin.Build
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

@Build([
    QualityAssessmentMergedPass
])
class ProcessedMergedBamFileQaFileServiceUnitTests {


    private final static long SOME_FILE_LENGTH = 10 //Content should only be positive



    private def createDataForDeleteChecking(Boolean valueForDestinationConsistence = null) {
        final File dataProcessingTempDir = TestCase.getUniqueNonExistentPath()
        final File tempPmbfProcessingDir = TestCase.getUniqueNonExistentPath()
        final File tempQaResultDir = TestCase.getUniqueNonExistentPath()

        QualityAssessmentMergedPass qualityAssessmentMergedPass = QualityAssessmentMergedPass.build()
        ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService = new ProcessedMergedBamFileQaFileService()
        processedMergedBamFileQaFileService.processedMergedBamFileService = [
            qaResultDestinationDirectory: { ProcessedMergedBamFile file -> return tempQaResultDir as String },
            processingDirectory: {final MergingPass mergingPass -> return tempPmbfProcessingDir as String },
        ] as ProcessedMergedBamFileService

        processedMergedBamFileQaFileService.dataProcessingFilesService = [
            getOutputDirectory: { Individual individual, DataProcessingFilesService.OutputDirectories dir ->
                return dataProcessingTempDir as String
            },

            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                File expectedProcessingDirectory = processedMergedBamFileQaFileService.qaPassProcessingDirectory(qualityAssessmentMergedPass) as File
                File expectedFinalDestinationDirectory = tempQaResultDir // fake it since we're not actually moving stuff: re-use QA as final destination in Project
                Collection<String> expectedAdditionalFiles = processedMergedBamFileQaFileService.allFileNames(qualityAssessmentMergedPass.abstractMergedBamFile)
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
                Collection<String> expectedAdditionalFiles = processedMergedBamFileQaFileService.allFileNames(qualityAssessmentMergedPass.abstractMergedBamFile)
                assert qualityAssessmentMergedPass.project == project
                assert expectedDirectory == processingDirectory
                assert expectedAdditionalFiles == fileNames
                return SOME_FILE_LENGTH
            },
        ] as DataProcessingFilesService

        return [
            qualityAssessmentMergedPass,
            processedMergedBamFileQaFileService,
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
        QualityAssessmentMergedPass.build(identifier: 1, abstractMergedBamFile: qualityAssessmentMergedPass.abstractMergedBamFile)

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

