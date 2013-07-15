package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(MergingSet)
@Mock([MergingWorkPackage, Sample, SampleType, Individual, Project])
class MergingSetTests {

    MergingWorkPackage workPackage = null

    void setUp() {
        Project project = new Project(
            name: "project",
            dirName: "dirName",
            realmName: "DKFZ")
        project.save(flush: true)

        Individual individual = new Individual(
            pid: "pid",
            mockPid: "mockPid",
            mockFullName: "mockFullName",
            type: Individual.Type.REAL,
            project: project)
        individual.save(flush: true)

        SampleType sampleType = new SampleType(
            name: "sample-type")
        sampleType.save(flush: true)

        Sample sample = new Sample (
            individual: individual,
            sampleType: sampleType)
        sample.save(flush: true)

        this.workPackage = new MergingWorkPackage(
            sample: sample)
        this.workPackage.save(flush: true)
    }

    void tearDown() {
        this.workPackage = null
    }

    void testSave() {
        MergingSet mergingSet = new MergingSet(
            status: MergingSet.State.DECLARED,
            mergingWorkPackage: workPackage)
        Assert.assertTrue(mergingSet.validate())
        mergingSet.save(flush: true)
    }

    void testContraints() {
        // status in not null
        MergingSet mergingSet = new MergingSet(
            mergingWorkPackage: workPackage)
        mergingSet.status = null
        Assert.assertFalse(mergingSet.validate())
        // mergingWorkPackage is not null
        mergingSet.status = MergingSet.State.DECLARED
        Assert.assertTrue(mergingSet.validate())
        mergingSet.mergingWorkPackage = null
        Assert.assertFalse(mergingSet.validate())
    }
}
