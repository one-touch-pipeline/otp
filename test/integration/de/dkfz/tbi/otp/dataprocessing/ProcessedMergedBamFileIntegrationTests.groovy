package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import org.junit.*

import static de.dkfz.tbi.otp.dataprocessing.AbstractBamFileServiceTests.*

class ProcessedMergedBamFileIntegrationTests {

    @Test
    void test_getOverallQualityAssessment_WhenOnePassExists_ShouldReturnThis() {

        final Long ARBITRARY_IDENTIFIER = 42

        def processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        def oqa = createOverallQualityAssessment(processedMergedBamFile, ARBITRARY_IDENTIFIER)

        assert processedMergedBamFile.overallQualityAssessment == oqa
    }

    @Test
    void test_getOverallQualityAssessment_WhenMultiplePassesExists_ShouldReturnLatest() {

        final Long IDENTIFIER_FORMER = 100
        final Long IDENTIFIER_LATER = 200

        assert IDENTIFIER_FORMER < IDENTIFIER_LATER

        def processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        def oqaFormer = createOverallQualityAssessment(processedMergedBamFile, IDENTIFIER_FORMER)
        def oqaLater = createOverallQualityAssessment(processedMergedBamFile, IDENTIFIER_LATER)

        assert processedMergedBamFile.overallQualityAssessment == oqaLater
        assert processedMergedBamFile.overallQualityAssessment != oqaFormer
    }



    @Test
    void testWithdraw_SetOneBamFileWithdrawn() {
        ProcessedMergedBamFile bamFile = createFinishedProcessedBamFile()

        LogThreadLocal.withThreadLog(System.out) {
            bamFile.withdraw()
        }
        assert bamFile.withdrawn
    }

    @Test
    void testWithdraw_SetTwoBamFilesWithdrawn() {
        ProcessedMergedBamFile bamFile = createFinishedProcessedBamFile()

        ProcessedMergedBamFile secondBamFile = DomainFactory.createIncrementalMergedBamFile(bamFile)
        assert !secondBamFile.withdrawn

        secondBamFile.workPackage.bamFileInProjectFolder = secondBamFile
        assert secondBamFile.workPackage.save(flush: true)

        LogThreadLocal.withThreadLog(System.out) {
            bamFile.withdraw()
        }
        assert bamFile.withdrawn
        assert secondBamFile.withdrawn
    }

    @Test
    void testWithdraw_SetSecondAndThirdBamFileWithdrawn() {
        ProcessedMergedBamFile bamFile = createFinishedProcessedBamFile()

        ProcessedMergedBamFile secondBamFile = DomainFactory.createIncrementalMergedBamFile(bamFile)
        assert !secondBamFile.withdrawn

        ProcessedMergedBamFile thirdBamFile = DomainFactory.createIncrementalMergedBamFile(secondBamFile)
        assert !thirdBamFile.withdrawn

        thirdBamFile.workPackage.bamFileInProjectFolder = thirdBamFile
        assert thirdBamFile.workPackage.save(flush: true)

        LogThreadLocal.withThreadLog(System.out) {
            secondBamFile.withdraw()
        }
        assert !bamFile.withdrawn
        assert secondBamFile.withdrawn
        assert thirdBamFile.withdrawn
    }

    @Test
    void testWithdraw_SetBamFileAndSnvResultsWithdrawn() {
        SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()

        assert !testData.bamFileTumor.withdrawn
        assert !testData.bamFileControl.withdrawn

        testData.bamFileTumor.workPackage.bamFileInProjectFolder = testData.bamFileTumor
        assert testData.bamFileTumor.workPackage.save(flush: true)

        testData.bamFileControl.workPackage.bamFileInProjectFolder = testData.bamFileControl
        assert testData.bamFileControl.workPackage.save(flush: true)

        SnvCallingInstance snvCallingInstance = testData.createAndSaveSnvCallingInstance()

        SnvJobResult callingResult = testData.createAndSaveSnvJobResult(snvCallingInstance, SnvCallingStep.CALLING)
        assert !callingResult.withdrawn

        LogThreadLocal.withThreadLog(System.out) {
            testData.bamFileTumor.withdraw()
        }
        assert testData.bamFileTumor.withdrawn
        assert snvCallingInstance.withdrawn
        assert callingResult.withdrawn
    }



    @Test
    void testGetPathForFurtherProcessing_IsSetInMergingWorkPackage_shouldReturnDir() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: DomainFactory.DEFAULT_FILE_SIZE,
        ])
        File expected = new File(bamFile.baseDirectory, bamFile.bamFileName)
        bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
        assert bamFile.mergingWorkPackage.save(flush: true, failOnError: true)

        assert expected == bamFile.getPathForFurtherProcessing()
    }

    @Test
    void testGetPathForFurtherProcessing_IsNotSetInMergingWorkPackage__shouldThrowException() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()

        TestCase.shouldFailWithMessage(IllegalStateException, /^This BAM file is not in the project folder(?s).*$/) {
            bamFile.getPathForFurtherProcessing()
        }
    }


    private static OverallQualityAssessmentMerged createOverallQualityAssessment(ProcessedMergedBamFile processedMergedBamFile, Long identifier) {

        assert processedMergedBamFile: 'processedMergedBamFile must not be null'

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                abstractMergedBamFile: processedMergedBamFile,
                identifier: QualityAssessmentMergedPass.nextIdentifier(processedMergedBamFile),
        ])
        assert qualityAssessmentMergedPass.save([flush: true])

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged(
                ARBITRARY_QA_VALUES + [
                id                   : identifier,
                qualityAssessmentMergedPass: qualityAssessmentMergedPass,
        ])
        assert overallQualityAssessmentMerged.save([flush: true])

        return overallQualityAssessmentMerged
    }

    private ProcessedMergedBamFile createFinishedProcessedBamFile() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: 1000,
        ])
        bamFile.save(flush: true)
        assert !bamFile.withdrawn

        bamFile.workPackage.bamFileInProjectFolder = bamFile
        assert bamFile.workPackage.save(flush: true)

        return bamFile
    }
}
