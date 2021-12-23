/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
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
    void testWithdraw_SetBamFileAndSnvCallingInstanceWithdrawn() {
        SamplePair samplePair = DomainFactory.createSamplePairWithProcessedMergedBamFiles()

        assert !samplePair.mergingWorkPackage1.bamFileInProjectFolder.withdrawn
        assert !samplePair.mergingWorkPackage2.bamFileInProjectFolder.withdrawn

        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                samplePair: samplePair,
        )

        LogThreadLocal.withThreadLog(System.out) {
            samplePair.mergingWorkPackage1.bamFileInProjectFolder.withdraw()
        }
        assert samplePair.mergingWorkPackage1.bamFileInProjectFolder.withdrawn
        assert snvCallingInstance.withdrawn
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
        assert bamFile.mergingWorkPackage.save(flush: true)

        assert expected == bamFile.pathForFurtherProcessing
    }

    @Test
    void testGetPathForFurtherProcessing_IsNotSetInMergingWorkPackage__shouldThrowException() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()

        TestCase.shouldFailWithMessage(IllegalStateException, /^This BAM file is not in the project folder(?s).*$/) {
            bamFile.pathForFurtherProcessing
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
                AbstractBamFileServiceIntegrationTests.ARBITRARY_QA_VALUES + [
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
