package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import java.util.Map;

import org.junit.*

class ProjectOverviewServiceIntegrationTests {

    ProjectOverviewService projectOverviewService

    void test_sampleTypeByProject_noData() {
        Project project = Project.build()
        def list = projectOverviewService.sampleTypeByProject(project)
        assert [] == list
    }


    void test_sampleTypeByProject_oneData() {
        Project project = Project.build()
        SampleType sampleType = SampleType.build(name: "BLOOD")

        createAggregateSequences(project, sampleType)

        List<String> results = projectOverviewService.sampleTypeByProject(project)
        assert 1 == results.size()
        assert sampleType.name == results[0]
    }


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
        assert 3 == list.size()
        assert list.contains(sampleType1.name)
        assert list.contains(sampleType2.name)
        assert list.contains(sampleType3.name)
    }


    void createAggregateSequences(Project project, SampleType sampleType) {
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
