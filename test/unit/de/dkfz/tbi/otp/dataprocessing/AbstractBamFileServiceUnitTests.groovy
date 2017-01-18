package de.dkfz.tbi.otp.dataprocessing

import org.junit.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.FileType
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor

@Build([
    DataFile,
    FileType,
    ProcessedBamFile,
    MergingSetAssignment,
    MergingWorkPackage,
    ProcessedMergedBamFile,
])
@TestFor(AbstractBamFileService)
class AbstractBamFileServiceUnitTests {

    AbstractBamFileService abstractBamFileService

    Date createdBefore = new Date().plus(1)



    @Before
    void setUp() {
        abstractBamFileService = new AbstractBamFileService()
    }



    private ProcessedBamFile createTestDataForHasBeenQualityAssessedAndMerged(Map processedBamFileMap = [:], Map mergingSetMap = [:], Map mergingSetAssignmentMap = [:], Map processedMergedBamFileMap = [:]) {
        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
        ] + processedBamFileMap)
        MergingSet mergingSet = MergingSet.build([
            mergingWorkPackage: processedBamFile.mergingWorkPackage,
            status: MergingSet.State.PROCESSED
        ] + mergingSetMap)
        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build([
            bamFile: processedBamFile,
            mergingSet: mergingSet,
        ] + mergingSetAssignmentMap)
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            mergingPass: DomainFactory.createMergingPass([
                mergingSet: mergingSet
            ]),
            workPackage: mergingSet.mergingWorkPackage,
        ] + processedMergedBamFileMap)
        return processedBamFile
    }


    @Test
    void testHasBeenQualityAssessedAndMerged() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        assert abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_QAIsNotFinished() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_StatusIsNotProcessed() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            status: AbstractBamFile.State.INPROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_BamFileIsNotInMergingSet() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.bamFile = DomainFactory.createProcessedBamFile(msa.mergingSet.mergingWorkPackage)
        assert msa.save(failOnError: true)

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_MergingSetIsNorProcessed() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [
            status: MergingSet.State.INPROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileHasQANotFinished() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileDateIsLater() {
        createdBefore = new Date().minus(1)
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileIsWithdrawn() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            withdrawn: true
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileAndBamFileAreWithdrawn() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            withdrawn: true], [:], [:], [withdrawn: true])

        assert abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileOtherMergingSet() {
        MergingPass mergingPass = MergingPass.build()
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            mergingPass: mergingPass,
            workPackage: mergingPass.mergingWorkPackage,
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }
}
