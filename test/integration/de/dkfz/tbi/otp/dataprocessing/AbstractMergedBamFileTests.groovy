package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import org.junit.Test

import static org.junit.Assert.assertEquals


/**
 */
class AbstractMergedBamFileTests {

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
    void testUpdateFileOperationStatus_ProcessedMergedBamFile() {
        ProcessedMergedBamFile mergedBamFile = DomainFactory.createProcessedMergedBamFile()
        mergedBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.DECLARED
        assert mergedBamFile.save(flush: true)
        assertEquals(mergedBamFile.fileOperationStatus, AbstractMergedBamFile.FileOperationStatus.DECLARED)
        mergedBamFile.updateFileOperationStatus(AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING)
        assert mergedBamFile.save(flush: true)
        assertEquals(mergedBamFile.fileOperationStatus, AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING)
    }

    @Test
    void testUpdateFileOperationStatus_RoddyBamFile() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                md5sum: null,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                fileSize: -1,
        ])
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.DECLARED
        assert roddyBamFile.save(flush: true)
        assertEquals(roddyBamFile.fileOperationStatus, AbstractMergedBamFile.FileOperationStatus.DECLARED)
        roddyBamFile.updateFileOperationStatus(AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING)
        assert roddyBamFile.save(flush: true)
        assertEquals(roddyBamFile.fileOperationStatus, AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING)
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenBamFileFileOperationStatusNotInProgress_ShouldFail() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
        ])

        TestCase.shouldFail(AssertionError) {
            bamFile.validateAndSetBamFileInProjectFolder()
        }
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenBamFileWithdrawn_ShouldFail() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                withdrawn: true,
        ])

        TestCase.shouldFail(AssertionError) {
            bamFile.validateAndSetBamFileInProjectFolder()
        }
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenOtherNonWithdrawnBamFilesWithFileOperationStatusInProgressOfSameMergingWorkPackageExist_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build()

        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ])

        DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        ])

        TestCase.shouldFail(AssertionError) {
            bamFile.validateAndSetBamFileInProjectFolder()
        }
    }

    @Test
    void testValidateAndSetBamFileInProjectFolder_WhenAllFine_ShouldSetBamFileInProjectFolder() {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build()

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

        bamFile.validateAndSetBamFileInProjectFolder()

        assert bamFile.mergingWorkPackage.bamFileInProjectFolder == bamFile
    }
}
