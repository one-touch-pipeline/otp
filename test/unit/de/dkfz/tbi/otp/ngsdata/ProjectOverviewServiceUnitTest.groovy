package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.*

import org.junit.*


@TestFor(ProjectOverviewService)
@Build([
    ReferenceGenomeProjectSeqType,
    SampleType,
])
class ProjectOverviewServiceUnitTest {


    ProjectOverviewService projectOverviewService


    @Before
    void setup() {
        projectOverviewService = new ProjectOverviewService()
    }



    void test_listReferenceGenome_oneData() {
        Project project = Project.build()
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.build(
                project: project,
                )

        def list = projectOverviewService.listReferenceGenome(project)
        assert 1 == list.size()
        assert referenceGenomeProjectSeqType == list[0]
    }

    void test_listReferenceGenome_noData_wrongProject() {
        Project project = Project.build()
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.build()

        def list = projectOverviewService.listReferenceGenome(project)
        assert 0 == list.size()
    }

    void test_listReferenceGenome_noData_dateDeprecated() {
        Project project = Project.build()
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.build(
                project: project,
                deprecatedDate: new Date()
                )

        def list = projectOverviewService.listReferenceGenome(project)
        assert 0 == list.size()
    }

    void test_listReferenceGenome_multipleData() {
        Project project = Project.build()
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType1 = ReferenceGenomeProjectSeqType.build(
                project: project,
                )
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType2 = ReferenceGenomeProjectSeqType.build(
                project: project,
                sampleType: SampleType.build()
                )
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType3 = ReferenceGenomeProjectSeqType.build(
                project: project,
                sampleType: SampleType.build()
                )

        def list = projectOverviewService.listReferenceGenome(project)
        assert 3 == list.size()
        assert list.contains(referenceGenomeProjectSeqType1)
        assert list.contains(referenceGenomeProjectSeqType2)
        assert list.contains(referenceGenomeProjectSeqType3)
    }


}




