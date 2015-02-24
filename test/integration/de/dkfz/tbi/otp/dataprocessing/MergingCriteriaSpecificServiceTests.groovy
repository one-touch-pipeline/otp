package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Test

import static org.junit.Assert.*

class MergingCriteriaSpecificServiceTests {

    MergingCriteriaSpecificService mergingCriteriaSpecificService

    @Test
    void testProcessedBamFilesForMerging_TwoBamFiles() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()
        ProcessedBamFile bamFile1 = DomainFactory.createProcessedBamFile(workPackage, [status: State.INPROGRESS])
        ProcessedBamFile bamFile2 = DomainFactory.createProcessedBamFile(workPackage, [status: State.NEEDS_PROCESSING])

        assert TestCase.containSame([bamFile1, bamFile2],
                mergingCriteriaSpecificService.processedBamFilesForMerging(workPackage))
    }

    @Test
    void testProcessedBamFilesForMerging_TwoBamFilesWrongBamType() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()
        ProcessedBamFile bamFile1 = DomainFactory.createProcessedBamFile(workPackage, [status: State.INPROGRESS, type: BamType.RMDUP])
        ProcessedBamFile bamFile2 = DomainFactory.createProcessedBamFile(workPackage, [status: State.NEEDS_PROCESSING])

        assert TestCase.containSame([bamFile2],
                mergingCriteriaSpecificService.processedBamFilesForMerging(workPackage))
    }

    @Test
    void testProcessedBamFilesForMerging_TwoBamFilesWithdrawn() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()
        ProcessedBamFile bamFile1 = DomainFactory.createProcessedBamFile(workPackage, [status: State.INPROGRESS, withdrawn: true])
        ProcessedBamFile bamFile2 = DomainFactory.createProcessedBamFile(workPackage, [status: State.NEEDS_PROCESSING])

        assert TestCase.containSame([bamFile2],
                mergingCriteriaSpecificService.processedBamFilesForMerging(workPackage))
    }

    @Test
    void testProcessedBamFilesForMerging_TwoBamFilesWrongStatus() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()
        ProcessedBamFile bamFile1 = DomainFactory.createProcessedBamFile(workPackage, [status: State.DECLARED])
        ProcessedBamFile bamFile2 = DomainFactory.createProcessedBamFile(workPackage, [status: State.NEEDS_PROCESSING])

        assert TestCase.containSame([bamFile2],
                mergingCriteriaSpecificService.processedBamFilesForMerging(workPackage))
    }

    @Test
    void testProcessedBamFilesForMerging_ThreeBamsOneIsOutdated() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()
        ProcessedBamFile bamFile1 = DomainFactory.createProcessedBamFile(workPackage, [status: State.INPROGRESS])
        ProcessedBamFile bamFile2 = DomainFactory.createProcessedBamFile(workPackage, [status: State.NEEDS_PROCESSING])
        ProcessedBamFile bamFile3 = TestData.createProcessedBamFile(
                alignmentPass: TestData.createAndSaveAlignmentPass(seqTrack: bamFile2.seqTrack),
                status: State.NEEDS_PROCESSING,
        ).save(failOnError: true)

        assert TestCase.containSame([bamFile1, bamFile3],
                mergingCriteriaSpecificService.processedBamFilesForMerging(workPackage))
    }

    @Test
    void testProcessedBamFilesForMerging_TwoBamFilesWorkPackageNotEqual() {
        ProcessedBamFile bamFile1 = ProcessedBamFile.build(status: State.NEEDS_PROCESSING)
        ProcessedBamFile bamFile2 = ProcessedBamFile.build(status: State.NEEDS_PROCESSING)

        assert TestCase.containSame([bamFile1],
                mergingCriteriaSpecificService.processedBamFilesForMerging(bamFile1.mergingWorkPackage))
    }

    @Test
    void testProcessedMergedBamFileForMerging_NoMergedBamFile() {
        ProcessedBamFile bamFile = ProcessedBamFile.build(status: State.NEEDS_PROCESSING)

        assert null == mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile.mergingWorkPackage)
    }

    @Test
    void testProcessedMergedBamFileForMerging_OnlyOneMergedBamFile() {
        ProcessedMergedBamFile bamFile = buildProcessedMergedBamFile()

        assertEquals(bamFile, mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile.mergingWorkPackage))
    }

    @Test
    void testProcessedMergedBamFileForMerging_WrongBamType() {
        ProcessedMergedBamFile bamFile = buildProcessedMergedBamFile([type: BamType.RMDUP])

        assert null == mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile.mergingWorkPackage)
    }

    @Test
    void testProcessedMergedBamFileForMerging_Withdrawn() {
        ProcessedMergedBamFile bamFile = buildProcessedMergedBamFile([withdrawn: true])

        assert null == mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile.mergingWorkPackage)
    }

    @Test
    void testProcessedMergedBamFileForMerging_OnlyOneMergedBamFileNotFinished() {
        ProcessedMergedBamFile bamFile = buildProcessedMergedBamFile([:], MergingSet.State.INPROGRESS)

        assertNull(mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile.mergingWorkPackage))
    }

    @Test
    void testProcessedMergedBamFileForMerging_OnlyOneMergedBamFileWrongWorkPackage() {
        ProcessedMergedBamFile bamFile = buildProcessedMergedBamFile()

        assertNull(mergingCriteriaSpecificService.processedMergedBamFileForMerging(MergingWorkPackage.build()))
    }

    @Test
    void testProcessedMergedBamFileForMerging_LatestMergingSet() {
        ProcessedMergedBamFile bamFile1 = buildProcessedMergedBamFile()
        ProcessedMergedBamFile bamFile2 = DomainFactory.createProcessedMergedBamFile(
                bamFile1.mergingWorkPackage, [type: BamType.MDUP])
        bamFile2.mergingSet.status = MergingSet.State.PROCESSED
        assert bamFile2.mergingSet.save(failOnError: true)

        assert bamFile2 == mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile1.mergingWorkPackage)
    }

    @Test
    void testProcessedMergedBamFileForMerging_LatestMergingPass() {
        ProcessedMergedBamFile bamFile1 = buildProcessedMergedBamFile()
        ProcessedMergedBamFile bamFile2 = DomainFactory.createProcessedMergedBamFile(
                bamFile1.mergingSet, [type: BamType.MDUP])

        assert bamFile2 == mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile1.mergingWorkPackage)
    }

    @Test
    void testProcessedMergedBamFileForMerging_LatestMergingSetAndPass() {
        ProcessedMergedBamFile bamFile1 = buildProcessedMergedBamFile()
        ProcessedMergedBamFile bamFile2 = DomainFactory.createProcessedMergedBamFile(
                bamFile1.mergingSet, [type: BamType.MDUP])
        ProcessedMergedBamFile bamFile3 = DomainFactory.createProcessedMergedBamFile(
                bamFile1.mergingWorkPackage, [type: BamType.MDUP])
        [bamFile2, bamFile3].each {
            it.mergingSet.status = MergingSet.State.PROCESSED
            assert it.mergingSet.save(failOnError: true)
        }

        assert bamFile3 == mergingCriteriaSpecificService.processedMergedBamFileForMerging(bamFile1.mergingWorkPackage)
    }

    private ProcessedMergedBamFile buildProcessedMergedBamFile(Map properties = [:], MergingSet.State mergingSetStatus = MergingSet.State.PROCESSED) {
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.build([type: BamType.MDUP] + properties)
        bamFile.mergingSet.status = mergingSetStatus
        assert bamFile.mergingSet.save(failOnError: true)
        return bamFile
    }
}
