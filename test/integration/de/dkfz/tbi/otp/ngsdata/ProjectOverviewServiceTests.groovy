package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired


class ProjectOverviewServiceTests {

    @Autowired
    ProjectOverviewService projectOverviewService


    @Test
    void testAbstractMergedBamFilesInProjectFolder_NoMergedBamFile_ShouldReturnNothing() {
        Project project = Project.build()
        assert [] == projectOverviewService.abstractMergedBamFilesInProjectFolder(project)
    }

    @Test
    void testAbstractMergedBamFilesInProjectFolder_MergedBamFile_ShouldReturnInfoForOneBam() {
        ProcessedMergedBamFile processedMergedBamFile = createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        List result = projectOverviewService.abstractMergedBamFilesInProjectFolder(processedMergedBamFile.project)
        assert result.size() == 1
        ensureResultsAreCorrect(result, processedMergedBamFile)
    }

    @Test
    void testAbstractMergedBamFilesInProjectFolder_DifferentProject_ShouldReturnNothing() {
        Project project = Project.build()
        createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        assert [] == projectOverviewService.abstractMergedBamFilesInProjectFolder(project)
    }

    @Test
    void testAbstractMergedBamFilesInProjectFolder_NoBamFileInProjectFolder_ShouldReturnNothing() {
        ProcessedMergedBamFile processedMergedBamFile1 = createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        MergingWorkPackage workPackage = processedMergedBamFile1.workPackage
        workPackage.bamFileInProjectFolder = null
        assert workPackage.save(flush: true)
        assert [] == projectOverviewService.abstractMergedBamFilesInProjectFolder(processedMergedBamFile1.project)
    }

    @Test
    void testAbstractMergedBamFilesInProjectFolder_TwoMergedBamFiles_OneInProgress_ShouldReturnInfoForFirstBam() {
        ProcessedMergedBamFile processedMergedBamFile1 = createFinishedBamFileForCoveragePerPatientAndSampleTypeAndSeqTypeTest()
        ProcessedMergedBamFile processedMergedBamFile2 = createSecondUnfinishedBamFile(processedMergedBamFile1)
        assert processedMergedBamFile1.mergingWorkPackage == processedMergedBamFile2.mergingWorkPackage
        List result = projectOverviewService.abstractMergedBamFilesInProjectFolder(processedMergedBamFile1.project)
        assert result.size() == 1
        ensureResultsAreCorrect(result, processedMergedBamFile1)
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
        assert result.first().individual.mockPid == processedMergedBamFile.individual.mockPid
        assert result.first().sampleType.name == processedMergedBamFile.sampleType.name
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
        MergingWorkPackage mergingWorkPackage = processedMergedBamFile.mergingWorkPackage
        mergingWorkPackage.bamFileInProjectFolder = processedMergedBamFile
        assert mergingWorkPackage.save(flush: true)
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
