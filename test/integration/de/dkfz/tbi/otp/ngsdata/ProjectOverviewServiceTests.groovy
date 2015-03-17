package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*

import static org.junit.Assert.*
import org.junit.*
import grails.buildtestdata.mixin.Build
import org.springframework.beans.factory.annotation.Autowired


class ProjectOverviewServiceTests {

    @Autowired
    ProjectOverviewService projectOverviewService


    @Test
    void testCoveragePerPatientAndSampleTypeAndSeqType_NoMergedBamFile_ShouldReturnNothing() {
        Project project = Project.build()
        assert [] == projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(project)
    }

    @Test
    void testCoveragePerPatientAndSampleTypeAndSeqType_OneMergedBamFile_ShouldReturnInfoForOneBam() {
        ProcessedMergedBamFile processedMergedBamFile = createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        List result = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile.project)
        assert result.size() == 1
        ensureResultsAreCorrect(result, processedMergedBamFile)
    }

    @Test
    void testCoveragePerPatientAndSampleTypeAndSeqType_TwoMergedBamFiles_OneInProgress_ShouldReturnInfoForFirstBam() {
        ProcessedMergedBamFile processedMergedBamFile1 = createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        ProcessedMergedBamFile processedMergedBamFile2 = createSecondUnfinishedBamFile(processedMergedBamFile1)
        assert processedMergedBamFile1.mergingWorkPackage == processedMergedBamFile2.mergingWorkPackage
        List result = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile1.project)
        assert result.size() == 1
        ensureResultsAreCorrect(result, processedMergedBamFile1)
    }

    @Test
    void testCoveragePerPatientAndSampleTypeAndSeqType_TwoFinishedMergedBamFiles_ShouldReturnInfoForSecondBam() {
        ProcessedMergedBamFile processedMergedBamFile1 = createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        ProcessedMergedBamFile processedMergedBamFile2 = createSecondUnfinishedBamFile(processedMergedBamFile1)
        processedMergedBamFile2.coverage = 40
        processedMergedBamFile2.numberOfMergedLanes = 4
        processedMergedBamFile2.md5sum = "2bb2f88846cd7f52e61f0af6987c0935"
        processedMergedBamFile2.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED
        assert processedMergedBamFile2.save(flush: true)

        assert processedMergedBamFile1.mergingWorkPackage == processedMergedBamFile2.mergingWorkPackage

        List result1 = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile1.project)
        assert result1.size() == 1
        List result2 = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile2.project)
        assert result2.size() == 1
        assert result1 == result2
        ensureResultsAreCorrect(result2, processedMergedBamFile2)
    }


    private void ensureResultsAreCorrect(List result, ProcessedMergedBamFile processedMergedBamFile) {
        assert result.first().mockPid == processedMergedBamFile.individual.mockPid
        assert result.first().sampleTypeName == processedMergedBamFile.sampleType.name
        assert result.first().seqType == processedMergedBamFile.seqType
        assert result.first().coverage == processedMergedBamFile.coverage
        assert result.first().numberOfMergedLanes == processedMergedBamFile.numberOfMergedLanes
    }

    private ProcessedMergedBamFile createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest() {
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build()
        processedMergedBamFile.coverage = 30
        processedMergedBamFile.numberOfMergedLanes = 3
        processedMergedBamFile.md5sum = "2bb2f88846cd7f52e61f0af6987c0935"
        processedMergedBamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED
        assert processedMergedBamFile.save(flush: true)
        return processedMergedBamFile
    }

    private ProcessedMergedBamFile createSecondUnfinishedBamFile(ProcessedMergedBamFile pmbfFirst) {
        return ProcessedMergedBamFile.build(
                mergingPass: MergingPass.build(
                        mergingSet: MergingSet.build(
                                mergingWorkPackage: pmbfFirst.mergingWorkPackage,
                                identifier: 2
                        )
                )
        )
    }
}
