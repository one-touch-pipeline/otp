package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.junit.*
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
    void testCoveragePerPatientAndSampleTypeAndSeqType_OneWithdrawnMergedBamFile_ShouldReturnNothing() {
        ProcessedMergedBamFile processedMergedBamFile = createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        processedMergedBamFile.withdrawn = true
        List result = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile.project)
        assert [] == projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile.project)
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
        processedMergedBamFile2.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        processedMergedBamFile2.fileSize = 10000
        assert processedMergedBamFile2.save(flush: true)

        assert processedMergedBamFile1.mergingWorkPackage == processedMergedBamFile2.mergingWorkPackage

        List result1 = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile1.project)
        assert result1.size() == 1
        List result2 = projectOverviewService.coveragePerPatientAndSampleTypeAndSeqType(processedMergedBamFile2.project)
        assert result2.size() == 1
        assert result1 == result2
        ensureResultsAreCorrect(result2, processedMergedBamFile2)
    }

    @Test
    void test_sampleTypeByProject_noData() {
        Project project = Project.build()
        def list = projectOverviewService.sampleTypeByProject(project)
        assert [] == list
    }

    @Test
    void test_sampleTypeByProject_oneData() {
        Project project = Project.build()
        SampleType sampleType = SampleType.build(name: "BLOOD")

        createAggregateSequences(project, sampleType)

        List<String> results = projectOverviewService.sampleTypeByProject(project)
        assert sampleType.name == CollectionUtils.exactlyOneElement(results)
    }

    @Test
    void test_sampleTypeByProject_multipleData() {
        Project project = Project.build()
        SampleType sampleType1 = SampleType.build(name: "BLOOD")
        SampleType sampleType2 = SampleType.build(name: "TUMOR")
        SampleType sampleType3 = SampleType.build(name: "RELAPSE")
        SampleType sampleType_OfOtherProject = SampleType.build(name: "CONTROL")

        createAggregateSequences(project, sampleType1)
        createAggregateSequences(project, sampleType2)
        createAggregateSequences(project, sampleType3)
        createAggregateSequences(Project.build(), sampleType_OfOtherProject)

        def list = projectOverviewService.sampleTypeByProject(project)
        assert CollectionUtils.containSame(list, [sampleType1.name, sampleType2.name, sampleType3.name])
    }

    private void ensureResultsAreCorrect(List result, ProcessedMergedBamFile processedMergedBamFile) {
        assert result.first().mockPid == processedMergedBamFile.individual.mockPid
        assert result.first().sampleTypeName == processedMergedBamFile.sampleType.name
        assert result.first().seqType == processedMergedBamFile.seqType
        assert result.first().coverage == processedMergedBamFile.coverage
        assert result.first().numberOfMergedLanes == processedMergedBamFile.numberOfMergedLanes
    }

    private ProcessedMergedBamFile createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest() {
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        processedMergedBamFile.coverage = 30
        processedMergedBamFile.numberOfMergedLanes = 3
        processedMergedBamFile.md5sum = "2bb2f88846cd7f52e61f0af6987c0935"
        processedMergedBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        processedMergedBamFile.fileSize = 10000
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
                ),
                workPackage: pmbfFirst.mergingWorkPackage,
        )
    }

    private void createAggregateSequences(Project project, SampleType sampleType) {
        int identifier = AggregateSequences.findAllByProjectId(project.id).size()
        AggregateSequences.build(
                projectId: project.id,
                sampleTypeName: sampleType.name,
                seqTypeId: identifier,
                seqPlatformId: identifier,
                sampleId: sampleType.id,
                seqCenterId: identifier,
                sampleTypeId: identifier,
                individualId: identifier,
        )
    }
}
