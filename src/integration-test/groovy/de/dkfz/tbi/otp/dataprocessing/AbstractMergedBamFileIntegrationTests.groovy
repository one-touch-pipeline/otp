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
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class AbstractMergedBamFileIntegrationTests {

    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    @Test
    void testConstraints_allFine_succeeds() {
        DomainFactory.createProcessedMergedBamFile()
    }

    @Test
    void testConstraints_numberOfMergedLanesIsZero_validationShouldFail() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()
        bamFile.numberOfMergedLanes = 0
        TestCase.assertValidateError(bamFile, "numberOfMergedLanes", "validator.invalid", 0)
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenBamFileFileOperationStatusNotInProgress_ShouldFail() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ])

        TestCase.shouldFail(AssertionError) {
            linkFilesToFinalDestinationService.validateAndSetBamFileInProjectFolder(bamFile)
        }
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenBamFileWithdrawn_ShouldFail() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                withdrawn: true,
        ])

        TestCase.shouldFail(AssertionError) {
            linkFilesToFinalDestinationService.validateAndSetBamFileInProjectFolder(bamFile)
        }
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenOtherNonWithdrawnBamFilesWithFileOperationStatusInProgressOfSameMergingWorkPackageExist_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createDefaultOtpPipeline())

        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ])

        DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ])

        TestCase.shouldFail(AssertionError) {
            linkFilesToFinalDestinationService.validateAndSetBamFileInProjectFolder(bamFile)
        }
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenAllFine_ShouldSetBamFileInProjectFolder() {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createDefaultOtpPipeline())

        DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ])

        DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [
                withdrawn: true,
        ])

        DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ])

        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ])

        linkFilesToFinalDestinationService.validateAndSetBamFileInProjectFolder(bamFile)

        assert bamFile.mergingWorkPackage.bamFileInProjectFolder == bamFile
    }
}
