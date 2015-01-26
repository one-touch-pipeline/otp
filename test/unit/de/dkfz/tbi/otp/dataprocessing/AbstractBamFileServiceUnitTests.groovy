package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor

@Build([
    ProcessedBamFile,
    MergingSetAssignment,
    ProcessedMergedBamFile,
])
@TestFor(AbstractBamFileService)
class AbstractBamFileServiceUnitTests {

    AbstractBamFileService abstractBamFileService

    Date createdBefore = new Date().plus(1)



    void setUp() {
        abstractBamFileService = new AbstractBamFileService()
    }



    private ProcessedBamFile createTestDataForHasBeenQualityAssessedAndMerged(Map processedBamFileMap = [:], Map mergingSetMap = [:], Map mergingSetAssignmentMap = [:], Map processedMergedBamFileMap = [:]) {
        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
        ] + processedBamFileMap)
        MergingSet mergingSet = MergingSet.build([
            status: MergingSet.State.PROCESSED
        ] + mergingSetMap)
        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build([
            bamFile: processedBamFile,
            mergingSet: mergingSet,
        ] + mergingSetAssignmentMap)
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            mergingPass: MergingPass.build([
                mergingSet: mergingSet
            ])
        ] + processedMergedBamFileMap)
        return processedBamFile
    }


    void testHasBeenQualityAssessedAndMerged() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        assert abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_QAIsNotFinished() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_StatusIsNotProcessed() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            status: AbstractBamFile.State.INPROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_BamFileIsNotInMergingSet() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [
            bamFile: ProcessedBamFile.build()
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_MergingSetIsNorProcessed() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [
            status: MergingSet.State.INPROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileHasQANotFinished() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileDateIsLater() {
        createdBefore = new Date().minus(1)
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileIsWithdrawn() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            withdrawn: true
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileAndBamFileAreWithdrawn() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            withdrawn: true], [:], [:], [withdrawn: true])

        assert abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileOtherMergingSet() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            mergingPass: MergingPass.build()
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }
}
