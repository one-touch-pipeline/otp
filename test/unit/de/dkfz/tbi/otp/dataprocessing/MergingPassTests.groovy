package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(MergingPass)
@Mock([MergingSet, MergingWorkPackage,
    Sample, SampleType, Individual, Project])
class MergingPassTests {

    MergingSet mergingSet = null

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

        Sample sample = new Sample(
            individual: individual,
            sampleType: sampleType)
        sample.save(flush: true)

        MergingWorkPackage workPackage = new MergingWorkPackage(
            sample: sample)
        workPackage.save(flush: true)

        this.mergingSet = new MergingSet(
            mergingWorkPackage: workPackage)
        this.mergingSet.save(flush: true)
    }

    void tearDown() {
        this.mergingSet = null
    }

    void testSave() {
        MergingPass pass = new MergingPass(
            identifier: 1,
            mergingSet: mergingSet)
        Assert.assertTrue(pass.validate())
        pass.save(flush: true)
    }

    void testConstraints() {
        MergingPass pass = new MergingPass()
        Assert.assertFalse(pass.validate())
        pass.mergingSet = this.mergingSet
        Assert.assertTrue(pass.validate())
        pass.description = null
        Assert.assertTrue(pass.validate())
    }

    void testIsLatestPass() {
        MergingPass pass = new MergingPass(
            identifier: 1,
            mergingSet: mergingSet)
        pass.save(flush: true)
        assertTrue(pass.isLatestPass())

        MergingPass pass2 = new MergingPass(
            identifier: 2,
            mergingSet: mergingSet)
        pass2.save(flush: true)
        assertTrue(pass2.isLatestPass())

        assertFalse(pass.isLatestPass())
    }
}
