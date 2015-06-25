package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.buildtestdata.mixin.Build
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.Individual.Type


@TestMixin(GrailsUnitTestMixin)
@TestFor(Individual)
@Build([
        Realm,
        Sample,
        SeqType,
])
class IndividualUnitTests {

    @Test
    void testSaveIndividual_AllCorrect() {
        Individual individual = createIndividual()
        assert individual.validate()
    }

    @Test
    void testSaveIndividual_NoPid() {
        Individual individual = createIndividual()
        individual.pid = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoMockPid() {
        Individual individual = createIndividual()
        individual.mockPid = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoMockFullName() {
        Individual individual = createIndividual()
        individual.mockFullName = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoInternIdentifier() {
        Individual individual = createIndividual()
        individual.internIdentifier = null
        assert individual.validate()
    }

    @Test
    void testSaveIndividual_NoType() {
        Individual individual = createIndividual()
        individual.type = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_NoProject() {
        Individual individual = createIndividual()
        individual.project = null
        assert !individual.validate()
    }

    @Test
    void testSaveIndividual_PidNotUnique() {
        Individual individual1 = createIndividual()
        assert individual1.validate()
        assert individual1.save()

        Individual individual2 = createIndividual()
        shouldFail (AssertionError, {assert individual2.validate()})
    }

    @Test
    void testGetSamples() {
        Individual individual = createIndividual()
        assert individual.validate()
        assert individual.save()

        Sample sample1 = new Sample(
                individual: individual,
                sampleType: new SampleType()
                )
        assert sample1.save()

        assertEquals([sample1], individual.getSamples())

        Sample sample2 = new Sample(
                individual: individual,
                sampleType: new SampleType()
                )
        assert sample2.save()

        assertEquals([sample1, sample2], individual.getSamples())
    }

    @Test
    void testGetViewByPidPathBase() {
        Individual individual = Individual.build()
        SeqType seqType = SeqType.build()
        Realm realm = Realm.build(operationType: Realm.OperationType.DATA_MANAGEMENT, name: individual.project.realmName)

        String expectedPath = "${realm.rootPath}/${individual.project.dirName}/sequencing/${seqType.dirName}/view-by-pid"
        String actualPath = individual.getViewByPidPathBase(seqType).absoluteDataManagementPath

        assert expectedPath == actualPath
    }

    @Test
    void testGetViewByPidPath() {
        Individual individual = Individual.build()
        SeqType seqType = SeqType.build()
        Realm realm = Realm.build(operationType: Realm.OperationType.DATA_MANAGEMENT, name: individual.project.realmName)

        String expectedPath = "${realm.rootPath}/${individual.project.dirName}/sequencing/${seqType.dirName}/view-by-pid/${individual.pid}"
        String actualPath = individual.getViewByPidPath(seqType).absoluteDataManagementPath

        assert expectedPath == actualPath
    }


    @Test
    void testGetResultsPerPidPath(){
        Realm realm = DomainFactory.createRealmDataManagementDKFZ()
        assert realm.save()

        Project project = TestData.createProject(
                dirName: "projectDirName",
                realmName: realm.name
                )

        Individual individual = createIndividual()
        individual.project = project

        String expectedPath = "${realm.rootPath}/${project.dirName}/results_per_pid/${individual.pid}"
        String actualPath = individual.getResultsPerPidPath().absoluteDataManagementPath

        assertEquals(expectedPath, actualPath)
    }


    private Individual createIndividual() {
        return new Individual(
        pid: "pid",
        mockPid: "mockPid",
        mockFullName: "mockFullName",
        internIdentifier: "internIdentifier",
        type: Type.REAL,
        project: TestData.createProject()
        )
    }
}
