package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.testing.*
import org.springframework.beans.factory.annotation.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.IntegrationSpec
import spock.lang.*

class ProjectOverviewServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    @Autowired
    ProjectOverviewService projectOverviewService

    void "test abstractMergedBamFilesInProjectFolder without MergedBamFiles in Project"() {
        given:
        Project project = DomainFactory.createProject()

        expect:
        projectOverviewService.abstractMergedBamFilesInProjectFolder(project) == []
    }

    void "test abstractMergedBamFilesInProjectFolder with abstractMergedBamFile in Project"() {
        given:
        AbstractMergedBamFile mergedBamFile = abstractMergedBamFile()

        when:
        List result = projectOverviewService.abstractMergedBamFilesInProjectFolder(mergedBamFile.project)

        then:
        mergedBamFile == CollectionUtils.exactlyOneElement(result)

        where:
        abstractMergedBamFile                                                | _
        ({ DomainFactory.createFinishedProcessedMergedBamFile() })           | _
        ({ DomainFactory.createFinishedExternallyProcessedMergedBamFile() }) | _
    }

    void "test abstractMergedBamFilesInProjectFolder with abstractMergedBamFile in different Project"() {
        given:
        Project project = DomainFactory.createProject()
        abstractMergedBamFile()

        when:
        List result = projectOverviewService.abstractMergedBamFilesInProjectFolder(project)

        then:
        result.isEmpty()

        where:
        abstractMergedBamFile                                                | _
        ({ DomainFactory.createFinishedProcessedMergedBamFile() })           | _
        ({ DomainFactory.createFinishedExternallyProcessedMergedBamFile() }) | _
    }

    void "test abstractMergedBamFilesInProjectFolder with MergedBamFiles in Project but without MergingWorkPackage"() {
        given:
        AbstractMergedBamFile mergedBamFile = abstractMergedBamFile()
        AbstractMergingWorkPackage workPackage = mergedBamFile.workPackage
        workPackage.bamFileInProjectFolder = null
        assert workPackage.save(flush: true)

        when:
        List result = projectOverviewService.abstractMergedBamFilesInProjectFolder(mergedBamFile.project)

        then:
        result.isEmpty()

        where:
        abstractMergedBamFile                                                | _
        ({ DomainFactory.createFinishedProcessedMergedBamFile() })           | _
        ({ DomainFactory.createFinishedExternallyProcessedMergedBamFile() }) | _
    }

    void "test abstractMergedBamFilesInProjectFolder with two MergedBamFiles in Project but one not Finished yet"() {
        given:
        AbstractMergedBamFile mergedBamFile1 = finishedAbstractMergedBamFile()
        unfisihedAbstractMergedBamFile(mergedBamFile1.mergingWorkPackage)

        when:
        List result = projectOverviewService.abstractMergedBamFilesInProjectFolder(mergedBamFile1.project)

        then:
        mergedBamFile1 == CollectionUtils.exactlyOneElement(result)

        where:
        finishedAbstractMergedBamFile                                        | unfisihedAbstractMergedBamFile
        ({ DomainFactory.createFinishedProcessedMergedBamFile() })           | ({
            DomainFactory.createProcessedMergedBamFile(workPackage: it)
        })
        ({ DomainFactory.createFinishedExternallyProcessedMergedBamFile() }) | ({
            DomainFactory.createExternallyProcessedMergedBamFile(workPackage: it)
        })
    }

    void "test sampleTypeByProject without SampleTypes in Project"() {
        given:
        Project project = DomainFactory.createProject()

        expect:
        projectOverviewService.sampleTypeByProject(project) == []
    }

    void "test sampleTypeByProject with one SampleType in Project"() {
        given:
        Project project = DomainFactory.createProject()
        SampleType sampleType = DomainFactory.createSampleType()
        createAggregateSequences(project, sampleType)

        when:
        List<String> results = projectOverviewService.sampleTypeByProject(project)

        then:
        sampleType.name == CollectionUtils.exactlyOneElement(results)
    }

    void "test sampleTypeByProject with multiple SampleType-Project combinations"() {
        given:
        Project project = DomainFactory.createProject()
        SampleType sampleType1 = DomainFactory.createSampleType()
        SampleType sampleType2 = DomainFactory.createSampleType()
        SampleType sampleType3 = DomainFactory.createSampleType()
        SampleType sampleType4 = DomainFactory.createSampleType()
        createAggregateSequences(project, sampleType1)
        createAggregateSequences(project, sampleType2)
        createAggregateSequences(project, sampleType3)
        createAggregateSequences(DomainFactory.createProject(), sampleType4)

        when:
        List<String> results = projectOverviewService.sampleTypeByProject(project)

        then:
        CollectionUtils.containSame(results, [sampleType1.name, sampleType2.name, sampleType3.name])
    }

    void "test getAccessPersons with some Data"() {
        given:
        Project project = DomainFactory.createProject()
        createUserAndRoles()
        DomainFactory.createAclObjects(project)

        expect:
        projectOverviewService.getAccessPersons(project).contains(ADMIN)
        !projectOverviewService.getAccessPersons(project).contains(TESTUSER)
        !projectOverviewService.getAccessPersons(project).contains("username")
    }

    private void createAggregateSequences(Project project, SampleType sampleType) {
        DomainFactory.createAggregateSequences([projectId: project.id, sampleTypeName: sampleType.name, sampleTypeId: sampleType.id])
    }
}
